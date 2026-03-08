package xyz.earthcow.networkjoinmessages.common.player;

import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
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

    private final CorePlugin plugin;
    private final PluginConfig config;
    private final PlayerDataStore store;

    private final Map<UUID, String>  previousServer = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> silentState    = new ConcurrentHashMap<>();
    private final Set<UUID> onlinePlayers  = ConcurrentHashMap.newKeySet();
    private final Set<UUID> noJoinMessage  = ConcurrentHashMap.newKeySet();
    private final Set<UUID> noLeaveMessage = ConcurrentHashMap.newKeySet();
    private final Set<UUID> noSwapMessage  = ConcurrentHashMap.newKeySet();

    public PlayerStateStore(CorePlugin plugin, PluginConfig config, @Nullable PlayerDataStore store) {
        this.plugin = plugin;
        this.config = config;
        this.store = store;
    }

    public void loadData(UUID playerUuid, String playerName) {
        if (store == null) return;
        PlayerDataSnapshot playerData = store.getData(playerUuid);
        if (playerData == null) {
            if (config.isIgnoreJoinByDefault())  noJoinMessage.add(playerUuid);
            if (config.isIgnoreSwapByDefault())  noSwapMessage.add(playerUuid);
            if (config.isIgnoreLeaveByDefault()) noLeaveMessage.add(playerUuid);
            store.saveData(playerUuid,
                new PlayerDataSnapshot(playerName, null, null, null, null));
        } else {
            if (playerData.silentState() != null) {
                silentState.put(playerUuid, playerData.silentState());
            }

            loadFlag(playerData.ignoreJoin(),  config.isIgnoreJoinByDefault(),  noJoinMessage,  playerUuid);
            loadFlag(playerData.ignoreSwap(),  config.isIgnoreSwapByDefault(),  noSwapMessage,  playerUuid);
            loadFlag(playerData.ignoreLeave(), config.isIgnoreLeaveByDefault(), noLeaveMessage, playerUuid);
        }
    }

    private void loadFlag(Boolean flag, boolean ignoreByDefault, Set<UUID> set, UUID uuid) {
        if (flag == null) flag = ignoreByDefault;
        if (flag) set.add(uuid);
        else      set.remove(uuid);
    }

    private void saveData(UUID playerUuid, String playerName) {
        if (store == null) return;
        PlayerDataSnapshot newPlayerData = new PlayerDataSnapshot(
            playerName,
            silentState.get(playerUuid),
            determineSaveState(noJoinMessage,  config.isIgnoreJoinByDefault(),  playerUuid),
            determineSaveState(noSwapMessage,  config.isIgnoreSwapByDefault(),  playerUuid),
            determineSaveState(noLeaveMessage, config.isIgnoreLeaveByDefault(), playerUuid)
        );
        plugin.runTaskAsync(() -> store.saveData(playerUuid, newPlayerData));
    }

    /**
     * Determines the value to persist for a single ignore-message flag.
     *
     * <p>Saves {@code null} when the player's current state matches the configured
     * default
     *
     * @param set             the suppression set for this message type
     * @param ignoreByDefault the configured default for this message type
     * @param uuid            the player whose state is being saved
     * @return {@code true} if explicitly suppressing against the default,
     *         {@code false} if explicitly receiving against the default,
     *         {@code null} if the player's state matches the default
     */
    private Boolean determineSaveState(Set<UUID> set, boolean ignoreByDefault, UUID uuid) {
        if (set.contains(uuid)) {
            if (ignoreByDefault) {
                return null;
            }
            return true;
        }
        if (ignoreByDefault) {
            return false;
        }
        return null;
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
        saveData(player.getUniqueId(), player.getName());
    }

    // --- Per-player message suppression ---

    public void setSendMessageState(String type, CorePlayer player, boolean enabled) {
        setSendMessageState(type, player.getUniqueId(), player.getName(), enabled);
    }

    /**
     * Updates in-memory suppression sets (effective immediately if the player
     * is online) and persists to the backing store so the state is restored on
     * their next login (after a proxy reboot) via {@link #loadData}.
     *
     * @param type       one of {@code "all"}, {@code "join"}, {@code "leave"}, {@code "swap"}
     * @param targetUuid the UUID of the player whose state should be changed
     * @param playerName the player's last-known name, used as the {@code player_name}
     *                   column value when writing the persistent record
     * @param enabled    {@code true} to re-enable messages, {@code false} to suppress
     */
    public void setSendMessageState(String type, UUID targetUuid, String playerName, boolean enabled) {
        switch (type) {
            case "all"   -> {
                updateSet(noSwapMessage,  targetUuid, enabled);
                updateSet(noJoinMessage,  targetUuid, enabled);
                updateSet(noLeaveMessage, targetUuid, enabled);
            }
            case "join"  -> updateSet(noJoinMessage,  targetUuid, enabled);
            case "leave" -> updateSet(noLeaveMessage, targetUuid, enabled);
            case "swap"  -> updateSet(noSwapMessage,  targetUuid, enabled);
        }
        saveData(targetUuid, playerName);
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
