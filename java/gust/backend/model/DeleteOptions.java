package gust.backend.model;


/** Describes options specifically involved with deleting existing model entities. */
public interface DeleteOptions extends OperationOptions {
  /** Default set of delete operation options. */
  DeleteOptions DEFAULTS = new DeleteOptions() {};
}
