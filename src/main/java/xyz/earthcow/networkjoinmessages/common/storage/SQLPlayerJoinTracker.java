package xyz.earthcow.networkjoinmessages.common.storage;

import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.util.SQLDriverLoader;

import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;

/**
 * Tracks which players have ever joined the network using an external SQL server.
 *
 * <p>Supports MySQL, MariaDB, and PostgreSQL. The required JDBC driver JAR is
 * downloaded automatically from Maven Central on first use and cached in
 * {@code <pluginDataFolder>/drivers/}. See {@link SQLDriverLoader}.
 *
 * <p>Connection details are supplied via {@link SQLConfig}. The tracker keeps a
 * single persistent {@link Connection} and transparently reconnects on failure.
 *
 * <p>All public methods are {@code synchronized} for thread safety.
 */
public class SQLPlayerJoinTracker implements PlayerJoinTracker {

    // MySQL / MariaDB uses INSERT … ON DUPLICATE KEY UPDATE.
    // PostgreSQL uses INSERT … ON CONFLICT DO NOTHING.
    private final String CREATE_TABLE_MYSQL;
    private final String CREATE_TABLE_POSTGRES;
    private final String SELECT_SQL;
    private final String UPSERT_MYSQL;
    private final String UPSERT_POSTGRES;

    private final CoreLogger logger;
    private final SQLConfig sqlConfig;
    private final boolean isPostgres;
    private Connection connection;

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

    public SQLPlayerJoinTracker(CoreLogger logger, SQLConfig sqlConfig, Path pluginDataFolder)
        throws SQLException, SQLDriverLoader.DriverLoadException {
        this.logger = logger;
        this.sqlConfig = sqlConfig;
        this.isPostgres = "postgresql".equals(sqlConfig.driver());
        new SQLDriverLoader(logger, pluginDataFolder).ensureLoaded(sqlConfig.driver());

        String tableName = sqlConfig.tablePrefix() + "players_joined";

        this.CREATE_TABLE_MYSQL =
            "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "  player_uuid VARCHAR(36) NOT NULL, " +
                "  player_name VARCHAR(64) NOT NULL, " +
                "  PRIMARY KEY (player_uuid)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        this.CREATE_TABLE_POSTGRES =
            "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "  player_uuid VARCHAR(36) NOT NULL, " +
                "  player_name VARCHAR(64) NOT NULL, " +
                "  PRIMARY KEY (player_uuid)" +
                ")";
        this.SELECT_SQL =
            "SELECT 1 FROM " + tableName + " WHERE player_uuid = ?";
        this.UPSERT_MYSQL =
            "INSERT INTO " + tableName + " (player_uuid, player_name) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE player_name = VALUES(player_name)";
        this.UPSERT_POSTGRES =
            "INSERT INTO " + tableName + " (player_uuid, player_name) VALUES (?, ?) " +
                "ON CONFLICT (player_uuid) DO UPDATE SET player_name = EXCLUDED.player_name";

        setUpConnection();
    }
    
    @Override
    public synchronized boolean hasJoined(UUID playerUuid) {
        if (isConnectionInvalid()) return false;
        try (PreparedStatement ps = connection.prepareStatement(SELECT_SQL)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.severe("[SQLPlayerJoinTracker] SQL failure querying join status for UUID " + playerUuid + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public synchronized void markAsJoined(UUID playerUuid, String playerName) {
        if (isConnectionInvalid()) return;
        String upsert = isPostgres ? UPSERT_POSTGRES : UPSERT_MYSQL;
        try (PreparedStatement ps = connection.prepareStatement(upsert)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[SQLPlayerJoinTracker] SQL failure marking player '" + playerName + "' (" + playerUuid + ") as joined: " + e.getMessage());
        }
    }

    @Override
    public synchronized void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
    
    // --- Internal helpers ---

    /**
     * Opens a new connection and ensures the table exists.
     */
    private void setUpConnection() throws SQLException {
        String url = buildJdbcUrl();
        this.connection = DriverManager.getConnection(url, sqlConfig.username(), sqlConfig.password());
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(isPostgres ? CREATE_TABLE_POSTGRES : CREATE_TABLE_MYSQL);
        }
        logger.debug("[SQLPlayerJoinTracker] Connected to " + sqlConfig.driver() + " at " + sqlConfig.host() + ":" + sqlConfig.port());
    }

    /**
     * Returns {@code true} if the connection is unusable, attempting a reconnect first.
     */
    private boolean isConnectionInvalid() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(sqlConfig.connectionTimeout())) {
                logger.info("[SQLPlayerJoinTracker] Connection lost — attempting reconnect...");
                setUpConnection();
            }
            return false;
        } catch (SQLException e) {
            logger.severe("[SQLPlayerJoinTracker] Cannot reach SQL server at '" + sqlConfig.host() + "': " + e.getMessage());
            return true;
        }
    }

    /**
     * Builds a JDBC URL from the {@link SQLConfig}.
     *
     * <ul>
     *   <li>MySQL:      {@code jdbc:mysql://host:port/db?...}</li>
     *   <li>MariaDB:    {@code jdbc:mariadb://host:port/db?...}</li>
     *   <li>PostgreSQL: {@code jdbc:postgresql://host:port/db?...}</li>
     * </ul>
     */
    private String buildJdbcUrl() {
        // Driver is already expected to be one of "mysql", "mariadb", or "postgresql"
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
}
