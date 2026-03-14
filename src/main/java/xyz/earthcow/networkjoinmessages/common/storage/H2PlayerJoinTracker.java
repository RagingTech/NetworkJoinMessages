package xyz.earthcow.networkjoinmessages.common.storage;

import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;

import java.sql.*;
import java.util.UUID;

/**
 * Tracks which players have ever joined the network using an embedded H2 database.
 */
public class H2PlayerJoinTracker extends H2Handler implements PlayerJoinTracker {

    private static final String CREATE_TABLE_SQL =
        "CREATE TABLE IF NOT EXISTS players_joined (" +
        "  player_uuid VARCHAR(36) PRIMARY KEY, " +
        "  player_name VARCHAR(32) NOT NULL" +
        ")";

    private static final String SELECT_SQL =
        "SELECT 1 FROM players_joined WHERE player_uuid = ?";

    private static final String UPSERT_SQL =
        "MERGE INTO players_joined (player_uuid, player_name) KEY(player_uuid) VALUES (?, ?)";

    public H2PlayerJoinTracker(CoreLogger logger, String dbPath) throws SQLException {
        super(logger, dbPath);
    }

    @Override
    protected String createTableSql() {
        return CREATE_TABLE_SQL;
    }

    /**
     * Returns true if the given player UUID has previously been recorded as joined.
     */
    public synchronized boolean hasJoined(UUID playerUuid) {
        if (isConnectionInvalid()) return false;
        try (PreparedStatement ps = connection().prepareStatement(SELECT_SQL)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.severe("SQL failure: Could not query join status for UUID " + playerUuid);
            return false;
        }
    }

    /**
     * Records that a player has joined. Safe to call multiple times for the same UUID (upsert).
     */
    public synchronized void markAsJoined(UUID playerUuid, String playerName) {
        if (isConnectionInvalid()) return;
        try (PreparedStatement ps = connection().prepareStatement(UPSERT_SQL)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("SQL failure: Could not mark player '" + playerName + "' (" + playerUuid + ") as joined");
        }
    }
}
