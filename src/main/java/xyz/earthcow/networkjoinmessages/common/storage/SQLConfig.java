package xyz.earthcow.networkjoinmessages.common.storage;

/**
 * Immutable value object carrying the SQL connection parameters read from config.
 */
public record SQLConfig(
    String host,
    int port,
    String database,
    String username,
    String password,
    String driver,
    String tablePrefix,
    boolean useSSL,
    int connectionTimeout
) {}
