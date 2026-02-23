package xyz.earthcow.networkjoinmessages.common.broadcast;

import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.abstraction.PremiumVanish;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.modules.SayanVanishHook;

import java.util.*;

/**
 * Formats message templates by resolving player-count and server-name placeholders.
 * Has no knowledge of how messages are dispatched or who receives them.
 *
 * <p>Resolves: %playercount_server%, %playercount_network%, %playercount_from%, %playercount_to%,
 * %to%, %to_clean%, %from%, %from_clean%.
 */
public final class MessageFormatter {

    private final CorePlugin plugin;
    private final PluginConfig config;

    @Nullable
    private final SayanVanishHook sayanVanishHook;

    public MessageFormatter(CorePlugin plugin, PluginConfig config, @Nullable SayanVanishHook sayanVanishHook) {
        this.plugin = plugin;
        this.config = config;
        this.sayanVanishHook = sayanVanishHook;
    }

    // --- Message template formatters ---

    public String formatFirstJoinMessage(CorePlayer player) {
        return applyPlayerCountPlaceholders(config.getFirstJoinNetworkMessage(), player, false);
    }

    public String formatJoinMessage(CorePlayer player) {
        return applyPlayerCountPlaceholders(config.getJoinNetworkMessage(), player, false);
    }

    public String formatLeaveMessage(CorePlayer player) {
        return applyPlayerCountPlaceholders(config.getLeaveNetworkMessage(), player, true);
    }

    /**
     * Formats the swap server message template, replacing server name and player-count placeholders.
     */
    public String formatSwapMessage(CorePlayer player, String fromName, String toName) {
        String displayFrom = config.getServerDisplayName(fromName);
        String displayTo   = config.getServerDisplayName(toName);

        String msg = config.getSwapServerMessage()
            .replace("%to%",         displayTo)
            .replace("%to_clean%",   toName)
            .replace("%from%",       displayFrom)
            .replace("%from_clean%", fromName);

        if (msg.contains("%playercount_from%")) {
            msg = msg.replace("%playercount_from%", getServerPlayerCount(fromName, true, player));
        }
        if (msg.contains("%playercount_to%")) {
            msg = msg.replace("%playercount_to%", getServerPlayerCount(toName, false, player));
        }
        if (msg.contains("%playercount_network%")) {
            msg = msg.replace("%playercount_network%", getNetworkPlayerCount(player, false));
        }
        return msg;
    }

    /**
     * Applies %playercount_server% and %playercount_network% to a join/leave message template.
     */
    private String applyPlayerCountPlaceholders(String message, CorePlayer player, boolean leaving) {
        if (message.contains("%playercount_server%")) {
            message = message.replace("%playercount_server%", getServerPlayerCount(player, leaving));
        }
        if (message.contains("%playercount_network%")) {
            message = message.replace("%playercount_network%", getNetworkPlayerCount(player, leaving));
        }
        return message;
    }

    // --- Player count helpers ---

    public String getServerPlayerCount(CorePlayer player, boolean leaving) {
        return getServerPlayerCount(player.getCurrentServer(), leaving, player);
    }

    public String getServerPlayerCount(String serverName, boolean leaving, CorePlayer player) {
        return getServerPlayerCount(plugin.getServer(serverName), leaving, player);
    }

    public String getServerPlayerCount(@Nullable CoreBackendServer server, boolean leaving, @Nullable CorePlayer player) {
        if (server == null) return leaving ? "0" : "1";
        return computePlayerCount(server.getPlayersConnected(), player, leaving);
    }

    public String getNetworkPlayerCount(CorePlayer player, boolean leaving) {
        return computePlayerCount(plugin.getAllPlayers(), player, leaving);
    }

    /**
     * Computes the visible player count for a collection of players, accounting for vanish integrations
     * and the pending join/leave of the given player.
     */
    private String computePlayerCount(Collection<CorePlayer> players, @Nullable CorePlayer subject, boolean leaving) {
        Set<UUID> vanishedIds = collectVanishedPlayers();

        List<CorePlayer> visible = players.stream()
            .filter(p -> !vanishedIds.contains(p.getUniqueId()))
            .toList();

        int count = visible.size();

        if (subject != null && !vanishedIds.contains(subject.getUniqueId())) {
            UUID subjectId = subject.getUniqueId();
            boolean isPresent = visible.stream().anyMatch(p -> p.getUniqueId().equals(subjectId));
            if (isPresent && leaving)    count--;
            else if (!isPresent && !leaving) count++;
        }

        return Integer.toString(count);
    }

    /**
     * Builds the combined set of vanished player UUIDs from all enabled vanish integrations.
     */
    private Set<UUID> collectVanishedPlayers() {
        Set<UUID> vanished = new HashSet<>();

        PremiumVanish pv = plugin.getVanishAPI();
        if (pv != null && config.isPVRemoveVanishedPlayersFromPlayerCount()) {
            vanished.addAll(pv.getInvisiblePlayers());
        }
        if (sayanVanishHook != null && config.isSVRemoveVanishedPlayersFromPlayerCount()) {
            vanished.addAll(sayanVanishHook.getVanishedPlayers());
        }
        return vanished;
    }
}
