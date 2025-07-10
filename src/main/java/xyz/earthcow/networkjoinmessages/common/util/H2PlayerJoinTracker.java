package xyz.earthcow.networkjoinmessages.common.util;

import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;

import java.sql.*;
import java.util.UUID;

public class H2PlayerJoinTracker implements AutoCloseable {
    private final Connection connection;

    public H2PlayerJoinTracker(String dbPath) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:h2:" + dbPath);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS players_joined (" +
                "player_uuid VARCHAR(36) PRIMARY KEY, " +
                "player_name VARCHAR(64) NOT NULL)");
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    public boolean hasJoined(UUID playerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT 1 FROM players_joined WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            NetworkJoinMessagesCore.getInstance().getPlugin().getCoreLogger().severe(
                "SQL Failure: Failed to determine if " + playerUuid + " has joined before");
            return false;
        }
    }

    public void markAsJoined(UUID playerUuid, String playerName) {
        try (PreparedStatement ps = connection.prepareStatement(
            "MERGE INTO players_joined (player_uuid, player_name) KEY(player_uuid) VALUES (?, ?)")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            NetworkJoinMessagesCore.getInstance().getPlugin().getCoreLogger().severe(
                "SQL Failure: Failed to mark player joined for player " + playerName);
        }
    }
}
