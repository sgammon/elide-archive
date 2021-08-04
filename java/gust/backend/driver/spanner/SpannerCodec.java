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
package gust.backend.driver.spanner;

import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Struct;
import com.google.protobuf.Message;
import gust.backend.model.*;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;


/**
 * Implements a {@link ModelCodec} for Spanner types used as intermediaries during database operations. In particular,
 * Spanner uses {@link Mutation} objects to express writes, and yields reads in the form of {@link Struct} objects which
 * each identify a result row.
 *
 * <p>This codec is implemented with custom serialization via {@link SpannerMutationSerializer}, and de-serialization
 * via {@link SpannerStructDeserializer}, which are similarly specialized at compile time to a given {@link Message}
 * generated schema implementation.</p>
 *
 * @see SpannerMutationSerializer Specialized serialization from {@link Message} objects to Spanner {@link Mutation}s.
 * @see SpannerStructDeserializer Specialized de-serialization from {@link Struct} objects to {@link Message}s.
 * @param <Model> Typed {@link Message} which implements a concrete model object structure, as defined and annotated by
 *                the core Gust annotations.
 */
@Factory
@Immutable
@ThreadSafe
public final class SpannerCodec<Model extends Message> implements ModelCodec<Model, Mutation, Struct> {
    /** Default model instance to build with. */
    private final Model instance;

    /** Serializer for the model, provided at construction time. */
    private final ModelSerializer<Model, Mutation> serializer;

    /** Deserializer for the model, provided at construction time. */
    private final ModelDeserializer<Struct, Model> deserializer;

    /**
     * Construct a mutation codec from scratch.
     *
     * @param instance Model instance we intend to encode / decode with this codec.
     * @param deserializer Deserializer for read-intermediate objects.
     */
    private SpannerCodec(@Nonnull Model instance,
                         @Nonnull ModelSerializer<Model, Mutation> serializer,
                         @Nonnull ModelDeserializer<Struct, Model> deserializer) {
        this.instance = instance;
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    /**
     * Create a Spanner message codec which adapts the provided builder to a Spanner {@link Mutation} and back with
     * default codecs and settings.
     *
     * @param instance Default instance for the model we will be serializing/deserializing.
     * @param <M> Model type for which we will construct or otherwise resolve a collapsed message codec.
     * @return Mutation codec bound to the provided message type.
     */
    @Context
    public static @Nonnull <M extends Message> SpannerCodec<M> forModel(@Nonnull M instance) {
        return forModel(
            instance,
            SpannerDriverSettings.DEFAULTS
        );
    }

    /**
     * Create a Spanner message codec which adapts the provided builder to a Spanner {@link Mutation} and back with
     * default codecs and custom settings.
     *
     * @param instance Default instance for the model we will be serializing/deserializing.
     * @param driverSettings Settings for the Spanner driver itself.
     * @param <M> Model type for which we will construct or otherwise resolve a collapsed message codec.
     * @return Mutation codec bound to the provided message type.
     */
    @Context
    public static @Nonnull <M extends Message> SpannerCodec<M> forModel(@Nonnull M instance,
                                                                        @Nonnull SpannerDriverSettings driverSettings) {
        return forModel(
            instance,
            SpannerMutationSerializer.forModel(
                instance,
                driverSettings
            ),
            SpannerStructDeserializer.forModel(
                instance,
                driverSettings
            )
        );
    }

    /**
     * Create a Spanner message codec which adapts the provided builder to a Spanner {@link Mutation} and back.
     *
     * @param instance Default instance for the model we will be serializing/deserializing.
     * @param serializer Custom serializer to use for this codec.
     * @param deserializer Custom deserializer to use for this codec.
     * @param <M> Model type for which we will construct or otherwise resolve a collapsed message codec.
     * @return Mutation codec bound to the provided message type.
     */
    @Context
    public static @Nonnull <M extends Message> SpannerCodec<M> forModel(
            @Nonnull M instance,
            @Nonnull ModelSerializer<M, Mutation> serializer,
            @Nonnull ModelDeserializer<Struct, M> deserializer) {
        return new SpannerCodec<>(instance, serializer, deserializer);
    }

    /**
     * Specialized entrypoint for converting model instances into {@link Mutation} instances so they may be written to
     * Spanner.
     *
     * @param initial Initial empty mutation to populate with the serialized message result.
     * @param model Model which we intend to store in Spanner.
     * @return Initialized and serialized mutation.
     * @throws IOException If some serialization error occurs while processing the model.
     */
    public @Nonnull Mutation serialize(@Nonnull Mutation.WriteBuilder initial,
                                       @Nonnull Model model) throws IOException {
        return ((SpannerMutationSerializer<Model>) this.serializer).initializeMutation(
            initial
        ).deflate(model);
    }

    // -- Implementation: Codec API -- //

    /** @inheritDoc */
    @Override
    public @Nonnull Model instance() {
        return this.instance;
    }

    /** @inheritDoc */
    @Override
    public @Nonnull ModelSerializer<Model, Mutation> serializer() {
        return this.serializer;
    }

    /** @inheritDoc */
    @Override
    public @Nonnull ModelDeserializer<Struct, Model> deserializer() {
        return this.deserializer;
    }
}
