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
package gust.backend.annotations;

import io.micronaut.aop.Introduction;
import static tools.elide.backend.builtin.Sitemap.ChangeFrequency;

import javax.annotation.Nonnull;
import java.lang.annotation.*;


/**
 * Specifies settings for a given page method, that may later be applied via outputs like the application's site-map, or
 * other metadata assets.
 */
@Documented
@Introduction
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Page {
    /** Property name for robots enable/disable. */
    String ROBOTS_ENABLE = "enableIndexing";

    /** Property name for indexing robot settings. */
    String ROBOTS_SETTINGS = "robotsSettings";

    /** Property for Googlebot-specific settings. */
    String GOOGLEBOT_SETTINGS = "googlebotSettings";

    /** Property name for sitemap enable/disable. */
    String SITEMAP = "sitemap";

    /** Property name for the canonical URL. */
    String CANONICAL = "canonicalUrl";

    /** Property name for the page's last-modified date. */
    String LAST_MODIFIED = "lastModified";

    /** Property name for the page's change-frequency value. */
    String CHANGE_FREQUENCY = "changeFrequency";

    /** Property name for the page's priority value. */
    String PRIORITY = "priority";

    /** Sentinel for no-value strings. */
    String NO_VALUE = "NO_VALUE";

    /**
     * Simple name / tag for the page. This can be used to generate URLs or route requests.
     *
     * @return Module name for this script module.
     */
    @Nonnull String value();

    /**
     * Whether to allow robots on the selected page.
     *
     * @return Defaults to {@code true}.
     */
    boolean enableIndexing() default true;

    /**
     * Generic settings for indexing robots.
     *
     * @return Indexing settings, if present, or {@code null}.
     */
    String robotsSettings() default NO_VALUE;

    /**
     * Settings for Googlebot on the selected page.
     *
     * @return Googlebot-specific settings, if present, or {@code null}.
     */
    String googlebotSettings() default NO_VALUE;

    /**
     * Whether to list this page in the site map.
     *
     * @return Defaults to {@code true}.
     */
    boolean sitemap() default true;

    /**
     * Specifies the canonical URL to use in sitemaps and robots.txt listings, etc, for this page, if applicable.
     *
     * @return The canonical URL, or {@code null}.
     */
    String canonicalUrl() default NO_VALUE;

    /**
     * Retrieve the last-modified string for this page. For now, this value is static.
     *
     * @return Last-modified value, or {@code null}.
     */
    String lastModified() default NO_VALUE;

    /**
     * Retrieve the change frequency set for this page, if any.
     *
     * @return Change frequency value, or {@code null}.
     */
    ChangeFrequency changeFrequency() default ChangeFrequency.UNSPECIFIED_CHANGE_FREQUENCY;

    /**
     * Priority value for the page.
     *
     * @return Priority value, or {@code null}.
     */
    String priority() default NO_VALUE;
}
