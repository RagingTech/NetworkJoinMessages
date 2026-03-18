package xyz.earthcow.networkjoinmessages.common.storage;

import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.util.PlayerDataSnapshot;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persists per-player preferences and identity using an embedded H2 database.
 *
 * <p>The database file is created in the plugin's data folder at the path
 * supplied to the constructor. A single table {@code players} stores one row
 * per UUID; all preference columns are nullable so that {@code null} can be
 * distinguished from an explicit {@code false}.
 *
 * <p>All public methods are {@code synchronized} for thread safety, consistent
 * with the project's other H2-backed stores.
 */
public class H2PlayerDataStore extends H2Handler implements PlayerDataStore {

    private static final String CREATE_TABLE_SQL =
        "CREATE TABLE IF NOT EXISTS players (" +
            "player_uuid  VARCHAR(36) PRIMARY KEY, " +
            "player_name  VARCHAR(32) NOT NULL, "    +
            "silent_state BOOLEAN NULL, "            +
            "ignore_join  BOOLEAN NULL, "            +
            "ignore_swap  BOOLEAN NULL, "            +
            "ignore_leave BOOLEAN NULL"              +
        ")";

    private static final String SELECT_SQL =
        "SELECT * FROM players WHERE player_uuid = ?";

    private static final String RESOLVE_SQL =
        "SELECT player_uuid FROM players WHERE LOWER(player_name) = LOWER(?)";

    private static final String EXPORT_SQL =
        "SELECT * FROM players";

    // MERGE upserts the identity columns on first insert, then UPDATE sets all
    // preference columns so an existing row is fully overwritten on save.
    private static final String UPSERT_SQL =
        "MERGE INTO players (player_uuid, player_name, silent_state, ignore_join, ignore_swap, ignore_leave) " +
            "KEY(player_uuid) VALUES (?, ?, ?, ?, ?, ?)";


    public H2PlayerDataStore(CoreLogger logger, String dbPath) throws SQLException {
        super(logger, dbPath);
    }

    @Override
    protected String createTableSql() {
        return CREATE_TABLE_SQL;
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
            logger.severe("SQL failure: Could not query player data for UUID " + playerUuid + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public synchronized void saveData(UUID playerUuid, PlayerDataSnapshot data) {
        if (isConnectionInvalid()) return;
        try (PreparedStatement ps = connection().prepareStatement(UPSERT_SQL)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, data.playerName());
            ps.setObject(3, data.silentState(), Types.BOOLEAN);
            ps.setObject(4, data.ignoreJoin(),  Types.BOOLEAN);
            ps.setObject(5, data.ignoreSwap(),  Types.BOOLEAN);
            ps.setObject(6, data.ignoreLeave(), Types.BOOLEAN);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("SQL failure: Could not save player data for '" + data.playerName() + "' (" + playerUuid + "): " + e.getMessage());
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
            logger.severe("SQL failure: Could not resolve UUID for player name '" + playerName + "': " + e.getMessage());
        }
        return null;
    }

    @Override
    public synchronized Map<UUID, PlayerDataSnapshot> exportAll() {
        Map<UUID, PlayerDataSnapshot> result = new LinkedHashMap<>();
        if (isConnectionInvalid()) return result;
        try (Statement stmt = connection().createStatement();
             ResultSet rs   = stmt.executeQuery(EXPORT_SQL)) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                result.put(uuid, new PlayerDataSnapshot(
                    rs.getString("player_name"),
                    rs.getObject("silent_state", Boolean.class),
                    rs.getObject("ignore_join",  Boolean.class),
                    rs.getObject("ignore_swap",  Boolean.class),
                    rs.getObject("ignore_leave", Boolean.class)
                ));
            }
        } catch (SQLException e) {
            logger.severe("[H2PlayerDataStore] SQL failure during exportAll(): " + e.getMessage());
        }
        return result;
    }
}
