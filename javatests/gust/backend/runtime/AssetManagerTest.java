package gust.backend.runtime;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import gust.backend.runtime.AssetManager.ManagedAsset;
import gust.backend.runtime.AssetManager.ManagedAssetContent;
import tools.elide.assets.AssetBundle;
import tools.elide.assets.AssetBundle.StyleBundle.StyleAsset;
import tools.elide.assets.AssetBundle.ScriptBundle.ScriptAsset;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


/** Tests for the {@link AssetManager} object. */
@SuppressWarnings("DuplicatedCode")
public final class AssetManagerTest {
  /** Asset manager to test against. */
  private static volatile AssetManager manager;
  private static final String jsAssetTest = "d36955de";
  private static final String jsAssetName = "app.js";
  private static final String cssAssetTest = "c20a4786";
  private static final String cssAssetName = "mdl.css";

  @BeforeAll static void setupManager() throws Exception {
    manager = AssetManager.acquire();
  }

  @AfterAll static void teardownManager() {
    manager = null;
  }

  @Test void testManagerResolveModule() {
    Optional<ManagedAsset<ScriptAsset>> assetMetaJS = manager.assetMetadataByModule("todolist.main");
    assertNotNull(assetMetaJS, "should not get null for asset meta optional");
    assertTrue(assetMetaJS.isPresent(), "known-good asset should be present in bundle");
    assertEquals("todolist.main", assetMetaJS.get().getName(), "name for module should match");
    assertEquals(AssetManager.ModuleType.JS, assetMetaJS.get().getType(), "type for module should match");
    assertNotNull(assetMetaJS.get().getAssets(), "should be able to get assets for module");
    assertNotEquals(0, assetMetaJS.get().getAssets().size(),
      "should have non-empty set of assets for module");
    assertNotNull(assetMetaJS.get().getContent(), "should be able to get content for module");
    assertNotEquals(0, assetMetaJS.get().getContent().size(),
      "should have non-empty set of content for module");

    Optional<ManagedAsset<StyleAsset>> assetMetaCSS = manager.assetMetadataByModule("todolist.styles");
    assertNotNull(assetMetaCSS, "should not get null for asset meta optional");
    assertTrue(assetMetaCSS.isPresent(), "known-good asset should be present in bundle");
    assertEquals("todolist.styles", assetMetaCSS.get().getName(), "name for module should match");
    assertEquals(AssetManager.ModuleType.CSS, assetMetaCSS.get().getType(), "type for module should match");
    assertNotNull(assetMetaCSS.get().getAssets(), "should be able to get assets for module");
    assertNotEquals(0, assetMetaCSS.get().getAssets().size(),
      "should have non-empty set of assets for module");
    assertNotNull(assetMetaCSS.get().getContent(), "should be able to get content for module");
    assertNotEquals(0, assetMetaCSS.get().getContent().size(),
      "should have non-empty set of content for module");
  }

  @Test void testAssetModuleCompareEquals() {
    Optional<ManagedAsset<ScriptAsset>> assetMetaJS = manager.assetMetadataByModule("todolist.main");
    assertNotNull(assetMetaJS, "should not get null for asset meta optional");
    assertTrue(assetMetaJS.isPresent(), "known-good asset should be present in bundle");
    Optional<ManagedAsset<ScriptAsset>> assetMetaJS2 = manager.assetMetadataByModule("todolist.main");
    assertNotNull(assetMetaJS2, "should not get null for asset meta optional");
    assertTrue(assetMetaJS2.isPresent(), "known-good asset should be present in bundle");
    assertEquals(assetMetaJS.get(), assetMetaJS2.get(), "identical asset metadata should equals()");
    assertEquals(assetMetaJS.get().hashCode(), assetMetaJS2.get().hashCode(),
      "identical asset metadata should report identical hash code");

    Optional<ManagedAsset<StyleAsset>> assetMetaCSS = manager.assetMetadataByModule("todolist.styles");
    assertNotNull(assetMetaCSS, "should not get null for asset meta optional");
    assertTrue(assetMetaCSS.isPresent(), "known-good asset should be present in bundle");
    Optional<ManagedAsset<StyleAsset>> assetMetaCSS2 = manager.assetMetadataByModule("todolist.styles");
    assertNotNull(assetMetaCSS2, "should not get null for asset meta optional");
    assertTrue(assetMetaCSS2.isPresent(), "known-good asset should be present in bundle");
    assertEquals(assetMetaCSS.get(), assetMetaCSS2.get(), "identical asset metadata should equals()");
    assertEquals(assetMetaCSS.get().hashCode(), assetMetaCSS2.get().hashCode(),
      "identical asset metadata should report identical hash code");

    assertFalse(assetMetaJS.get().equals(assetMetaCSS.get()),
      "different asset modules should report `false` for `equals()`");
    assertNotEquals(assetMetaJS.get().hashCode(), assetMetaCSS.get().hashCode(),
      "different asset modules should report different hash codes");
    assertNotEquals(0, assetMetaJS.get().compareTo(assetMetaCSS.get()),
      "should be able to compare different asset metadata objects");
  }

  @Test void testManagerResolveUnknown() {
    Optional<ManagedAsset<ScriptAsset>> assetMetaJS = manager.assetMetadataByModule("todolist.doesnotexist");
    assertNotNull(assetMetaJS, "should not get null for asset meta optional");
    assertFalse(assetMetaJS.isPresent(), "known-bad asset should not be present in bundle");
    Optional<ManagedAsset<StyleAsset>> assetMetaCSS = manager.assetMetadataByModule("yoyoyoyoy");
    assertNotNull(assetMetaCSS, "should not get null for asset meta optional");
    assertFalse(assetMetaCSS.isPresent(), "known-bad asset should not be present in bundle");
  }

  @Test void testResolveJsChunk() {
    Optional<ManagedAssetContent> jsAsset = manager.assetDataByToken(jsAssetTest);
    assertNotNull(jsAsset, "should not get null for asset data optional");
    assertTrue(jsAsset.isPresent(), "known-good asset data should be present in bundle");
    assertEquals(jsAssetName, jsAsset.get().getFilename(), "original filename should match");
    assertEquals("todolist.main", jsAsset.get().getModule(), "asset module should match");
    assertNotNull(jsAsset.get().getToken(), "asset should have a non-null token");
    assertNotEquals("", jsAsset.get().getToken(), "asset should have a non-empty token");
    assertNotNull(jsAsset.get().getETag(), "asset should have a non-null etag");
    assertNotEquals("", jsAsset.get().getETag(), "asset should have a non-empty etag");
    assertNotNull(jsAsset.get().getLastModified(), "asset should provide a non-null last-modified");
    assertNotNull(jsAsset.get().getSize(), "asset should provide a non-null size");
    assertTrue(jsAsset.get().getSize() > 0L, "asset should provide a greater-than-zero size");
    assertNotNull(jsAsset.get().getOptimalCompression(),
      "asset should provide a non-null optimal compression");
    assertNotNull(jsAsset.get().getCompressedSize(), "asset should provide a non-null compressed size");
    assertTrue(jsAsset.get().getCompressedSize() > 0L,
      "asset should provide a greater-than-zero compressed size");
    assertTrue(jsAsset.get().getVariantCount() > 0,
      "asset should specify a non-zero variant count");
    assertNotNull(jsAsset.get().getCompressionOptions(),
      "asset should specify a non-null set of compression options");
    assertNotNull(jsAsset.get().getContent(),
      "asset should specify a non-null content record");
  }

  @Test void testResolveCssChunk() {
    Optional<ManagedAssetContent> cssAsset = manager.assetDataByToken(cssAssetTest);
    assertNotNull(cssAsset, "should not get null for asset data optional");
    assertTrue(cssAsset.isPresent(), "known-good asset data should be present in bundle");
    assertEquals(cssAssetName, cssAsset.get().getFilename(), "original filename should match");
    assertEquals("todolist.mdl", cssAsset.get().getModule(), "asset module should match");
    assertNotNull(cssAsset.get().getToken(), "asset should have a non-null token");
    assertNotEquals("", cssAsset.get().getToken(), "asset should have a non-empty token");
    assertNotNull(cssAsset.get().getETag(), "asset should have a non-null etag");
    assertNotEquals("", cssAsset.get().getETag(), "asset should have a non-empty etag");
    assertNotNull(cssAsset.get().getLastModified(), "asset should provide a non-null last-modified");
    assertNotNull(cssAsset.get().getSize(), "asset should provide a non-null size");
    assertTrue(cssAsset.get().getSize() > 0L, "asset should provide a greater-than-zero size");
    assertNotNull(cssAsset.get().getOptimalCompression(),
      "asset should provide a non-null optimal compression");
    assertNotNull(cssAsset.get().getCompressedSize(), "asset should provide a non-null compressed size");
    assertTrue(cssAsset.get().getCompressedSize() > 0L,
      "asset should provide a greater-than-zero compressed size");
    assertTrue(cssAsset.get().getVariantCount() > 0,
      "asset should specify a non-zero variant count");
    assertNotNull(cssAsset.get().getCompressionOptions(),
      "asset should specify a non-null set of compression options");
    assertNotNull(cssAsset.get().getContent(),
      "asset should specify a non-null content record");
  }

  @Test void testResolveMissingChunk() {
    Optional<ManagedAssetContent> chunkNotFound = manager.assetDataByToken("yoyoyoyoyoy");
    assertNotNull(chunkNotFound, "missing chunk optional should still not be null");
    assertFalse(chunkNotFound.isPresent(), "known-bad chunk should not be in the bundle");
  }

  @Test void testChunkCompareEquals() {
    Optional<ManagedAssetContent> jsAsset = manager.assetDataByToken(jsAssetTest);
    assertNotNull(jsAsset, "should not get null for asset data optional");
    assertTrue(jsAsset.isPresent(), "known-good asset data should be present in bundle");
    assertEquals(jsAssetName, jsAsset.get().getFilename(), "original filename should match");
    Optional<ManagedAssetContent> jsAsset2 = manager.assetDataByToken(jsAssetTest);
    assertNotNull(jsAsset2, "should not get null for asset data optional");
    assertTrue(jsAsset2.isPresent(), "known-good asset data should be present in bundle");
    assertEquals(jsAssetName, jsAsset2.get().getFilename(), "original filename should match");
    assertEquals(jsAsset.get(), jsAsset2.get(),
      "identical asset chunks should report themselves `true` for `equals()`");
    assertEquals(jsAsset.get().hashCode(), jsAsset2.get().hashCode(),
      "identical asset chunks should report identical hash codes");

    Optional<ManagedAssetContent> cssAsset = manager.assetDataByToken(cssAssetTest);
    assertNotNull(cssAsset, "should not get null for asset data optional");
    assertTrue(cssAsset.isPresent(), "known-good asset data should be present in bundle");
    assertEquals(cssAssetName, cssAsset.get().getFilename(), "original filename should match");
    Optional<ManagedAssetContent> cssAsset2 = manager.assetDataByToken(cssAssetTest);
    assertNotNull(cssAsset2, "should not get null for asset data optional");
    assertTrue(cssAsset2.isPresent(), "known-good asset data should be present in bundle");
    assertEquals(cssAssetName, cssAsset2.get().getFilename(), "original filename should match");
    assertEquals(cssAsset.get(), cssAsset2.get(),
      "identical asset chunks should report themselves `true` for `equals()`");
    assertEquals(cssAsset.get().hashCode(), cssAsset2.get().hashCode(),
      "identical asset chunks should report identical hash codes");

    assertFalse(jsAsset.get().equals(cssAsset.get()),
      "different asset chunks should report `false` for `equals()`");
    assertNotEquals(jsAsset.get().hashCode(), cssAsset.get().hashCode(),
      "different asset chunks should report different hash codes");
    assertNotEquals(0, jsAsset.get().compareTo(cssAsset.get()),
      "should be able to compare different asset chunks objects");
  }

  @Test void testInvalidAlgorithm() {
    assertThrows(RuntimeException.class, () -> AssetManager.ContentInfo
      .fromProto(AssetBundle.AssetContent.getDefaultInstance(),
      "i-do-not-exist"));

    assertThrows(RuntimeException.class, () -> AssetManager.ContentInfo
      .fromProto(AssetBundle.AssetContent.getDefaultInstance(),
      "SHA256"));
  }
}
