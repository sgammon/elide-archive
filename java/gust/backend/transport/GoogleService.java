package gust.backend.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;


/** Enumerates Google Cloud services with built-in managed transport support. */
public enum GoogleService {
  /** Google Cloud Pub-Sub (token: <pre>pubsub</pre>). */
  PUBSUB("pubsub", null),

  /** Google Cloud Storage (token: <pre>storage</pre>). */
  STORAGE("storage", null),

  /** Google Cloud Firestore (token: <pre>firestore</pre>) */
  FIRESTORE("firestore", null);

  /** Prefix at which the specified service may be configured. */
  private final @Nonnull String token;

  /** Config bindings class for the provided service. */
  private final @Nullable Class<GoogleTransportConfig> configType;

  /**
   * Private constructor.
   *
   * @param token Configuration binding prefix.
   * @param configType Configuration class type.
   */
  GoogleService(@Nonnull String token,
                @Nullable Class<GoogleTransportConfig> configType) {
    this.token = token;
    this.configType = configType;
  }

  // -- Getters -- //
  /**
   * @return Prefix at which the specified service can be configured.
   */
  public @Nonnull String getToken() {
    return token;
  }

  /**
   * @return Configuration bindings class for the provided service, if supported.
   */
  public @Nonnull Optional<Class<GoogleTransportConfig>> getConfigType() {
    if (configType == null)
      return Optional.empty();
    return Optional.of(configType);
  }
}
