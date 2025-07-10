package xyz.earthcow.networkjoinmessages.common.util;

import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;

import java.sql.*;
import java.util.UUID;

public class SQLitePlayerJoinTracker {
    private final Connection connection;

    public SQLitePlayerJoinTracker(String dbPath) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS players_joined (`player_uuid` TEXT PRIMARY KEY, `player_name` TEXT NOT NULL);");
        }
    }

    public boolean hasJoined(UUID playerUuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM players_joined WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            NetworkJoinMessagesCore.getInstance().getPlugin().getCoreLogger().severe("SQL Failure: Failed to determine if " + playerUuid + " has joined before");
            return false;
        }
    }

    public void markAsJoined(UUID playerUuid, String playerName) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT OR IGNORE INTO players_joined (player_uuid, player_name) VALUES (?, ?)")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            NetworkJoinMessagesCore.getInstance().getPlugin().getCoreLogger().severe("SQL Failure: Failed to mark player joined for player " + playerName);
        }
    }
}
