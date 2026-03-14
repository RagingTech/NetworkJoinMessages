package xyz.earthcow.networkjoinmessages.common.storage;

import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.util.PlayerDataSnapshot;
import xyz.earthcow.networkjoinmessages.common.util.SQLDriverLoader;

import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;

/**
 * Persists per-player preferences and identity using an external SQL server.
 *
 * <p>Supports MySQL, MariaDB, and PostgreSQL. The required JDBC driver JAR is
 * downloaded automatically from Maven Central on first use and cached in
 * {@code <dataFolder>/drivers/}. See {@link SQLDriverLoader}.
 *
 * <p>Connection details are supplied via {@link SQLConfig}.
 * The store keeps a single persistent {@link Connection} and transparently
 * reconnects on failure.
 *
 * <p>All public methods are {@code synchronized} for thread safety.
 */
public class SQLPlayerDataStore extends SQLHandler implements PlayerDataStore {

    // MySQL / MariaDB uses INSERT … ON DUPLICATE KEY UPDATE.
    // PostgreSQL uses INSERT … ON CONFLICT DO UPDATE.
    private final String CREATE_TABLE_MYSQL;
    private final String CREATE_TABLE_POSTGRES;
    private final String SELECT_SQL;
    private final String RESOLVE_SQL;
    private final String UPSERT_MYSQL;
    private final String UPSERT_POSTGRES;

    public SQLPlayerDataStore(CoreLogger logger, SQLConfig sqlConfig, Path dataFolder)
        throws SQLException, SQLDriverLoader.DriverLoadException {
        super(logger, sqlConfig, dataFolder);

        String tableName = sqlConfig.tablePrefix() + "players";

        this.CREATE_TABLE_MYSQL =
            "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "  player_uuid  VARCHAR(36)  NOT NULL, "  +
                "  player_name  VARCHAR(32)  NOT NULL, "  +
                "  silent_state BOOLEAN      NULL, "      +
                "  ignore_join  BOOLEAN      NULL, "      +
                "  ignore_swap  BOOLEAN      NULL, "      +
                "  ignore_leave BOOLEAN      NULL, "      +
                "  PRIMARY KEY (player_uuid)"             +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        this.CREATE_TABLE_POSTGRES =
            "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "  player_uuid  VARCHAR(36)  NOT NULL, "  +
                "  player_name  VARCHAR(32)  NOT NULL, "  +
                "  silent_state BOOLEAN      NULL, "      +
                "  ignore_join  BOOLEAN      NULL, "      +
                "  ignore_swap  BOOLEAN      NULL, "      +
                "  ignore_leave BOOLEAN      NULL, "      +
                "  PRIMARY KEY (player_uuid)"             +
                ")";
        this.SELECT_SQL =
            "SELECT * FROM " + tableName + " WHERE player_uuid = ?";
        this.RESOLVE_SQL =
            "SELECT player_uuid FROM " + tableName + " WHERE LOWER(player_name) = LOWER(?)";
        this.UPSERT_MYSQL =
            "INSERT INTO " + tableName +
                " (player_uuid, player_name, silent_state, ignore_join, ignore_swap, ignore_leave)" +
                " VALUES (?, ?, ?, ?, ?, ?)" +
                " ON DUPLICATE KEY UPDATE" +
                "   player_name  = VALUES(player_name),"  +
                "   silent_state = VALUES(silent_state)," +
                "   ignore_join  = VALUES(ignore_join),"  +
                "   ignore_swap  = VALUES(ignore_swap),"  +
                "   ignore_leave = VALUES(ignore_leave)";
        this.UPSERT_POSTGRES =
            "INSERT INTO " + tableName +
                " (player_uuid, player_name, silent_state, ignore_join, ignore_swap, ignore_leave)" +
                " VALUES (?, ?, ?, ?, ?, ?)" +
                " ON CONFLICT (player_uuid) DO UPDATE SET" +
                "   player_name  = EXCLUDED.player_name,"  +
                "   silent_state = EXCLUDED.silent_state," +
                "   ignore_join  = EXCLUDED.ignore_join,"  +
                "   ignore_swap  = EXCLUDED.ignore_swap,"  +
                "   ignore_leave = EXCLUDED.ignore_leave";

    }

    @Override
    protected String createTableSql() {
        return isPostgres ? CREATE_TABLE_POSTGRES : CREATE_TABLE_MYSQL;
    }

    @Override
    @Nullable
    public synchronized PlayerDataSnapshot getData(UUID playerUuid) {
        if (isConnectionInvalid()) return null;
        try (PreparedStatement ps = connection().prepareStatement(SELECT_SQL)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlayerDataSnapshot(
                        rs.getString("player_name"),
                        rs.getObject("silent_state", Boolean.class),
                        rs.getObject("ignore_join",  Boolean.class),
                        rs.getObject("ignore_swap",  Boolean.class),
                        rs.getObject("ignore_leave", Boolean.class)
                    );
                }
            }
        } catch (SQLException e) {
            logger.severe("[SQLPlayerDataStore] SQL failure querying player data for UUID " + playerUuid + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public synchronized void saveData(UUID playerUuid, PlayerDataSnapshot data) {
        if (isConnectionInvalid()) return;
        String upsert = isPostgres ? UPSERT_POSTGRES : UPSERT_MYSQL;
        try (PreparedStatement ps = connection().prepareStatement(upsert)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, data.playerName());
            ps.setObject(3, data.silentState(), Types.BOOLEAN);
            ps.setObject(4, data.ignoreJoin(),  Types.BOOLEAN);
            ps.setObject(5, data.ignoreSwap(),  Types.BOOLEAN);
            ps.setObject(6, data.ignoreLeave(), Types.BOOLEAN);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[SQLPlayerDataStore] SQL failure saving player data for '" + data.playerName() + "' (" + playerUuid + "): " + e.getMessage());
        }
    }

    @Override
    @Nullable
    public synchronized UUID resolveUuid(String playerName) {
        if (isConnectionInvalid()) return null;
        try (PreparedStatement ps = connection().prepareStatement(RESOLVE_SQL)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("player_uuid"));
                }
            }
        } catch (SQLException e) {
            logger.severe("[SQLPlayerDataStore] SQL failure resolving UUID for player name '" + playerName + "': " + e.getMessage());
        }
        return null;
    }
}