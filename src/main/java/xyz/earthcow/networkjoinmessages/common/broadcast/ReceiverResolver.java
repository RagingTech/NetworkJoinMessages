package xyz.earthcow.networkjoinmessages.common.broadcast;

import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.util.MessageType;

import java.util.*;

/**
 * Resolves which players should receive a broadcast message for a given event.
 * Encapsulates blacklist/whitelist rules and per-server suppression lists.
 * Has no knowledge of player state or message formatting.
 */
public final class ReceiverResolver {

    private final CorePlugin plugin;
    private final PluginConfig config;

    public ReceiverResolver(CorePlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    // --- Audience resolution ---

    public List<CorePlayer> getSwapReceivers(String to, String from) {
        return resolve(config.isSwapViewableByJoined(), config.isSwapViewableByLeft(), config.isSwapViewableByOther(), to, from);
    }

    public List<CorePlayer> getFirstJoinReceivers(String server) {
        return resolve(config.isFirstJoinViewableByJoined(), true, config.isFirstJoinViewableByOther(), server, null);
    }

    public List<CorePlayer> getJoinReceivers(String server) {
        return resolve(config.isJoinViewableByJoined(), true, config.isJoinViewableByOther(), server, null);
    }

    public List<CorePlayer> getLeaveReceivers(String server) {
        return resolve(config.isLeaveViewableByLeft(), true, config.isLeaveViewableByOther(), server, null);
    }

    /**
     * Computes the list of players who should see a message, based on server membership and visibility flags.
     *
     * @param viewableByJoined whether the destination server's players see the message
     * @param viewableByLeft   whether the origin server's players see the message (ignored when {@code fromServer} is null)
     * @param viewableByOther  whether all other servers' players see the message
     * @param toServer         destination server name, or null
     * @param fromServer       origin server name, or null
     */
    private List<CorePlayer> resolve(
            boolean viewableByJoined,
            boolean viewableByLeft,
            boolean viewableByOther,
            @Nullable String toServer,
            @Nullable String fromServer
    ) {
        if (viewableByJoined && (viewableByLeft || fromServer == null) && viewableByOther) {
            return new ArrayList<>(plugin.getAllPlayers());
        }

        if (viewableByOther) {
            List<CorePlayer> receivers = new ArrayList<>(plugin.getAllPlayers());
            if (!viewableByJoined && toServer != null)   receivers.removeAll(getServerPlayers(toServer));
            if (!viewableByLeft   && fromServer != null) receivers.removeAll(getServerPlayers(fromServer));
            return receivers;
        }

        List<CorePlayer> receivers = new ArrayList<>();
        if (viewableByJoined && toServer != null)   receivers.addAll(getServerPlayers(toServer));
        if (viewableByLeft   && fromServer != null) receivers.addAll(getServerPlayers(fromServer));
        return receivers;
    }

    // --- Blacklist / whitelist checks ---

    /**
     * Returns true if the player's current server is blocked by the blacklist/whitelist rules.
     */
    public boolean isBlacklisted(CorePlayer player) {
        String server = player.getCurrentServer().getName();
        boolean listed = config.getBlacklistedServers().contains(server);
        boolean result = config.isUseBlacklistAsWhitelist() != listed;
        plugin.getCoreLogger().debug(String.format(
            "Blacklist check for player %s on server %s: listed=%s, mode=%s, result=%s",
            player.getName(), server, listed,
            config.isUseBlacklistAsWhitelist() ? "WHITELIST" : "BLACKLIST", result
        ));
        return result;
    }

    /**
     * Returns true if the given server pair is blocked by the blacklist/whitelist and
     * {@code SwapServerMessageRequires} rules.
     */
    public boolean isBlacklisted(@Nullable String from, @Nullable String to) {
        boolean fromListed = from != null && config.getBlacklistedServers().contains(from);
        boolean toListed   = to   != null && config.getBlacklistedServers().contains(to);

        boolean result = switch (config.getSwapServerMessageRequires()) {
            case "JOINED" -> toListed;
            case "LEFT"   -> fromListed;
            case "ANY"    -> fromListed || toListed;
            case "BOTH"   -> fromListed && toListed;
            default -> {
                plugin.getCoreLogger().warn("Unrecognized SwapServerMessageRequires value: "
                    + config.getSwapServerMessageRequires());
                yield false;
            }
        };

        boolean finalResult = config.isUseBlacklistAsWhitelist() != result;
        plugin.getCoreLogger().debug(String.format(
            "Blacklist check for swap (from=%s, to=%s): fromListed=%s, toListed=%s, mode=%s, requires=%s, result=%s",
            from, to, fromListed, toListed,
            config.isUseBlacklistAsWhitelist() ? "WHITELIST" : "BLACKLIST",
            config.getSwapServerMessageRequires(), finalResult
        ));
        return finalResult;
    }

    // --- Per-server suppression ---

    /**
     * Returns the UUIDs of all players currently on servers where the given message type is suppressed.
     */
    public Set<UUID> getServerSuppressedPlayers(MessageType type) {
        List<String> disabledServers = switch (type) {
            case FIRST_JOIN -> config.getServerFirstJoinMessageDisabled();
            case JOIN       -> config.getServerJoinMessageDisabled();
            case LEAVE      -> config.getServerLeaveMessageDisabled();
            default -> {
                plugin.getCoreLogger().debug("No server suppression list for message type: " + type);
                yield Collections.emptyList();
            }
        };

        Set<UUID> suppressed = new HashSet<>();
        for (String serverName : disabledServers) {
            CoreBackendServer server = plugin.getServer(serverName);
            if (server != null) {
                server.getPlayersConnected().forEach(p -> suppressed.add(p.getUniqueId()));
            } else {
                plugin.getCoreLogger().debug("Suppressed server not found or offline: " + serverName);
            }
        }
        return suppressed;
    }

    // --- Helper ---

    public List<CorePlayer> getServerPlayers(String serverName) {
        CoreBackendServer server = plugin.getServer(serverName);
        return server != null ? server.getPlayersConnected() : Collections.emptyList();
    }
}
