package gust.backend.model;

import com.google.protobuf.Message;


/**
 *
 */
public interface DatabaseDriver<Key extends Message, Model extends Message, Record>
  extends PersistenceDriver<Key, Model, Record> {
}
