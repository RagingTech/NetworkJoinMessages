package xyz.earthcow.networkjoinmessages.common.modules;

import dev.dejvokep.boostedyaml.YamlDocument;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.discordwebhook.DiscordWebhook;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;

import java.awt.*;
import java.util.Date;
import java.util.List;

/**
 * Builds a {@link DiscordWebhook} skeleton for a specific event type by reading discord.yml.
 *
 * <p>This class is a <em>pure structural mapper</em>: it copies config values verbatim into
 * the webhook fields with no placeholder substitution. All placeholder resolution — both the
 * player-count/server-name pre-processing and the final LuckPerms/PAPI pass — is handled
 * by the caller ({@link DiscordIntegration}) on the serialized JSON string. This means
 * substitution happens in one place, on one string, once.
 */
public final class DiscordWebhookBuilder {

    private final CorePlugin plugin;
    private final YamlDocument discordConfig;

    public DiscordWebhookBuilder(CorePlugin plugin, YamlDocument discordConfig) {
        this.plugin = plugin;
        this.discordConfig = discordConfig;
    }

    /**
     * Returns a webhook skeleton for a swap event, or {@code null} if disabled in config.
     */
    @Nullable
    public DiscordWebhook buildSwapWebhook(String webhookUrl) {
        return buildWebhook(webhookUrl, "Messages.SwapServer");
    }

    /**
     * Returns a webhook skeleton for a join or first-join event, or {@code null} if disabled.
     *
     * @param key either {@code "Messages.JoinNetwork"} or {@code "Messages.FirstJoinNetwork"}
     */
    @Nullable
    public DiscordWebhook buildJoinWebhook(String webhookUrl, String key) {
        return buildWebhook(webhookUrl, key);
    }

    /**
     * Returns a webhook skeleton for a leave event, or {@code null} if disabled in config.
     */
    @Nullable
    public DiscordWebhook buildLeaveWebhook(String webhookUrl) {
        return buildWebhook(webhookUrl, "Messages.LeaveNetwork");
    }

    // --- Internal builder ---

    /**
     * Constructs a webhook from the config section at {@code key}, copying all values verbatim.
     * Returns {@code null} when the event type is disabled.
     */
    @Nullable
    private DiscordWebhook buildWebhook(String webhookUrl, String key) {
        if (!discordConfig.getBoolean(key + ".Enabled")) return null;

        DiscordWebhook webhook = new DiscordWebhook(webhookUrl);

        if (discordConfig.getBoolean(key + ".CustomWebhook.Enabled")) {
            webhook.setUsername(discordConfig.getString(key + ".CustomWebhook.Name"));
            webhook.setAvatarUrl(discordConfig.getString(key + ".CustomWebhook.AvatarUrl"));
        }
        if (discordConfig.getBoolean(key + ".Embed.Enabled")) {
            DiscordWebhook.EmbedObject embed = buildEmbed(key + ".Embed");
            if (embed != null) webhook.addEmbed(embed);
        }
        String content = discordConfig.getString(key + ".Content");
        if (!content.isEmpty()) {
            webhook.setContent(content);
        }
        return webhook;
    }

    /**
     * Builds an embed from the config section at {@code key}, copying all values verbatim.
     */
    @Nullable
    private DiscordWebhook.EmbedObject buildEmbed(String key) {
        if (discordConfig.get(key) == null) return null;

        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject();

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
            String name = discordConfig.getString(key + ".Author.Name");
            if (!name.isEmpty()) {
                embed.setAuthor(
                    name,
                    discordConfig.getString(key + ".Author.Url"),
                    discordConfig.getString(key + ".Author.ImageUrl")
                );
            }
        }

        // Thumbnail
        String thumbnail = discordConfig.getString(key + ".ThumbnailUrl");
        if (!thumbnail.isEmpty()) embed.setThumbnail(thumbnail);

        // Title
        if (discordConfig.get(key + ".Title", null) != null) {
            String titleText = discordConfig.getString(key + ".Title.Text");
            if (!titleText.isEmpty()) {
                embed.setTitle(titleText);
                embed.setUrl(discordConfig.getString(key + ".Title.Url"));
            }
        }

        // Description
        String description = discordConfig.getString(key + ".Description");
        if (!description.isEmpty()) embed.setDescription(description);

        // Fields
        List<String> fields = discordConfig.getStringList(key + ".Fields");
        for (String field : fields) {
            if (field.contains(";")) {
                String[] parts = field.split(";");
                if (parts.length < 2) continue;
                boolean inline = parts.length < 3 || Boolean.parseBoolean(parts[2]);
                embed.addField(parts[0], parts[1], inline);
            } else {
                embed.addField("\u200e", "\u200e", Boolean.parseBoolean(field));
            }
        }

        // Image
        String image = discordConfig.getString(key + ".ImageUrl");
        if (!image.isEmpty()) embed.setImage(image);

        // Footer
        if (discordConfig.get(key + ".Footer", null) != null) {
            String footerText = discordConfig.getString(key + ".Footer.Text");
            if (!footerText.isEmpty()) {
                embed.setFooter(footerText, discordConfig.getString(key + ".Footer.IconUrl"));
            }
        }

        // Timestamp
        if (discordConfig.getBoolean(key + ".Timestamp")) {
            embed.setTimestamp(new Date().toInstant());
        }

        return embed;
    }
}
