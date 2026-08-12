package xyz.earthcow.networkjoinmessages.common.player;

import dev.dejvokep.boostedyaml.YamlDocument;
import xyz.earthcow.discordwebhook.DiscordWebhook;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.broadcast.MessageFormatter;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.modules.DiscordWebhookBuilder;
import xyz.earthcow.networkjoinmessages.common.util.PlaceholderResolver;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pre-computes and caches the formatted leave message for each online player, both for the
 * in-game leave message and the Discord leave webhook payload.
 *
 * <p>Because a player's leave message cannot be computed after disconnect (no live
 * placeholder data), it is formatted periodically while the player is connected and
 * stored on the player object, ready to be sent immediately on disconnect. Both caches
 * are refreshed together on the same timer so they can never fall out of sync with
 * each other.
 */
public final class LeaveMessageCache {

    private final CorePlugin plugin;
    private final PluginConfig config;
    private final MessageFormatter messageFormatter;
    private final PlaceholderResolver placeholderResolver;
    private final DiscordWebhookBuilder discordWebhookBuilder;
    private final YamlDocument discordConfig;

    /** Maps player UUID -> repeating cache-refresh task ID */
    private final Map<UUID, Integer> refreshTasks = new ConcurrentHashMap<>();

    public LeaveMessageCache(
            CorePlugin plugin,
            PluginConfig config,
            MessageFormatter messageFormatter,
            PlaceholderResolver placeholderResolver,
            DiscordWebhookBuilder discordWebhookBuilder,
            YamlDocument discordConfig
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messageFormatter = messageFormatter;
        this.placeholderResolver = placeholderResolver;
        this.discordWebhookBuilder = discordWebhookBuilder;
        this.discordConfig = discordConfig;
    }

    /** Starts cache-refresh tasks for all currently online players. Called on reload. */
    public void initForAllPlayers() {
        refreshTasks.values().forEach(plugin::cancelTask);
        refreshTasks.clear();
        plugin.getAllPlayers().forEach(player -> {
            refresh(player);          // immediate refresh so the cache is current right away
            startFor(player);         // then schedule the repeating background refresh
        });
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
        String template = messageFormatter.formatLeaveMessage(player);
        placeholderResolver.resolve(template, player, player::setCachedLeaveMessage);
        refreshDiscordPayload(player);
    }

    private void refreshDiscordPayload(CorePlayer player) {
        if (!discordConfig.getBoolean("Enabled")) {
            player.setCachedDiscordLeavePayload(null);
            return;
        }
        String webhookUrl = discordConfig.getString("WebhookUrl");
        DiscordWebhook webhook = discordWebhookBuilder.buildLeaveWebhook(webhookUrl);
        if (webhook == null) {
            player.setCachedDiscordLeavePayload(null);
            return;
        }
        String avatarUrl = discordWebhookBuilder.resolveAvatarUrl(player);
        String preparedJson = messageFormatter.prepareDiscordJoinLeaveTemplate(
            webhook.getJsonString(), player, true, avatarUrl);
        placeholderResolver.resolve(preparedJson, player, player::setCachedDiscordLeavePayload);
    }
}
