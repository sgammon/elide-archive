/*
 * Copyright © 2020, The Gust Framework Authors. All rights reserved.
 *
 * The Gust/Elide framework and tools, and all associated source or object computer code, except where otherwise noted,
 * are licensed under the Zero Prosperity license, which is enclosed in this repository, in the file LICENSE.txt. Use of
 * this code in object or source form requires and implies consent and agreement to that license in principle and
 * practice. Source or object code not listing this header, or unless specified otherwise, remain the property of
 * Elide LLC and its suppliers, if any. The intellectual and technical concepts contained herein are proprietary to
 * Elide LLC and its suppliers and may be covered by U.S. and Foreign Patents, or patents in process, and are protected
 * by trade secret and copyright law. Dissemination of this information, or reproduction of this material, in any form,
 * is strictly forbidden except in adherence with assigned license requirements.
 */
package gust.backend.runtime;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import gust.util.Hex;
import gust.util.Pair;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Infrastructure;
import io.micronaut.core.annotation.NonNull;
import org.slf4j.Logger;
import tools.elide.assets.AssetBundle;
import tools.elide.assets.AssetBundle.StyleBundle.StyleAsset;
import tools.elide.assets.AssetBundle.ScriptBundle.ScriptAsset;
import tools.elide.core.data.CompressedData;
import tools.elide.core.data.CompressionMode;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;


/**
 * Manager class, which mediates interactions with the binary asset bundle. When managed assets are active, the content
 * and manifest are located in a binary proto file at the root of the JAR.
 *
 * <p>This object acts as a singleton, and is responsible for the actual mechanics of initially reading the asset bundle
 * and interpreting its contents. Once we have established indexes and completed other prep work, the manager moves into
 * a read-only mode, where its primary job shifts to satisfying dynamic asset requests - either for referential metadata
 * which is used to embed an asset in the DOM, or content data, which is used to serve the asset itself.</p>
 *
 * <p>Multiple "variants" of an asset are stored in the bundle (if so configured). This includes one variant reflecting
 * the regular, un-modified content for the asset, and an additional variant for each caching strategy supported by the
 * framework ({@code GZIP} and {@code BROTLI} at the time of this writing).</p>
 */
@Context
@ThreadSafe
@Infrastructure
@SuppressWarnings("UnstableApiUsage")
public final class AssetManager {
  /** Private logging pipe. */
  private static final @NonNull Logger logging = Logging.logger(AssetManager.class);

  /** Path to the asset manifest resource. */
  private static final @NonNull String manifestPath = "/assets.pb";

  /** Length of generated ETag values. */
  private static final int ETAG_LENGTH = 8;

  /** Algorithm to use for ETag value generation. */
  private static final @NonNull String ETAG_DIGEST_ALGORITHM = "SHA-256";

  /** Shared/static asset bundle object, which is immutable. */
  private static volatile AssetBundle loadedBundle;

  /** Specifies a map of asset modules to metadata. */
  private static final @NonNull SortedMap<String, ModuleMetadata<? extends Message>> assetMap = new TreeMap<>();

  /** Maps content blocks to their module names. */
  private static final @NonNull Multimap<String, String> modulesToTokens = MultimapBuilder
    .hashKeys()
    .treeSetValues()
    .build();

  /** Specifies a map of tokens to their content info. */
  private static final @NonNull SortedMap<String, ContentInfo> tokenMap = new TreeMap<>();

  /** Holds on to info related to a raw asset file. */
  @Immutable
  static final class ContentInfo {
    /** Unique token for this asset. */
    final @NonNull String token;

    /** Unique token for this asset. */
    final @NonNull String module;

    /** Original filename for this asset. */
    final @NonNull String filename;

    /** Uncompressed data size. */
    final @NonNull Long size;

    /** Etag, calculated from the token and filename. */
    final @NonNull String etag;

    /** Smallest compression option. */
    final @NonNull CompressionMode optimalCompression;

    /** Size of the optimally-compressed variant. */
    final @NonNull Long compressedSize;

    /** Count of variants held by this content info block. */
    final @NonNull Integer variantCount;

    /** Options that exist for pre-compressed variants of this content. */
    final @NonNull EnumSet<CompressionMode> compressionOptions;

    /** Pointer to the content record backing this object. */
    final @NonNull AssetBundle.AssetContent content;

    /** Raw constructor for content info metadata. */
    private ContentInfo(@NonNull String token,
                        @NonNull String module,
                        @NonNull String filename,
                        @NonNull Long size,
                        @NonNull String etag,
                        @NonNull CompressionMode optimalCompression,
                        @NonNull Long compressedSize,
                        @NonNull Integer variantCount,
                        @NonNull EnumSet<CompressionMode> compressionOptions,
                        @NonNull AssetBundle.AssetContent content) {
      this.token = token;
      this.module = module;
      this.filename = filename;
      this.size = size;
      this.etag = etag;
      this.optimalCompression = optimalCompression;
      this.compressedSize = compressedSize;
      this.variantCount = variantCount;
      this.compressionOptions = compressionOptions;
      this.content = content;
    }

    /**
     * Inflate a {@link ContentInfo} record from an {@link AssetBundle.AssetContent} definition. This method variant
     * additionally allows specification of a custom `ETag` digest algorithm.
     *
     * @param content Asset content protocol object.
     * @param algorithm Algorithm to use for etags.
     * @return Checked content info object.
     */
    static @NonNull ContentInfo fromProto(@NonNull AssetBundle.AssetContent content, @NonNull String algorithm) {
      try {
        MessageDigest digester = MessageDigest.getInstance(algorithm);
        digester.update(content.getModule().getBytes(StandardCharsets.UTF_8));
        digester.update(content.getFilename().getBytes(StandardCharsets.UTF_8));
        digester.update(content.getToken().getBytes(StandardCharsets.UTF_8));
        digester.update(String.valueOf(content.getVariantCount()).getBytes(StandardCharsets.UTF_8));
        byte[] etagDigest = digester.digest();

        // find uncompressed size
        Long uncompressedAssetSize = content.getVariantList().stream()
          .filter((variant) -> variant.getCompression().equals(CompressionMode.IDENTITY))
          .findFirst()
          .orElseGet(CompressedData::getDefaultInstance)
          .getSize();

        // resolve optimal compression
        Pair<Long, CompressionMode> optimalCompression = content.getVariantList().stream()
          .map((data) -> Pair.of(data.getSize(), data.getCompression()))
          .min(Comparator.comparing(Pair::getKey))
          .orElse(Pair.of(0L, CompressionMode.IDENTITY));

        // resolve set of supported compression options for this asset
        EnumSet<CompressionMode> compressionOptions = EnumSet.copyOf(content.getVariantList().parallelStream()
          .map(CompressedData::getCompression)
          .collect(Collectors.toList()));

        return new ContentInfo(
          content.getToken(),
          content.getModule(),
          content.getFilename(),
          uncompressedAssetSize,
          Hex.bytesToHex(etagDigest, ETAG_LENGTH),
          optimalCompression.getValue(),
          optimalCompression.getKey(),
          content.getVariantCount(),
          compressionOptions,
          content);

      } catch (NoSuchAlgorithmException exc) {
        throw new RuntimeException(exc);
      }
    }

    /**
     * Inflate a {@link ContentInfo} record from an {@link AssetBundle.AssetContent} definition.
     *
     * @param content Asset content protocol object.
     * @return Checked content info object.
     */
    static @NonNull ContentInfo fromProto(AssetBundle.AssetContent content) {
      return fromProto(content, ETAG_DIGEST_ALGORITHM);
    }
  }

  /** Enumerates types of asset modules. */
  public enum ModuleType {
    /** The bundle contains JavaScript code. */
    JS,

    /** The bundle contains style declarations. */
    CSS
  }

  /** Holds on to info related to an asset module's metadata. */
  @Immutable
  static final class ModuleMetadata<M extends Message> {
    /** Name of this asset module. */
    final @NonNull String name;

    /** Type of code/logic contained by this asset. */
    final @NonNull ModuleType type;

    /** Raw asset records for this module. */
    final @NonNull List<M> assets;

    /** Raw constructor for asset module metadata. */
    private ModuleMetadata(@NonNull ModuleType type,
                           @NonNull String name,
                           @NonNull List<M> assets) {
      this.name = name;
      this.type = type;
      this.assets = assets;
    }

    /**
     * Inflate a {@link ModuleMetadata} record from a {@link AssetBundle.StyleBundle} definition.
     *
     * @param content Asset content protocol object.
     * @return Checked module info object.
     */
    static @NonNull ModuleMetadata<StyleAsset> fromStyleProto(@NonNull AssetBundle.StyleBundle content) {
      return new ModuleMetadata<>(
        ModuleType.CSS,
        content.getModule(),
        content.getAssetList());
    }

    /**
     * Inflate a {@link ModuleMetadata} record from a {@link AssetBundle.ScriptBundle} definition.
     *
     * @param content Asset content protocol object.
     * @return Checked module info object.
     */
    static @NonNull ModuleMetadata<ScriptAsset> fromScriptProto(@NonNull AssetBundle.ScriptBundle content) {
      return new ModuleMetadata<>(
        ModuleType.JS,
        content.getModule(),
        content.getAssetList());
    }
  }

  /** Public API surface for interacting with raw asset content. */
  @Immutable
  @SuppressWarnings("unused")
  public static final class ManagedAssetContent implements Comparable<ManagedAssetContent> {
    /** Attached/encapsulated asset content and info. */
    private final @NonNull ContentInfo content;

    /** Create a {@link ManagedAssetContent} object from scratch. */
    ManagedAssetContent(@NonNull ContentInfo content) {
      this.content = content;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) return true;
      if (other == null || getClass() != other.getClass()) return false;
      ManagedAssetContent that = (ManagedAssetContent) other;
      return com.google.common.base.Objects
        .equal(content.token, that.content.token);
    }

    @Override
    public int hashCode() {
      return com.google.common.base.Objects.hashCode(content.token);
    }

    @Override
    public int compareTo(@NonNull ManagedAssetContent other) {
      return this.content.token.compareTo(other.content.token);
    }

    /** @return Opaque token identifying this asset content. */
    public @NonNull String getToken() {
      return content.token;
    }

    /** @return Module name for this content chunk. */
    public @NonNull String getModule() {
      return content.module;
    }

    /** @return Pre-calculated ETag value for this asset. */
    public @NonNull String getETag() {
      return content.etag;
    }

    /** @return Original filename for the asset. */
    public @NonNull String getFilename() {
      return content.filename;
    }

    /** @return Last-modified-timestamp for this asset. */
    public @NonNull Timestamp getLastModified() {
      return loadedBundle.getGenerated();
    }

    /** @return Un-compressed size of the asset. */
    public @NonNull Long getSize() {
      return content.size;
    }

    /** @return Optimal compression mode. */
    public @NonNull CompressionMode getOptimalCompression() {
      return content.optimalCompression;
    }

    /** @return Compressed size of the asset (optimal). */
    @SuppressWarnings("WeakerAccess")
    public @NonNull Long getCompressedSize() {
      return content.compressedSize;
    }

    /** @return Count of variants that exist for this asset. */
    public @NonNull Integer getVariantCount() {
      return content.variantCount;
    }

    /** @return Set of supported compression modes for this asset. */
    public @NonNull EnumSet<CompressionMode> getCompressionOptions() {
      return content.compressionOptions;
    }

    /** Retrieve the content backing this info record. */
    public @NonNull AssetBundle.AssetContent getContent() {
      return content.content;
    }
  }

  /** Public API surface for interacting with raw asset metadata. */
  @Immutable
  public static final class ManagedAsset<M extends Message> implements Comparable<ManagedAsset> {
    /** Resolved module metadata for this asset. */
    private final @NonNull ModuleMetadata<M> module;

    /** Logic references that constitute this managed asset, including dependencies, in reverse topological order. */
    private final @NonNull Collection<ManagedAssetContent> content;

    /** Construct a new managed asset from scratch. */
    ManagedAsset(@NonNull ModuleMetadata<M> module,
                 @NonNull Collection<ManagedAssetContent> content) {
      this.module = module;
      this.content = content;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ManagedAsset that = (ManagedAsset) o;
      return com.google.common.base.Objects
        .equal(module.name, that.module.name);
    }

    @Override
    public int hashCode() {
      return com.google.common.base.Objects
        .hashCode(module.name);
    }

    @Override
    public int compareTo(@NonNull ManagedAsset other) {
      return this.module.name.compareTo(other.module.name);
    }

    /** @return This module's assigned name. */
    public @NonNull String getName() {
      return module.name;
    }

    /** @return This module's assigned type. */
    public @NonNull ModuleType getType() {
      return module.type;
    }

    /** @return Collection of typed asset records constituting this bundle. */
    public @NonNull Collection<M> getAssets() {
      return module.assets;
    }

    /** @return Content configurations associated with this asset bundle. */
    public @NonNull Collection<ManagedAssetContent> getContent() {
      return this.content;
    }
  }

  /** index the newly-installed asset bundle. */
  private static void index() {
    if (logging.isDebugEnabled())
      logging.debug("Indexing raw assets by token...");
    tokenMap.putAll(loadedBundle.getAssetList().stream()
      .map(ContentInfo::fromProto)
      .map((info) -> Pair.of(info.token, info))
      .peek((pair) -> {
        if (logging.isTraceEnabled())
          logging.trace(format("- Indexing asset content at token '%s' from original file '%s'.",
            pair.getKey(),
            pair.getValue().filename));
      })
      .collect(Collectors.toMap(Pair::getKey, Pair::getValue)));

    if (logging.isDebugEnabled())
      logging.debug("Indexing CSS assets by module name...");
    assetMap.putAll(loadedBundle.getStylesMap().entrySet().stream()
      .map((entry) -> Pair.of(entry.getKey(), ModuleMetadata.fromStyleProto(entry.getValue())))
      .peek((pair) -> {
        // map each asset to its constituent module
        pair.getValue().assets.forEach((asset) -> modulesToTokens.put(pair.getKey(), asset.getToken()));

        if (logging.isTraceEnabled())
          logging.trace(format("- Indexing style module '%s' of type %s.",
            pair.getKey(),
            pair.getValue().type));
      })
      .collect(Collectors.toMap(Pair::getKey, Pair::getValue)));

    if (logging.isDebugEnabled())
      logging.debug("Indexing JS assets by module name...");
    assetMap.putAll(loadedBundle.getScriptsMap().entrySet().stream()
      .map((entry) -> Pair.of(entry.getKey(), ModuleMetadata.fromScriptProto(entry.getValue())))
      .peek((pair) -> {
        // map each asset to its constituent module
        pair.getValue().assets.forEach((asset) -> modulesToTokens.put(pair.getKey(), asset.getToken()));

        if (logging.isTraceEnabled())
          logging.trace(format("- Indexing script module '%s' of type %s.",
            pair.getKey(),
            pair.getValue().type));
      })
      .collect(Collectors.toMap(Pair::getKey, Pair::getValue)));
  }

  /**
   * Attempt to force-load the asset manifest, in a static context, preparing our indexed read-only data regarding the
   * data it contains. If we can't load the file, surface an exception so the invoking code can decide what to do.
   *
   * @throws IOException If some otherwise unmentioned I/O error occurs.
   * @throws FileNotFoundException If the asset manifest could not be found.
   * @throws InvalidProtocolBufferException If the enclosed Protocol Buffer data isn't recognizable.
   */
  public static void load() throws IOException, InvalidProtocolBufferException {
    if (loadedBundle != null) return;
    if (logging.isDebugEnabled())
      logging.debug(format("Attempting to load manifest as resource (at path '%s')", manifestPath));

    URL manifestURL = AssetManager.class.getResource(manifestPath);
    if (manifestURL == null) {
      logging.debug("No resource manifest found. Proceeding with empty manifest...");
      loadedBundle = AssetBundle.getDefaultInstance();
    } else {
      if (logging.isDebugEnabled()) logging.debug("Loading resource manifest...");
      try (InputStream assetBundle = AssetManager.class.getResourceAsStream(manifestPath)) {
        try (BufferedInputStream buffer = new BufferedInputStream(assetBundle)) {
          AssetBundle bundle = AssetBundle.parseDelimitedFrom(buffer);
          Function<Integer, Boolean> plural = ((number) -> (number > 1 || number == 0));

          if (bundle.isInitialized()) {
            loadedBundle = bundle;
            index();
            logging.info(format("Asset bundle loaded with %s %s (%s %s, %s %s%s).",
              bundle.getAssetCount(),
              plural.apply(bundle.getAssetCount()) ? "assets" : "asset",
              bundle.getScriptsCount(),
              plural.apply(bundle.getScriptsCount()) ? "scripts" : "script",
              bundle.getStylesCount(),
              plural.apply(bundle.getStylesCount()) ? "stylesheets" : "stylesheet",
              bundle.getRewrite() ? ", with rewriting ACTIVE" : ", with no style rewriting"));
          }
        }
      }
    }
  }

  /**
   * Acquire a new instance of the asset manager. The instance provided by this method is not guaranteed to be fresh for
   * every invocation (it may be a shared object), but all operations on the asset manager are threadsafe nonetheless.
   *
   * @return Asset manager instance.
   */
  public static AssetManager acquire() throws IOException {
    AssetManager.load();
    return new AssetManager();
  }

  /** Package-private constructor. Acquire an instance through {@link #acquire()}. */
  @SuppressWarnings("WeakerAccess")
  AssetManager() { /* Disallow instantiation except through DI. */ }

  // -- Public API -- //

  /**
   * Resolve raw asset content by its opaque token. This will hand back an object containing representations of the
   * asset for each enabled compression mode.
   *
   * <p>The object also knows how to resolve the most-optimal representation, based on the accepted compression modes
   * indicated by the invoking client.</p>
   *
   * @param token Token uniquely identifying this asset (generated from the module name and content fingerprint).
   * @return Optional, either {@link Optional#empty()} if the asset could not be found, or wrapping the result.
   */
  public @NonNull Optional<ManagedAssetContent> assetDataByToken(@NonNull String token) {
    if (logging.isTraceEnabled())
      logging.trace(format("Resolving asset by token '%s'.", token));
    if (!tokenMap.containsKey(token)) {
      logging.warn(format("Asset not found at token '%s'.", token));
      return Optional.empty();
    }
    if (logging.isDebugEnabled())
      logging.debug(format("Resolved valid asset via token '%s'.", token));
    return Optional.of(new ManagedAssetContent(Objects.requireNonNull(tokenMap.get(token))));
  }

  /**
   * Resolve asset metadata by its module name. This will hand back an object specifying the type/name of the module,
   * and links to each of the content blocks that constitute it.
   *
   * <p>This code path is generally used for resolving metadata for an asset so it can be <i>referenced</i>. The serving
   * for URL generated for the asset refers to a specific content block with an opaque token, rather than the module
   * name, which refers to a bundle of assets or content.</p>
   *
   * @param module Module name for which we should resolve asset metadata.
   * @return Optional, either {@link Optional#empty()} if the asset group could not be found, or wrapping the result.
   */
  @SuppressWarnings("unused")
  public @NonNull <M extends Message> Optional<ManagedAsset<M>> assetMetadataByModule(@NonNull String module) {
    if (logging.isTraceEnabled())
      logging.trace(format("Resolving asset metadata at module '%s'.", module));
    if (!assetMap.containsKey(module)) {
      if (assetMap.isEmpty()) {
        logging.warn(format("Asset metadata not found in (EMPTY) module map, at module name '%s'.", module));
      } else {
        logging.warn(format("Asset metadata not found at module name '%s'.", module));
      }
      return Optional.empty();
    }

    // resolve content for the module
    ImmutableSortedSet.Builder<ManagedAssetContent> assetsBuilder = ImmutableSortedSet.naturalOrder();
    Collection<String> contentTokens = modulesToTokens.get(module);

    Collection<ManagedAssetContent> contents = Collections.emptySet();
    if (!contentTokens.isEmpty()) {
      // resolve content for each token
      assetsBuilder.addAll((Iterable<ManagedAssetContent>)contentTokens.parallelStream()
        .map((token) -> Pair.of(token, this.assetDataByToken(token)))
        .peek((pair) -> {
          var content = pair.getValue();
          if (content.isPresent() && logging.isDebugEnabled()) {
            logging.debug(format("Resolved content block at token '%s' for module '%s.'",
              pair.getKey(),
              module));
          }
        })
        .filter((pair) -> pair.getValue().isPresent())
        .map(Pair::getValue)
        .map(Optional::get)
        .collect(Collectors.toCollection(ConcurrentSkipListSet::new)));

      contents = assetsBuilder.build();
    }

    if (logging.isDebugEnabled())
      logging.debug(format("Resolved valid asset metadata for module '%s'.", module));
    //noinspection unchecked
    return Optional.of(new ManagedAsset<>(
      Objects.requireNonNull((ModuleMetadata<M>)assetMap.get(module)),
      contents));
  }
}
