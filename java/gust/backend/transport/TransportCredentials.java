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
package gust.backend.transport;

import javax.annotation.Nonnull;


/**
 * Specifies the generic notion of "transport credentials," as configuration or logic. Credentials of this nature are
 * generally used during external connection establishment via {@link TransportManager} implementations.
 */
public interface TransportCredentials {
  /**
   * @return Whether a transport requires credentials. This defaults to <code>false</code>.
   */
  default @Nonnull Boolean requiresCredentials() {
    return false;
  }
}
