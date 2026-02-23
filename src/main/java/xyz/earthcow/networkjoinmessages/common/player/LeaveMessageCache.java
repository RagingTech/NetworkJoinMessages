package xyz.earthcow.networkjoinmessages.common.player;

import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.broadcast.MessageFormatter;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.util.PlaceholderResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pre-computes and caches the formatted leave message for each online player.
 *
 * <p>Because a player's leave message cannot be computed after disconnect (no live
 * placeholder data), it is formatted periodically while the player is connected and
 * stored on the player object, ready to be sent immediately on disconnect.
 */
public final class LeaveMessageCache {

    private final CorePlugin plugin;
    private final PluginConfig config;
    private final MessageFormatter messageFormatter;
    private final PlaceholderResolver placeholderResolver;

    /** Maps player UUID -> repeating cache-refresh task ID */
    private final Map<UUID, Integer> refreshTasks = new HashMap<>();

    public LeaveMessageCache(
            CorePlugin plugin,
            PluginConfig config,
            MessageFormatter messageFormatter,
            PlaceholderResolver placeholderResolver
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messageFormatter = messageFormatter;
        this.placeholderResolver = placeholderResolver;
    }

    /** Starts cache-refresh tasks for all currently online players. Called on reload. */
    public void initForAllPlayers() {
        refreshTasks.values().forEach(plugin::cancelTask);
        refreshTasks.clear();
        if (config.getLeaveCacheDuration() == 0) return;
        plugin.getAllPlayers().forEach(this::startFor);
    }

    /** Starts a repeating cache-refresh task for the given player. No-op if caching is disabled. */
    public void startFor(CorePlayer player) {
        if (config.getLeaveCacheDuration() == 0) return;
        int taskId = plugin.runTaskRepeatedly(
            () -> refresh(player),
            config.getLeaveCacheDuration()
        );
        refreshTasks.put(player.getUniqueId(), taskId);
    }

    /** Stops the cache-refresh task for the given player. */
    public void stopFor(CorePlayer player) {
        Integer taskId = refreshTasks.remove(player.getUniqueId());
        if (taskId != null) plugin.cancelTask(taskId);
    }

    /** Forces an immediate refresh of the cached leave message for the given player. */
    public void refresh(CorePlayer player) {
        plugin.getCoreLogger().debug("Refreshing cached leave message for " + player.getName());
        String template = messageFormatter.formatLeaveMessage(player);
        placeholderResolver.resolve(template, player, player::setCachedLeaveMessage);
    }
}
