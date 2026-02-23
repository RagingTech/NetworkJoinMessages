package xyz.earthcow.networkjoinmessages.common.modules;

import dev.dejvokep.boostedyaml.YamlDocument;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.discordwebhook.DiscordWebhook;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.events.NetworkJoinEvent;
import xyz.earthcow.networkjoinmessages.common.events.NetworkLeaveEvent;
import xyz.earthcow.networkjoinmessages.common.events.SwapServerEvent;
import xyz.earthcow.networkjoinmessages.common.util.PlaceholderResolver;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Handles Discord webhook execution for network join/leave/swap events.
 * Delegates webhook construction to {@link DiscordWebhookBuilder}
 * and placeholder resolution to {@link PlaceholderResolver}.
 */
public class DiscordIntegration {

    private final CorePlugin plugin;
    private final PlaceholderResolver placeholderResolver;
    private final DiscordWebhookBuilder webhookBuilder;
    private final YamlDocument discordConfig;

    private String webhookUrl;

    public DiscordIntegration(
            CorePlugin plugin,
            PlaceholderResolver placeholderResolver,
            DiscordWebhookBuilder webhookBuilder,
            YamlDocument discordConfig
    ) {
        this.plugin = plugin;
        this.placeholderResolver = placeholderResolver;
        this.webhookBuilder = webhookBuilder;
        this.discordConfig = discordConfig;
        loadConfig();
    }

    /** Reads webhook URL from config and registers/unregisters this listener accordingly. */
    public void loadConfig() {
        if (!discordConfig.getBoolean("Enabled")) {
            plugin.unregisterDiscordListener();
            return;
        }
        webhookUrl = discordConfig.getString("WebhookUrl");
        plugin.registerDiscordListener(this);
        plugin.getCoreLogger().info("Discord Integration is enabled!");
    }

    // --- Event handlers ---

    public void onSwapServer(SwapServerEvent event) {
        if (event.isSilenced()) return;
        CorePlayer player = event.player();
        DiscordWebhook webhook = webhookBuilder.buildSwapWebhook(webhookUrl, player, event.serverTo(), event.serverFrom());
        if (webhook != null) executeWebhook(webhook, player);
    }

    public void onNetworkJoin(NetworkJoinEvent event) {
        if (event.isSilenced()) return;
        CorePlayer player = event.player();
        String key = event.isFirstJoin() ? "Messages.FirstJoinNetwork" : "Messages.JoinNetwork";
        DiscordWebhook webhook = webhookBuilder.buildJoinWebhook(webhookUrl, key, player);
        if (webhook != null) executeWebhook(webhook, player);
    }

    public void onNetworkLeave(NetworkLeaveEvent event) {
        if (event.isSilenced()) return;
        CorePlayer player = event.player();
        DiscordWebhook webhook = webhookBuilder.buildLeaveWebhook(webhookUrl, player);
        if (webhook != null) executeWebhook(webhook, player);
    }

    // --- Webhook execution ---

    /**
     * Resolves any remaining placeholders in the webhook payload, then executes the webhook
     * asynchronously.
     */
    private void executeWebhook(DiscordWebhook webhook, CorePlayer parseTarget) {
        placeholderResolver.resolve(webhook.getJsonString(), parseTarget, formatted ->
            plugin.runTaskAsync(() -> {
                try {
                    webhook.execute(formatted);
                } catch (IOException e) {
                    plugin.getCoreLogger().severe("[DiscordIntegration] " + describeHttpError(e));
                    plugin.getCoreLogger().debug("Exception: " + e);
                    plugin.getCoreLogger().debug("Webhook payload: " + webhook.getJsonString());
                }
            })
        );
    }

    /** Produces a human-readable error description for a failed webhook HTTP request. */
    private String describeHttpError(IOException e) {
        if (e instanceof FileNotFoundException) {
            return "The webhook URL is not valid.";
        }
        String message = e.getMessage();
        if (message != null && message.contains("HTTP response code:")) {
            try {
                int code = Integer.parseInt(message.substring(message.indexOf(":") + 2, message.indexOf(":") + 5));
                return switch (code) {
                    case 400 -> "400 Bad Request — verify all URLs in the discord config are blank or valid.";
                    case 401 -> "401 Unauthorized — verify the webhook URL and Discord server status.";
                    case 403 -> "403 Forbidden — verify the webhook URL and Discord server status.";
                    case 404 -> "404 Not Found — verify the webhook URL and Discord server status.";
                    case 429 -> "429 Too Many Requests — this webhook is being rate-limited.";
                    case 500 -> "500 Internal Server Error — Discord services may be temporarily down.";
                    default  -> code + " Unexpected response code.";
                };
            } catch (IndexOutOfBoundsException | NumberFormatException ex) {
                plugin.getCoreLogger().debug("Could not parse HTTP response code: " + ex);
            }
        }
        return "Unknown error. Please file a bug report at https://github.com/RagingTech/NetworkJoinMessages/issues.";
    }
}
