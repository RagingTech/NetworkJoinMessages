package xyz.earthcow.networkjoinmessages.common.storage;

import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.util.SQLDriverLoader;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

abstract class SQLHandler implements AutoCloseable {
    protected final CoreLogger logger;
    protected final SQLConfig sqlConfig;
    protected final boolean isPostgres;
    private final String logPrefix;
    private Connection connection;

    protected SQLHandler(CoreLogger logger, SQLConfig sqlConfig, Path dataFolder)
        throws SQLException, SQLDriverLoader.DriverLoadException {
        this.logger = logger;
        this.sqlConfig = sqlConfig;
        this.isPostgres = "postgresql".equals(sqlConfig.driver());
        this.logPrefix = "[" + getClass().getSimpleName() + "] ";
        new SQLDriverLoader(logger, dataFolder).ensureLoaded(sqlConfig.driver());
        setUpConnection();
    }

    protected abstract String createTableSql();

    // Provided to subclasses
    protected synchronized Connection connection() {
        return this.connection;
    }

    /**
     * Returns {@code true} if the connection is unusable, attempting a reconnect first.
     */
    protected synchronized boolean isConnectionInvalid() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(sqlConfig.connectionTimeout())) {
                logger.info(logPrefix + "Connection lost — attempting reconnect...");
                setUpConnection();
            }
            return false;
        } catch (SQLException e) {
            logger.severe(logPrefix + "Cannot reach SQL server at '" + sqlConfig.host() + "': " + e.getMessage());
            return true;
        }
    }

    /**
     * Opens a new connection and ensures the table exists.
     */
    private void setUpConnection() throws SQLException {
        String url = buildJdbcUrl();
        this.connection = DriverManager.getConnection(url, sqlConfig.username(), sqlConfig.password());
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSql());
        }
        logger.debug(logPrefix + "Connected to " + sqlConfig.driver() + " at " + sqlConfig.host() + ":" + sqlConfig.port());
    }

    private String buildJdbcUrl() {
        StringBuilder url = new StringBuilder()
            .append("jdbc:").append(sqlConfig.driver()).append("://")
            .append(sqlConfig.host()).append(':').append(sqlConfig.port())
            .append('/').append(sqlConfig.database())
            .append("?autoReconnect=true")
            .append("&connectTimeout=").append(sqlConfig.connectionTimeout() * 1000)
            .append("&allowPublicKeyRetrieval=true");

        if (!isPostgres) {
            url.append("&useSSL=").append(sqlConfig.useSSL());
            url.append("&characterEncoding=utf8");
        }

        return url.toString();
    }

    @Override
    public synchronized void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

}
