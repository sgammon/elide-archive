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
package gust.util;

import javax.annotation.Nonnull;


/** Provides utilities for encoding values into hex, or decoding values from hex. */
public final class Hex {
  /** Array of hex-allowable characters. */
  private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

  /**
   * Convert a byte array to hex.
   *
   * @param bytes Raw bytes to encode.
   * @return Resulting hex-encoded string.
   */
  public static @Nonnull String bytesToHex(byte[] bytes) {
    return bytesToHex(bytes, -1);
  }

  /**
   * Convert a byte array to hex, optionally limiting the number of characters returned and cycles performed.
   *
   * @param bytes Raw bytes to encode.
   * @param maxChars Max number of output characters to care about. Pass `-1` to encode the whole thing.
   * @return Resulting hex-encoded string.
   */
  public static @Nonnull String bytesToHex(byte[] bytes, int maxChars) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < (maxChars == -1 ? bytes.length : Math.min(bytes.length, maxChars / 2)); j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_ARRAY[v >>> 4];
      hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }
    return new String(hexChars).trim().replace("\000", "");
  }

  private Hex() { /* Disallow instantiation. */ }
}
