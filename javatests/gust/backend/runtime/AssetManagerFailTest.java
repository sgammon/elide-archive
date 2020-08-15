package gust.backend.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/** Tests that the {@link AssetManager} fails gracefully. */
public class AssetManagerFailTest {
  @Test void testAssetManagerLoadFailure() throws Exception {
    assertDoesNotThrow(AssetManager::load);
  }
}
