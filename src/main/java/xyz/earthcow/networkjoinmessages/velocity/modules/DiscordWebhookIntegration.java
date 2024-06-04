package xyz.earthcow.networkjoinmessages.velocity.modules;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import xyz.earthcow.networkjoinmessages.velocity.events.NetworkJoinEvent;
import xyz.earthcow.networkjoinmessages.velocity.events.NetworkQuitEvent;
import xyz.earthcow.networkjoinmessages.velocity.events.SwapServerEvent;
import xyz.earthcow.networkjoinmessages.velocity.general.VelocityMain;
import xyz.earthcow.networkjoinmessages.velocity.util.DiscordWebhook;
import xyz.earthcow.networkjoinmessages.velocity.util.MessageHandler;

public class DiscordWebhookIntegration {

    private CommentedConfigurationNode discordConfigRootNode;
    private final VelocityMain plugin = VelocityMain.getInstance();
    private String webhookUrl = "";

    public DiscordWebhookIntegration() {
        loadConfig();
    }

    public void loadConfig() {
        // Create the data folder if non-existent
        File dataFolder = plugin.getDataDirectory().toFile();
        if (!dataFolder.exists()) {
            if (!dataFolder.mkdir()) {
                plugin
                    .getLogger()
                    .error(
                        "Failed to create data folder!! Check permissions and storage."
                    );
                return;
            }
        }
        // Create the discord.yml config file if non-existent
        File file = plugin.getDataDirectory().resolve("discord.yml").toFile();

        if (!file.exists()) {
            try (
                InputStream in = getClass()
                    .getClassLoader()
                    .getResourceAsStream("discord.yml")
            ) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
            .path(plugin.getDataDirectory().resolve("discord.yml"))
            .build();

        try {
            discordConfigRootNode = loader.load();
        } catch (IOException e) {
            System.err.println(
                "An error occurred while loading the configuration: " +
                e.getMessage()
            );
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }
        }

        // Register/unregister this listener if enabled/disabled
        if (discordConfigRootNode.node("Enabled").getBoolean(false)) {
            // Set the main webhook url
            webhookUrl = discordConfigRootNode.node("WebhookUrl").getString("");
            plugin.getProxy().getEventManager().register(plugin, this);
            plugin.getLogger().info("Discord Integration is enabled!");
        } else {
            plugin
                .getProxy()
                .getEventManager()
                .unregisterListener(plugin, this);
        }
    }

    private void executeWebhook(DiscordWebhook webhook) {
        plugin
            .getProxy()
            .getScheduler()
            .buildTask(plugin, () -> {
                try {
                    webhook.execute();
                } catch (Exception e) {
                    plugin
                        .getLogger()
                        .warn(
                            "[DiscordIntegration] There is a problem with your configuration! Verify the webhook url and all config values. Make sure anything that is supposed to be a url is either blank or a valid url."
                        );
                }
            })
            .schedule();
    }

    // Event handlers
    @Subscribe
    public void onSwapServer(SwapServerEvent event) {
        // Ignore if the event is silenced
        if (event.isSilenced()) return;
        // Ignore if the message is disabled
        if (
            !discordConfigRootNode
                .node("Messages", "SwapServer", "Enabled")
                .getBoolean()
        ) return;
        // Construct the webhook
        DiscordWebhook discordWebhook = new DiscordWebhook(webhookUrl);
        // Define variables
        Player player = event.getPlayer();
        String toServer = event.getServerTo();
        String fromServer = event.getServerFrom();
        // Check if custom webhook is enabled
        if (
            discordConfigRootNode
                .node("Messages", "SwapServer", "CustomWebhook", "Enabled")
                .getBoolean()
        ) {
            discordWebhook.setUsername(
                getSwapConfigValue(
                    "Messages.SwapServer.CustomWebhook.Name",
                    player,
                    toServer,
                    fromServer
                )
            );
            discordWebhook.setAvatarUrl(
                getSwapConfigValue(
                    "Messages.SwapServer.CustomWebhook.AvatarUrl",
                    player,
                    toServer,
                    fromServer
                )
            );
        }
        if (
            discordConfigRootNode
                .node("Messages", "SwapServer", "Embed", "Enabled")
                .getBoolean()
        ) {
            try {
                discordWebhook.addEmbed(
                    getEmbedFromConfigSwap(
                        "Messages.SwapServer.Embed",
                        player,
                        toServer,
                        fromServer
                    )
                );
            } catch (SerializationException serializationException) {
                serializationException.printStackTrace();
            }
        }
        if (
            !discordConfigRootNode
                .node("Messages", "SwapServer", "Content")
                .getString("")
                .isEmpty()
        ) {
            discordWebhook.setContent(
                getSwapConfigValue(
                    "Messages.SwapServer.Content",
                    player,
                    toServer,
                    fromServer
                )
            );
        }
        executeWebhook(discordWebhook);
    }

    @Subscribe
    public void onNetworkJoin(NetworkJoinEvent event) {
        // Ignore if the event is silenced
        if (event.isSilenced()) return;
        // Ignore if the message is disabled
        if (
            !discordConfigRootNode
                .node("Messages", "JoinNetwork", "Enabled")
                .getBoolean()
        ) return;
        // Construct the webhook
        DiscordWebhook discordWebhook = new DiscordWebhook(webhookUrl);
        // Define variables
        Player player = event.getPlayer();
        // Check if custom webhook is enabled
        if (
            discordConfigRootNode
                .node("Messages", "JoinNetwork", "CustomWebhook", "Enabled")
                .getBoolean()
        ) {
            discordWebhook.setUsername(
                getJoinLeaveConfigValue(
                    "Messages.JoinNetwork.CustomWebhook.Name",
                    player,
                    false
                )
            );
            discordWebhook.setAvatarUrl(
                getJoinLeaveConfigValue(
                    "Messages.JoinNetwork.CustomWebhook.AvatarUrl",
                    player,
                    false
                )
            );
        }
        if (
            discordConfigRootNode
                .node("Messages", "JoinNetwork", "Embed", "Enabled")
                .getBoolean()
        ) {
            try {
                discordWebhook.addEmbed(
                    getEmbedFromConfigJoinLeave(
                        "Messages.JoinNetwork.Embed",
                        player,
                        false
                    )
                );
            } catch (SerializationException serializationException) {
                serializationException.printStackTrace();
            }
        }
        if (
            !discordConfigRootNode
                .node("Messages", "JoinNetwork", "Content")
                .getString("")
                .isEmpty()
        ) {
            discordWebhook.setContent(
                getJoinLeaveConfigValue(
                    "Messages.JoinNetwork.Content",
                    player,
                    false
                )
            );
        }
        executeWebhook(discordWebhook);
    }

    @Subscribe
    public void onNetworkQuit(NetworkQuitEvent event) {
        // Ignore if the event is silenced
        if (event.isSilenced()) return;
        // Ignore if the message is disabled
        if (
            !discordConfigRootNode
                .node("Messages", "LeaveNetwork", "Enabled")
                .getBoolean()
        ) return;
        // Construct the webhook
        DiscordWebhook discordWebhook = new DiscordWebhook(webhookUrl);
        // Define variables
        Player player = event.getPlayer();
        // Check if custom webhook is enabled
        if (
            discordConfigRootNode
                .node("Messages", "LeaveNetwork", "CustomWebhook", "Enabled")
                .getBoolean()
        ) {
            discordWebhook.setUsername(
                getJoinLeaveConfigValue(
                    "Messages.LeaveNetwork.CustomWebhook.Name",
                    player,
                    true
                )
            );
            discordWebhook.setAvatarUrl(
                getJoinLeaveConfigValue(
                    "Messages.LeaveNetwork.CustomWebhook.AvatarUrl",
                    player,
                    true
                )
            );
        }
        if (
            discordConfigRootNode
                .node("Messages", "LeaveNetwork", "Embed", "Enabled")
                .getBoolean()
        ) {
            try {
                discordWebhook.addEmbed(
                    getEmbedFromConfigJoinLeave(
                        "Messages.LeaveNetwork.Embed",
                        player,
                        true
                    )
                );
            } catch (SerializationException serializationException) {
                serializationException.printStackTrace();
            }
        }
        if (
            !discordConfigRootNode
                .node("Messages", "LeaveNetwork", "Content")
                .getString("")
                .isEmpty()
        ) {
            discordWebhook.setContent(
                getJoinLeaveConfigValue(
                    "Messages.LeaveNetwork.Content",
                    player,
                    true
                )
            );
        }
        executeWebhook(discordWebhook);
    }

    private String replacePlaceholdersSwap(
        String txt,
        Player player,
        String toServer,
        String fromServer
    ) {
        String displayTo = LegacyComponentSerializer.legacySection()
            .serialize(
                LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(
                        MessageHandler.getInstance().getServerName(toServer)
                    )
            );
        String displayFrom = LegacyComponentSerializer.legacySection()
            .serialize(
                LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(
                        MessageHandler.getInstance().getServerName(fromServer)
                    )
            );
        return MessageHandler.getInstance()
            .formatMessage(txt, player)
            .replace("%embedavatarurl%", getEmbedAvatarUrl(player))
            .replace("%to%", displayTo)
            .replace(
                "%to_clean%",
                PlainTextComponentSerializer.plainText()
                    .serialize(
                        LegacyComponentSerializer.legacyAmpersand()
                            .deserialize(displayTo)
                    )
            )
            .replace("%from%", displayFrom)
            .replace(
                "%from_clean%",
                PlainTextComponentSerializer.plainText()
                    .serialize(
                        LegacyComponentSerializer.legacyAmpersand()
                            .deserialize(displayFrom)
                    )
            )
            .replace(
                "%playercount_from%",
                MessageHandler.getInstance()
                    .getServerPlayerCount(fromServer, true, player)
            )
            .replace(
                "%playercount_to%",
                MessageHandler.getInstance()
                    .getServerPlayerCount(toServer, false, player)
            )
            .replace(
                "%playercount_network%",
                MessageHandler.getInstance()
                    .getNetworkPlayerCount(player, false)
            );
    }

    private String getSwapConfigValue(
        String key,
        Player player,
        String toServer,
        String fromServer
    ) {
        return replacePlaceholdersSwap(
            discordConfigRootNode.node(List.of(key.split("\\."))).getString(""),
            player,
            toServer,
            fromServer
        );
    }

    private String replacePlaceholdersJoinLeave(
        String txt,
        Player player,
        boolean leaving
    ) {
        return MessageHandler.getInstance()
            .formatMessage(txt, player)
            .replace("%embedavatarurl%", getEmbedAvatarUrl(player))
            .replace(
                "%playercount_server%",
                MessageHandler.getInstance()
                    .getServerPlayerCount(
                        Optional.of(
                            player.getCurrentServer().get().getServer()
                        ),
                        leaving,
                        player
                    )
            )
            .replace(
                "%playercount_network%",
                MessageHandler.getInstance()
                    .getNetworkPlayerCount(player, leaving)
            );
    }

    private String getJoinLeaveConfigValue(
        String key,
        Player player,
        boolean leaving
    ) {
        return replacePlaceholdersJoinLeave(
            discordConfigRootNode.node(List.of(key.split("\\."))).getString(""),
            player,
            leaving
        );
    }

    private String getEmbedAvatarUrl(Player player) {
        return discordConfigRootNode
            .node("EmbedAvatarUrl")
            .getString("")
            .replace("%uuid%", player.getUniqueId().toString())
            .replace("%player%", player.getUsername());
    }

    @Nullable
    private DiscordWebhook.EmbedObject getEmbedFromConfigSwap(
        @NotNull String key,
        @NotNull Player player,
        @NotNull String toServer,
        @NotNull String fromServer
    ) throws SerializationException {
        ConfigurationNode keyNode = discordConfigRootNode.node(
            List.of(key.split("\\."))
        );

        if (keyNode.isNull()) {
            return null;
        }

        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject();

        String hexColor = keyNode.node("Color").getString("#000000");

        if (hexColor.isEmpty()) {
            VelocityMain.getInstance()
                .getLogger()
                .warn("A color was missing from embed config!");
            hexColor = "#000000";
        }

        hexColor = hexColor.trim();
        if (!hexColor.startsWith("#")) {
            hexColor = "#" + hexColor;
        }
        if (hexColor.length() != 7) {
            VelocityMain.getInstance()
                .getLogger()
                .warn("An invalid color: " + hexColor + " was provided!");
            hexColor = "#000000";
        }

        embed.setColor(Color.decode(hexColor));

        ConfigurationNode authorNode = keyNode.node("Author");
        if (!authorNode.isNull()) {
            String authorName = getSwapConfigValue(
                key + ".Author.Name",
                player,
                toServer,
                fromServer
            );
            if (!authorName.isEmpty()) {
                embed.setAuthor(
                    authorName,
                    getSwapConfigValue(
                        key + ".Author.Url",
                        player,
                        toServer,
                        fromServer
                    ),
                    getSwapConfigValue(
                        key + ".Author.ImageUrl",
                        player,
                        toServer,
                        fromServer
                    )
                );
            }
        }

        String thumbnailUrl = keyNode.node("ThumbnailUrl").getString();
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            embed.setThumbnail(thumbnailUrl);
        }

        ConfigurationNode titleNode = keyNode.node("Title");
        if (!titleNode.isNull()) {
            String titleText = titleNode.node("Text").getString();
            if (titleText != null && !titleText.isEmpty()) {
                embed.setTitle(
                    getSwapConfigValue(
                        key + ".Title.Text",
                        player,
                        toServer,
                        fromServer
                    )
                );
                embed.setUrl(
                    getSwapConfigValue(
                        key + ".Title.Url",
                        player,
                        toServer,
                        fromServer
                    )
                );
            }
        }

        String description = keyNode.node("Description").getString();
        if (description != null && !description.isEmpty()) {
            embed.setDescription(
                getSwapConfigValue(
                    key + ".Description",
                    player,
                    toServer,
                    fromServer
                )
            );
        }

        List<String> fields = keyNode.node("Fields").getList(String.class);
        if (fields != null && !fields.isEmpty()) {
            for (String field : fields) {
                if (field.contains(";")) {
                    String[] parts = field.split(";");
                    if (parts.length < 2) {
                        continue;
                    }

                    boolean inline =
                        parts.length < 3 || Boolean.parseBoolean(parts[2]);
                    embed.addField(
                        replacePlaceholdersSwap(
                            parts[0],
                            player,
                            toServer,
                            fromServer
                        ),
                        replacePlaceholdersSwap(
                            parts[1],
                            player,
                            toServer,
                            fromServer
                        ),
                        inline
                    );
                } else {
                    boolean inline = Boolean.parseBoolean(field);
                    embed.addField("\u200e", "\u200e", inline);
                }
            }
        }

        String imageUrl = keyNode.node("ImageUrl").getString();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            embed.setImage(
                getSwapConfigValue(
                    key + ".ImageUrl",
                    player,
                    toServer,
                    fromServer
                )
            );
        }

        ConfigurationNode footerNode = keyNode.node("Footer");
        if (!footerNode.isNull()) {
            String footerText = footerNode.node("Text").getString();
            if (footerText != null && !footerText.isEmpty()) {
                embed.setFooter(
                    getSwapConfigValue(
                        key + ".Footer.Text",
                        player,
                        toServer,
                        fromServer
                    ),
                    getSwapConfigValue(
                        key + ".Footer.IconUrl",
                        player,
                        toServer,
                        fromServer
                    )
                );
            }
        }

        boolean timestamp = keyNode.node("Timestamp").getBoolean(false);
        if (timestamp) {
            embed.setTimestamp((new Date()).toInstant());
        }

        return embed;
    }

    @Nullable
    private DiscordWebhook.EmbedObject getEmbedFromConfigJoinLeave(
        @NotNull String key,
        @NotNull Player player,
        boolean leaving
    ) throws SerializationException {
        ConfigurationNode keyNode = discordConfigRootNode.node(
            List.of(key.split("\\."))
        );

        if (keyNode.isNull()) {
            return null;
        }

        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject();

        String hexColor = keyNode.node("Color").getString("#000000");

        if (hexColor.isEmpty()) {
            VelocityMain.getInstance()
                .getLogger()
                .warn("A color was missing from embed config!");
            hexColor = "#000000";
        }

        hexColor = hexColor.trim();
        if (!hexColor.startsWith("#")) {
            hexColor = "#" + hexColor;
        }
        if (hexColor.length() != 7) {
            VelocityMain.getInstance()
                .getLogger()
                .warn("An invalid color: " + hexColor + " was provided!");
            hexColor = "#000000";
        }

        embed.setColor(Color.decode(hexColor));

        ConfigurationNode authorNode = keyNode.node("Author");
        if (!authorNode.isNull()) {
            String authorName = getJoinLeaveConfigValue(
                key + ".Author.Name",
                player,
                leaving
            );
            if (!authorName.isEmpty()) {
                embed.setAuthor(
                    authorName,
                    getJoinLeaveConfigValue(
                        key + ".Author.Url",
                        player,
                        leaving
                    ),
                    getJoinLeaveConfigValue(
                        key + ".Author.ImageUrl",
                        player,
                        leaving
                    )
                );
            }
        }

        String thumbnailUrl = keyNode.node("ThumbnailUrl").getString();
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            embed.setThumbnail(
                getJoinLeaveConfigValue(key + ".ThumbnailUrl", player, leaving)
            );
        }

        ConfigurationNode titleNode = keyNode.node("Title");
        if (!titleNode.isNull()) {
            String titleText = titleNode.node("Text").getString();
            if (titleText != null && !titleText.isEmpty()) {
                embed.setTitle(
                    getJoinLeaveConfigValue(
                        key + ".Title.Text",
                        player,
                        leaving
                    )
                );
                embed.setUrl(
                    getJoinLeaveConfigValue(key + ".Title.Url", player, leaving)
                );
            }
        }

        String description = keyNode.node("Description").getString();
        if (description != null && !description.isEmpty()) {
            embed.setDescription(
                getJoinLeaveConfigValue(key + ".Description", player, leaving)
            );
        }

        List<String> fields = keyNode.node("Fields").getList(String.class);
        if (fields != null && !fields.isEmpty()) {
            for (String field : fields) {
                if (field.contains(";")) {
                    String[] parts = field.split(";");
                    if (parts.length < 2) {
                        continue;
                    }

                    boolean inline =
                        parts.length < 3 || Boolean.parseBoolean(parts[2]);
                    embed.addField(
                        replacePlaceholdersJoinLeave(parts[0], player, leaving),
                        replacePlaceholdersJoinLeave(parts[1], player, leaving),
                        inline
                    );
                } else {
                    boolean inline = Boolean.parseBoolean(field);
                    embed.addField("\u200e", "\u200e", inline);
                }
            }
        }

        String imageUrl = keyNode.node("ImageUrl").getString();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            embed.setImage(
                getJoinLeaveConfigValue(key + ".ImageUrl", player, leaving)
            );
        }

        ConfigurationNode footerNode = keyNode.node("Footer");
        if (!footerNode.isNull()) {
            String footerText = footerNode.node("Text").getString();
            if (footerText != null && !footerText.isEmpty()) {
                embed.setFooter(
                    getJoinLeaveConfigValue(
                        key + ".Footer.Text",
                        player,
                        leaving
                    ),
                    getJoinLeaveConfigValue(
                        key + ".Footer.IconUrl",
                        player,
                        leaving
                    )
                );
            }
        }

        boolean timestamp = keyNode.node("Timestamp").getBoolean(false);
        if (timestamp) {
            embed.setTimestamp((new Date()).toInstant());
        }

        return embed;
    }
}
