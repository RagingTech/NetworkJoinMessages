package xyz.earthcow.networkjoinmessages.common.storage;

import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.util.SQLDriverLoader;

import java.nio.file.Path;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
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
public class SQLPlayerJoinTracker extends SQLHandler implements PlayerJoinTracker {

    // MySQL / MariaDB uses INSERT … ON DUPLICATE KEY UPDATE.
    // PostgreSQL uses INSERT … ON CONFLICT DO NOTHING.
    private final String CREATE_TABLE_MYSQL;
    private final String CREATE_TABLE_POSTGRES;
    private final String SELECT_SQL;
    private final String UPSERT_MYSQL;
    private final String UPSERT_POSTGRES;
    private final String EXPORT_SQL;

    public SQLPlayerJoinTracker(CoreLogger logger, SQLConfig sqlConfig, Path dataFolder)
        throws SQLException, SQLDriverLoader.DriverLoadException {
        super(logger, sqlConfig, dataFolder);

        String tableName = sqlConfig.tablePrefix() + "players_joined";

        this.CREATE_TABLE_MYSQL =
            "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "  player_uuid VARCHAR(36) NOT NULL, " +
                "  player_name VARCHAR(32) NOT NULL, " +
                "  PRIMARY KEY (player_uuid)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        this.CREATE_TABLE_POSTGRES =
            "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "  player_uuid VARCHAR(36) NOT NULL, " +
                "  player_name VARCHAR(32) NOT NULL, " +
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
        this.EXPORT_SQL =
            "SELECT player_uuid, player_name FROM " + tableName;

    }

    @Override
    protected String createTableSql() {
        return isPostgres ? CREATE_TABLE_POSTGRES : CREATE_TABLE_MYSQL;
    }
    
    @Override
    public synchronized boolean hasJoined(UUID playerUuid) {
        if (isConnectionInvalid()) return false;
        try (PreparedStatement ps = connection().prepareStatement(SELECT_SQL)) {
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
        try (PreparedStatement ps = connection().prepareStatement(upsert)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[SQLPlayerJoinTracker] SQL failure marking player '" + playerName + "' (" + playerUuid + ") as joined: " + e.getMessage());
        }
    }

    @Override
    public synchronized Map<UUID, String> exportAll() {
        Map<UUID, String> result = new LinkedHashMap<>();
        if (isConnectionInvalid()) return result;
        try (Statement stmt = connection().createStatement();
             ResultSet rs   = stmt.executeQuery(EXPORT_SQL)) {
            while (rs.next()) {
                result.put(
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name")
                );
            }
        } catch (SQLException e) {
            logger.severe("[SQLPlayerJoinTracker] SQL failure during exportAll(): " + e.getMessage());
        }
        return result;
    }
}
