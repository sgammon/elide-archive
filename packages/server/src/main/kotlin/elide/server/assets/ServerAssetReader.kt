package elide.server.assets

import com.google.common.annotations.VisibleForTesting
import com.google.common.util.concurrent.Futures
import elide.server.cfg.AssetConfig
import elide.util.Base64
import io.micronaut.context.annotation.Context
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.guava.asDeferred
import tools.elide.assets.AssetBundle.AssetContent
import tools.elide.data.CompressedData
import tools.elide.data.CompressionMode
import java.util.EnumSet

/**
 * Default implementation of an [AssetReader]; used in concert with the default [AssetManager] to fulfill HTTP requests
 * for static assets embedded within the application.
 *
 * @param assetConfig Server-side asset configuration.
 * @Param assetIndex Live index of asset data.
 */
@Context @Singleton
public class ServerAssetReader @Inject internal constructor(
  private val assetConfig: AssetConfig,
  private val assetIndex: ServerAssetIndex,
) : AssetReader {
  @VisibleForTesting
  internal fun buildETagForAsset(entry: AssetContent): String {
    val identityVariant = entry.getVariant(0)
    return if (identityVariant.integrityCount > 0) {
      val integrity = identityVariant.getIntegrity(0)
      val encoded = Base64.encodeWebSafe(integrity.fingerprint.toByteArray())
      "\"$encoded\""
    } else {
      // since we don't have an integrity fingerprint for this asset, we can substitute and use a "weak" ETag via the
      // generated-timestamp in the asset bundle.
      val generatedTime = assetIndex.getBundleTimestamp()
      "W/\"$generatedTime\""
    }
  }

  @VisibleForTesting
  internal fun baselineHeaders(entry: AssetContent, variant: CompressedData): Map<String, String> {
    val headerMap = HashMap<String, String>()

    // apply content encoding header
    val contentEncoding = when (variant.compression) {
      CompressionMode.IDENTITY -> "identity"
      CompressionMode.GZIP -> "gzip"
      CompressionMode.BROTLI -> "br"
      else -> null
    }
    if (contentEncoding != null) headerMap[HttpHeaders.CONTENT_ENCODING] = contentEncoding

    // if we have a digest for this asset, we should affix it as the `ETag` for the response.
    if (assetConfig.etags) {
      val identityVariant = entry.getVariant(0)
      headerMap[HttpHeaders.ETAG] = buildETagForAsset(entry)
    }
    return headerMap
  }

  @VisibleForTesting
  internal fun selectBestVariant(
    content: AssetContent,
    request: HttpRequest<*>
  ): Pair<Map<String, String>, CompressedData> {
    val identity = content.getVariant(0)
    val acceptEncoding = request.headers[HttpHeaders.ACCEPT_ENCODING]
    if (acceptEncoding != null && acceptEncoding.isNotBlank()) {
      // calculate supported encodings based on request
      val encodings = EnumSet.copyOf(
        acceptEncoding.split(",").mapNotNull {
          when (it.trim().lowercase()) {
            "gzip" -> CompressionMode.GZIP
            "br" -> CompressionMode.BROTLI
            else -> null
          }
        }
      )

      // based on the set of supported encodings, find the smallest response available. because payloads are either
      // elided based on size, or sorted by size in ascending order (aside from the first which is the `IDENTITY`
      // payload), then it should always be the first option we can actually use.
      val bestCandidate = content.variantList.find {
        (it.compression != CompressionMode.IDENTITY && encodings.contains(it.compression))
      }

      // sanity check: the compressed variant should of course be smaller than the identity variant, otherwise it is
      // more efficient to just serve the identity variant.
      if (bestCandidate != null && bestCandidate.size < identity.size) {
        return baselineHeaders(content, bestCandidate) to bestCandidate
      }
    }

    // fallback to serve the non-compressed version of the asset.
    return baselineHeaders(content, identity) to identity
  }

  /** @inheritDoc */
  override suspend fun readAsync(descriptor: ServerAsset, request: HttpRequest<*>): Deferred<RenderedAsset> {
    val module = descriptor.module
    val content = assetIndex.readByModuleIndex(
      descriptor.index!!
    )

    // select the best content variant to use based on the input request, which may specify supported compression
    // schemes, or may be expressing if-not-modified or if-modified-since conditions.
    val (headers, selectedVariant) = selectBestVariant(
      content,
      request,
    )

    return Futures.immediateFuture(
      RenderedAsset(
        module = module,
        type = descriptor.assetType,
        variant = selectedVariant.compression,
        headers = headers,
        size = selectedVariant.size,
        lastModified = assetIndex.getBundleTimestamp(),
        digest = if (selectedVariant.integrityCount > 0) {
          val subj = selectedVariant.getIntegrity(0)
          subj.hash to subj.fingerprint
        } else {
          null
        }
      ) { selectedVariant.data.raw }
    ).asDeferred()
  }

  /** @inheritDoc */
  override fun resolve(path: String): ServerAsset? {
    val unprefixed = if (path.startsWith(assetConfig.prefix)) {
      // if the asset is prefixed, trim it first
      path.substring(assetConfig.prefix.length)
    } else {
      path
    }
    val unextensioned = if (unprefixed.contains(".")) {
      unprefixed.dropLast(unprefixed.length - unprefixed.lastIndexOf("."))
    } else {
      unprefixed
    }
    return assetIndex.resolveByTag(
      if (unextensioned.startsWith("/")) {
        unextensioned.drop(1)
      } else {
        unextensioned
      }
    )
  }
}