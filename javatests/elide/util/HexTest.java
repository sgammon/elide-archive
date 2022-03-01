package elide.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;


/** Test the hex-encode utility class. */
public final class HexTest {
  private static final String goldenPreimage1 = "jkhgjftt56y789upiokjl";
  private static final String goldenPreimage2 = "bvgfc6789iokplmjnhbgv";
  private static final String goldenPreimage3 = ":P)(*&^%$#%^&*(&^%$%^";
  private static final String goldenHex1 = "6a6b68676a6674743536793738397570696f6b6a6c";
  private static final String goldenHex2 = "627667666336373839696f6b706c6d6a6e68626776";
  private static final String goldenHex3 = "3a5029282a265e252423255e262a28265e2524255e";

  @Test void testSimpleHexEncode() {
    String encoded = Hex.bytesToHex("Hello123".getBytes(StandardCharsets.UTF_8));
    assertNotNull(encoded, "should not get null from encoding bytes to hex");
    assertEquals("48656c6c6f313233", encoded, "encoded value as hex should be expected value");
  }

  @Test void testHexEncodeLimited() {
    String encoded = Hex.bytesToHex("Hello123".getBytes(StandardCharsets.UTF_8), 4);
    assertNotNull(encoded, "should not get null from encoding bytes to hex");
    assertEquals("4865", encoded, "encoded value as hex should be expected value, when limited");

    String encoded2 = Hex.bytesToHex("Hello123".getBytes(StandardCharsets.UTF_8), 500);
    assertNotNull(encoded2, "should not get null from encoding bytes to hex");
    assertEquals("48656c6c6f313233", encoded2, "encoded value as hex should be expected value, when limited");
  }

  @Test void testHexEncodeUnlimited() {
    String encoded = Hex.bytesToHex("Hello123".getBytes(StandardCharsets.UTF_8), -1);
    assertNotNull(encoded, "should not get null from encoding bytes to hex");
    assertEquals("48656c6c6f313233", encoded, "encoded value as hex should be expected value, when limited");
  }

  @Test void testHexEncodeKnown() {
    String encoded = Hex.bytesToHex(goldenPreimage1.getBytes(StandardCharsets.UTF_8));
    assertNotNull(encoded, "should not get null from encoding bytes to hex");
    assertEquals(goldenHex1, encoded, "encoded value as hex should be expected value");
    String encoded2 = Hex.bytesToHex(goldenPreimage2.getBytes(StandardCharsets.UTF_8));
    assertNotNull(encoded2, "should not get null from encoding bytes to hex");
    assertEquals(goldenHex2, encoded2, "encoded value as hex should be expected value");
    String encoded3 = Hex.bytesToHex(goldenPreimage3.getBytes(StandardCharsets.UTF_8));
    assertNotNull(encoded3, "should not get null from encoding bytes to hex");
    assertEquals(goldenHex3, encoded3, "encoded value as hex should be expected value");
  }
}
