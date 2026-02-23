package xyz.earthcow.networkjoinmessages.common.modules;

import dev.dejvokep.boostedyaml.YamlDocument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.discordwebhook.DiscordWebhook;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.broadcast.MessageFormatter;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.util.Formatter;

import java.awt.*;
import java.util.Date;
import java.util.List;

/**
 * Builds a configured {@link DiscordWebhook} for a specific event type by reading
 * the discord.yml config and applying the appropriate placeholder substitutions.
 *
 * <p>Knows about config structure and placeholder replacement, but has no knowledge
 * of how the resulting webhook is executed or dispatched.
 */
public final class DiscordWebhookBuilder {

    private final CorePlugin plugin;
    private final PluginConfig pluginConfig;
    private final MessageFormatter messageFormatter;
    private final YamlDocument discordConfig;

    public DiscordWebhookBuilder(
            CorePlugin plugin,
            PluginConfig pluginConfig,
            MessageFormatter messageFormatter,
            YamlDocument discordConfig
    ) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
        this.messageFormatter = messageFormatter;
        this.discordConfig = discordConfig;
    }

    /**
     * Builds a fully configured webhook for a swap-server event, or null if the event
     * is disabled in config.
     */
    @Nullable
    public DiscordWebhook buildSwapWebhook(String webhookUrl, CorePlayer player, String toServer, String fromServer) {
        if (!discordConfig.getBoolean("Messages.SwapServer.Enabled")) return null;

        DiscordWebhook webhook = new DiscordWebhook(webhookUrl);

        if (discordConfig.getBoolean("Messages.SwapServer.CustomWebhook.Enabled")) {
            webhook.setUsername(swapValue("Messages.SwapServer.CustomWebhook.Name", player, toServer, fromServer));
            webhook.setAvatarUrl(swapValue("Messages.SwapServer.CustomWebhook.AvatarUrl", player, toServer, fromServer));
        }
        if (discordConfig.getBoolean("Messages.SwapServer.Embed.Enabled")) {
            DiscordWebhook.EmbedObject embed = buildEmbed("Messages.SwapServer.Embed", player, toServer, fromServer);
            if (embed != null) webhook.addEmbed(embed);
        }
        String content = discordConfig.getString("Messages.SwapServer.Content");
        if (!content.isEmpty()) {
            webhook.setContent(swapValue("Messages.SwapServer.Content", player, toServer, fromServer));
        }
        return webhook;
    }

    /**
     * Builds a fully configured webhook for a join or first-join event, or null if disabled.
     *
     * @param key either "Messages.JoinNetwork" or "Messages.FirstJoinNetwork"
     */
    @Nullable
    public DiscordWebhook buildJoinWebhook(String webhookUrl, String key, CorePlayer player) {
        if (!discordConfig.getBoolean(key + ".Enabled")) return null;

        DiscordWebhook webhook = new DiscordWebhook(webhookUrl);

        if (discordConfig.getBoolean(key + ".CustomWebhook.Enabled")) {
            webhook.setUsername(joinLeaveValue(key + ".CustomWebhook.Name", player, false));
            webhook.setAvatarUrl(joinLeaveValue(key + ".CustomWebhook.AvatarUrl", player, false));
        }
        if (discordConfig.getBoolean(key + ".Embed.Enabled")) {
            DiscordWebhook.EmbedObject embed = buildEmbedJoinLeave(key + ".Embed", player, false);
            if (embed != null) webhook.addEmbed(embed);
        }
        String content = discordConfig.getString(key + ".Content");
        if (!content.isEmpty()) {
            webhook.setContent(joinLeaveValue(key + ".Content", player, false));
        }
        return webhook;
    }

    /**
     * Builds a fully configured webhook for a leave event, or null if disabled.
     */
    @Nullable
    public DiscordWebhook buildLeaveWebhook(String webhookUrl, CorePlayer player) {
        if (!discordConfig.getBoolean("Messages.LeaveNetwork.Enabled")) return null;

        DiscordWebhook webhook = new DiscordWebhook(webhookUrl);

        if (discordConfig.getBoolean("Messages.LeaveNetwork.CustomWebhook.Enabled")) {
            webhook.setUsername(joinLeaveValue("Messages.LeaveNetwork.CustomWebhook.Name", player, true));
            webhook.setAvatarUrl(joinLeaveValue("Messages.LeaveNetwork.CustomWebhook.AvatarUrl", player, true));
        }
        if (discordConfig.getBoolean("Messages.LeaveNetwork.Embed.Enabled")) {
            DiscordWebhook.EmbedObject embed = buildEmbedJoinLeave("Messages.LeaveNetwork.Embed", player, true);
            if (embed != null) webhook.addEmbed(embed);
        }
        String content = discordConfig.getString("Messages.LeaveNetwork.Content");
        if (!content.isEmpty()) {
            webhook.setContent(joinLeaveValue("Messages.LeaveNetwork.Content", player, true));
        }
        return webhook;
    }

    // --- Embed construction ---

    @Nullable
    private DiscordWebhook.EmbedObject buildEmbed(String key, CorePlayer player, String toServer, String fromServer) {
        if (discordConfig.get(key) == null) return null;
        return populateEmbed(new DiscordWebhook.EmbedObject(), key,
            (txt) -> applySwapPlaceholders(txt, player, toServer, fromServer));
    }

    @Nullable
    private DiscordWebhook.EmbedObject buildEmbedJoinLeave(String key, CorePlayer player, boolean leaving) {
        if (discordConfig.get(key) == null) return null;
        return populateEmbed(new DiscordWebhook.EmbedObject(), key,
            (txt) -> applyJoinLeavePlaceholders(txt, player, leaving));
    }

    /**
     * Populates a {@link DiscordWebhook.EmbedObject} from config, applying text substitutions
     * via the provided {@code replacer} function.
     */
    private DiscordWebhook.EmbedObject populateEmbed(
            DiscordWebhook.EmbedObject embed,
            String key,
            java.util.function.UnaryOperator<String> replacer
    ) {
        // Color
        String hexColor = discordConfig.getString(key + ".Color").trim();
        if (hexColor.isEmpty()) {
            plugin.getCoreLogger().warn("Missing embed color in config at " + key + ".Color — defaulting to black.");
            hexColor = "#000000";
        }
        if (!hexColor.startsWith("#")) hexColor = "#" + hexColor;
        if (hexColor.length() != 7) {
            plugin.getCoreLogger().warn("Invalid embed color '" + hexColor + "' at " + key + ".Color — defaulting to black.");
            hexColor = "#000000";
        }
        embed.setColor(Color.decode(hexColor));

        // Author
        if (discordConfig.get(key + ".Author", null) != null) {
            String name = replacer.apply(discordConfig.getString(key + ".Author.Name"));
            if (!name.isEmpty()) {
                embed.setAuthor(
                    name,
                    replacer.apply(discordConfig.getString(key + ".Author.Url")),
                    replacer.apply(discordConfig.getString(key + ".Author.ImageUrl"))
                );
            }
        }

        // Thumbnail
        String thumbnail = discordConfig.getString(key + ".ThumbnailUrl");
        if (!thumbnail.isEmpty()) embed.setThumbnail(thumbnail);

        // Title
        if (discordConfig.get(key + ".Title", null) != null) {
            String titleText = replacer.apply(discordConfig.getString(key + ".Title.Text"));
            if (!titleText.isEmpty()) {
                embed.setTitle(titleText);
                embed.setUrl(replacer.apply(discordConfig.getString(key + ".Title.Url")));
            }
        }

        // Description
        String description = discordConfig.getString(key + ".Description");
        if (!description.isEmpty()) embed.setDescription(replacer.apply(description));

        // Fields
        List<String> fields = discordConfig.getStringList(key + ".Fields");
        for (String field : fields) {
            if (field.contains(";")) {
                String[] parts = field.split(";");
                if (parts.length < 2) continue;
                boolean inline = parts.length < 3 || Boolean.parseBoolean(parts[2]);
                embed.addField(replacer.apply(parts[0]), replacer.apply(parts[1]), inline);
            } else {
                embed.addField("\u200e", "\u200e", Boolean.parseBoolean(field));
            }
        }

        // Image
        String image = discordConfig.getString(key + ".ImageUrl");
        if (!image.isEmpty()) embed.setImage(replacer.apply(image));

        // Footer
        if (discordConfig.get(key + ".Footer", null) != null) {
            String footerText = replacer.apply(discordConfig.getString(key + ".Footer.Text"));
            if (!footerText.isEmpty()) {
                embed.setFooter(footerText, replacer.apply(discordConfig.getString(key + ".Footer.IconUrl")));
            }
        }

        // Timestamp
        if (discordConfig.getBoolean(key + ".Timestamp")) {
            embed.setTimestamp(new Date().toInstant());
        }

        return embed;
    }

    // --- Placeholder replacement ---

    private String applySwapPlaceholders(String txt, CorePlayer player, String toServer, String fromServer) {
        String displayTo   = pluginConfig.getServerDisplayName(toServer);
        String displayFrom = pluginConfig.getServerDisplayName(fromServer);

        if (txt.contains("%playercount_from%")) {
            txt = txt.replace("%playercount_from%", messageFormatter.getServerPlayerCount(fromServer, true, player));
        }
        if (txt.contains("%playercount_to%")) {
            txt = txt.replace("%playercount_to%", messageFormatter.getServerPlayerCount(toServer, false, player));
        }
        if (txt.contains("%playercount_network%")) {
            txt = txt.replace("%playercount_network%", messageFormatter.getNetworkPlayerCount(player, false));
        }
        return txt
            .replace("%embedavatarurl%", getAvatarUrl(player))
            .replace("%to%",         displayTo)
            .replace("%to_clean%",   Formatter.sanitize(displayTo))
            .replace("%from%",       displayFrom)
            .replace("%from_clean%", Formatter.sanitize(displayFrom));
    }

    private String applyJoinLeavePlaceholders(String txt, CorePlayer player, boolean leaving) {
        if (txt.contains("%playercount_server%")) {
            txt = txt.replace("%playercount_server%",
                messageFormatter.getServerPlayerCount(player.getCurrentServer(), leaving, player));
        }
        if (txt.contains("%playercount_network%")) {
            txt = txt.replace("%playercount_network%", messageFormatter.getNetworkPlayerCount(player, leaving));
        }
        return txt.replace("%embedavatarurl%", getAvatarUrl(player));
    }

    private String swapValue(String key, CorePlayer player, String toServer, String fromServer) {
        return applySwapPlaceholders(discordConfig.getString(key), player, toServer, fromServer);
    }

    private String joinLeaveValue(String key, CorePlayer player, boolean leaving) {
        return applyJoinLeavePlaceholders(discordConfig.getString(key), player, leaving);
    }

    private String getAvatarUrl(CorePlayer player) {
        return discordConfig.getString("EmbedAvatarUrl")
            .replace("%uuid%", player.getUniqueId().toString())
            .replace("%player%", player.getName());
    }
}
