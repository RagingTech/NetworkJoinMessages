package xyz.earthcow.networkjoinmessages.common.player;

import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.util.MessageType;

import java.util.*;

/**
 * Holds all mutable per-player runtime state that must survive a player switching servers.
 * Has no knowledge of config values or message logic — it is a pure data store.
 */
public final class PlayerStateStore {

    private final PluginConfig config;

    private final Map<UUID, String>  previousServer = new HashMap<>();
    private final Map<UUID, Boolean> silentState    = new HashMap<>();
    private final Set<UUID> onlinePlayers  = new HashSet<>();
    private final Set<UUID> noJoinMessage  = new HashSet<>();
    private final Set<UUID> noLeaveMessage = new HashSet<>();
    private final Set<UUID> noSwapMessage  = new HashSet<>();

    public PlayerStateStore(PluginConfig config) {
        this.config = config;
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
