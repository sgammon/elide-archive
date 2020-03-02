package gust.backend.model;

import com.google.protobuf.Message;

import javax.annotation.Nonnull;


/**
 * Extends the standard {@link ModelAdapter} interface with rich persistence features, including querying, indexing, and
 * other stuff one would expect when interacting with a full database.
 *
 * @param <Key> Type of key used to uniquely address models.
 * @param <Model> Message type which this database adapter is handling.
 */
public interface DatabaseAdapter<Key extends Message, Model extends Message, DataRecord>
  extends ModelAdapter<Key, Model, DataRecord> {
  /**
   * Return the lower-level {@link DatabaseDriver} powering this adapter. The driver is responsible for communicating
   * with the actual database or storage service, either via local stubs/emulators or a production API.
   *
   * @return Database driver instance currently in use by this model adapter.
   */
  @Nonnull DatabaseDriver<Key, Model, DataRecord> engine();

  /** {@inheritDoc} */
  @Override
  default @Nonnull Key generateKey(@Nonnull Message instance) {
    return engine().generateKey(instance);
  }
}
