package gust.backend.model;


/** Describes options specifically involved with updating existing model entities. */
public interface UpdateOptions extends WriteOptions {
  /** Default set of update operation options. */
  UpdateOptions DEFAULTS = new UpdateOptions() {};
}
