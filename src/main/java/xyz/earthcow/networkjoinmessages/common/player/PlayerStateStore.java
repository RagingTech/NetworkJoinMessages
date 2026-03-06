package xyz.earthcow.networkjoinmessages.common.player;

import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.MessageType;
import xyz.earthcow.networkjoinmessages.common.storage.PlayerDataStore;
import xyz.earthcow.networkjoinmessages.common.util.PlayerDataSnapshot;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds all mutable per-player runtime state that must survive a player switching servers.
 * Has no knowledge of config values or message logic — it is a pure data store.
 *
 * <p>All collections are thread-safe: {@code onServerConnected} runs on an async thread pool
 * and may access this store concurrently with disconnect and command handlers.
 */
public final class PlayerStateStore {

    private final PluginConfig config;
    private final PlayerDataStore store;

    private final Map<UUID, String>  previousServer = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> silentState    = new ConcurrentHashMap<>();
    private final Set<UUID> onlinePlayers  = ConcurrentHashMap.newKeySet();
    private final Set<UUID> noJoinMessage  = ConcurrentHashMap.newKeySet();
    private final Set<UUID> noLeaveMessage = ConcurrentHashMap.newKeySet();
    private final Set<UUID> noSwapMessage  = ConcurrentHashMap.newKeySet();

    public PlayerStateStore(PluginConfig config, @Nullable PlayerDataStore store) {
        this.config = config;
        this.store = store;
    }

    public void loadData(UUID playerUuid, String playerName) {
        if (store == null) return;
        PlayerDataSnapshot playerData = store.getData(playerUuid);
        if (playerData == null) {
            PlayerDataSnapshot newPlayerData = new PlayerDataSnapshot(
                playerName,
                silentState.get(playerUuid),
                noJoinMessage.contains(playerUuid) ? true : null,
                noSwapMessage.contains(playerUuid) ? true : null,
                noLeaveMessage.contains(playerUuid) ? true : null
            );
            store.saveData(playerUuid, newPlayerData);
        } else {
            if (playerData.silentState() != null) {
                silentState.put(playerUuid, playerData.silentState());
            }
            if (playerData.ignoreJoin() != null && playerData.ignoreJoin()) {
                noJoinMessage.add(playerUuid);
            }
            if (playerData.ignoreSwap() != null && playerData.ignoreSwap()) {
                noSwapMessage.add(playerUuid);
            }
            if (playerData.ignoreLeave() != null && playerData.ignoreLeave()) {
                noLeaveMessage.add(playerUuid);
            }
        }
    }

    // --- Online tracking ---

    public boolean isConnected(CorePlayer player) {
        return onlinePlayers.contains(player.getUniqueId());
    }

    public void setConnected(CorePlayer player, boolean connected) {
        if (connected) onlinePlayers.add(player.getUniqueId());
        else           onlinePlayers.remove(player.getUniqueId());
    }

    // --- Previous server ---

    public String getFrom(CorePlayer player) {
        return previousServer.getOrDefault(player.getUniqueId(), player.getCurrentServer().getName());
    }

    public void setFrom(CorePlayer player, String serverName) {
        previousServer.put(player.getUniqueId(), serverName);
    }

    // --- Silent join state ---

    /**
     * Returns the effective silent state for the given player.
     * Initialises to the configured default on first access.
     */
    public boolean getSilentState(CorePlayer player) {
        if (!player.hasPermission("networkjoinmessages.silent")) return false;
        return silentState.computeIfAbsent(player.getUniqueId(), id -> config.isSilentJoinDefaultState());
    }

    public void setSilentState(CorePlayer player, boolean state) {
        silentState.put(player.getUniqueId(), state);
    }

    // --- Per-player message suppression ---

    public void setSendMessageState(String type, UUID id, boolean enabled) {
        switch (type) {
            case "all"   -> { updateSet(noSwapMessage, id, enabled); updateSet(noJoinMessage, id, enabled); updateSet(noLeaveMessage, id, enabled); }
            case "join"  -> updateSet(noJoinMessage,  id, enabled);
            case "leave" -> updateSet(noLeaveMessage, id, enabled);
            case "swap"  -> updateSet(noSwapMessage,  id, enabled);
        }
    }

    private void updateSet(Set<UUID> set, UUID id, boolean enabled) {
        if (enabled) set.remove(id); else set.add(id);
    }

    /**
     * Returns the set of UUIDs whose messages of the given type should be suppressed.
     * Returns a live view — callers must not mutate it.
     */
    public Set<UUID> getSuppressedPlayers(MessageType type) {
        return switch (type) {
            case JOIN, FIRST_JOIN -> noJoinMessage;
            case SWAP             -> noSwapMessage;
            case LEAVE            -> noLeaveMessage;
        };
    }
}
