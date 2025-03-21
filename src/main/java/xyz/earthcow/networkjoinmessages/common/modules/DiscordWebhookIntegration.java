package xyz.earthcow.networkjoinmessages.common.modules;

import dev.dejvokep.boostedyaml.YamlDocument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.discordwebhook.DiscordWebhook;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.events.NetworkJoinEvent;
import xyz.earthcow.networkjoinmessages.common.events.NetworkQuitEvent;
import xyz.earthcow.networkjoinmessages.common.events.SwapServerEvent;
import xyz.earthcow.networkjoinmessages.common.general.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;
import xyz.earthcow.networkjoinmessages.common.util.MessageHandler;

import java.awt.*;
import java.util.Date;
import java.util.List;

public class DiscordWebhookIntegration {

    private final CorePlugin corePlugin = NetworkJoinMessagesCore.getInstance().getPlugin();

    private YamlDocument discordConfig;
    private String webhookUrl;
    private boolean enabled;

    public DiscordWebhookIntegration() {
        loadVariables();
    }

    public void loadVariables(){
        discordConfig = ConfigManager.getDiscordConfig();
        enabled = discordConfig.getBoolean("Enabled");
        if (!enabled) {
            return;
        }
        // Set the main webhook url
        webhookUrl = discordConfig.getString("WebhookUrl");

        corePlugin.getCoreLogger().info("Discord Integration is enabled!");
    }

    private void executeWebhook(DiscordWebhook webhook) {
        corePlugin.runTaskAsync(() -> {
                try {
                    webhook.execute();
                } catch (Exception e) {
                    corePlugin
                        .getCoreLogger()
                        .warn(
                            "[DiscordIntegration] There is a problem with your configuration! Verify the webhook url and all config values. Make sure anything that is supposed to be a url is either blank or a valid url."
                        );
                }
            });
    }

    // Event handlers
    public void onSwapServer(SwapServerEvent event) {
        if (!enabled) {
            return;
        }
        // Ignore if the event is silenced
        if (event.isSilenced()) return;
        // Ignore if the message is disabled
        if (!discordConfig.getBoolean("Messages.SwapServer.Enabled")) return;
        // Construct the webhook
        DiscordWebhook discordWebhook = new DiscordWebhook(webhookUrl);
        // Define variables
        CorePlayer player = event.getPlayer();
        String toServer = event.getServerTo();
        String fromServer = event.getServerFrom();
        // Check if custom webhook is enabled
        if (
                discordConfig.getBoolean(
                        "Messages.SwapServer.CustomWebhook.Enabled"
                )
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
        if (discordConfig.getBoolean("Messages.SwapServer.Embed.Enabled")) {
            discordWebhook.addEmbed(
                    getEmbedFromConfigSwap(
                            "Messages.SwapServer.Embed",
                            player,
                            toServer,
                            fromServer
                    )
            );
        }
        if (!discordConfig.getString("Messages.SwapServer.Content").isEmpty()) {
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

    public void onNetworkJoin(NetworkJoinEvent event) {
        if (!enabled) {
            return;
        }
        // Ignore if the event is silenced
        if (event.isSilenced()) return;
        // Ignore if the message is disabled
        if (!discordConfig.getBoolean("Messages.JoinNetwork.Enabled")) return;
        // Construct the webhook
        DiscordWebhook discordWebhook = new DiscordWebhook(webhookUrl);
        // Define variables
        CorePlayer player = event.getPlayer();
        // Check if custom webhook is enabled
        if (
                discordConfig.getBoolean(
                        "Messages.JoinNetwork.CustomWebhook.Enabled"
                )
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
        if (discordConfig.getBoolean("Messages.JoinNetwork.Embed.Enabled")) {
            discordWebhook.addEmbed(
                    getEmbedFromConfigJoinLeave(
                            "Messages.JoinNetwork.Embed",
                            player,
                            false
                    )
            );
        }
        if (
                !discordConfig.getString("Messages.JoinNetwork.Content").isEmpty()
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

    public void onNetworkQuit(NetworkQuitEvent event) {
        if (!enabled) {
            return;
        }
        // Ignore if the event is silenced
        if (event.isSilenced()) return;
        // Ignore if the message is disabled
        if (!discordConfig.getBoolean("Messages.LeaveNetwork.Enabled")) return;
        // Construct the webhook
        DiscordWebhook discordWebhook = new DiscordWebhook(webhookUrl);
        // Define variables
        CorePlayer player = event.getPlayer();
        // Check if custom webhook is enabled
        if (
                discordConfig.getBoolean(
                        "Messages.LeaveNetwork.CustomWebhook.Enabled"
                )
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
        if (discordConfig.getBoolean("Messages.LeaveNetwork.Embed.Enabled")) {
            discordWebhook.addEmbed(
                    getEmbedFromConfigJoinLeave(
                            "Messages.LeaveNetwork.Embed",
                            player,
                            true
                    )
            );
        }
        if (
                !discordConfig.getString("Messages.LeaveNetwork.Content").isEmpty()
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

    private String replacePlaceholdersSwap(String txt, CorePlayer player, String toServer, String fromServer) {
        String displayTo = MessageHandler.getInstance().getServerDisplayName(toServer);
        String displayFrom = MessageHandler.getInstance().getServerDisplayName(fromServer);
        return
            MessageHandler.stripColor(
                    MessageHandler.getInstance()
                        .formatMessage(txt, player)
                )
                    .replace("%embedavatarurl%", getEmbedAvatarUrl(player))
                    .replace("%to%", displayTo)
                    .replace("%to_clean%", MessageHandler.sanitize(displayTo))
                    .replace("%from%", displayFrom)
                    .replace("%from_clean%", MessageHandler.sanitize(displayFrom))
                    .replace(
                            "%playercount_from%", MessageHandler.getInstance()
                                    .getServerPlayerCount(fromServer, true, player)
                    )
                    .replace(
                            "%playercount_to%", MessageHandler.getInstance()
                                    .getServerPlayerCount(toServer, false, player)
                    )
                    .replace(
                            "%playercount_network%", MessageHandler.getInstance()
                                    .getNetworkPlayerCount(player, false)
                    );
    }

    private String getSwapConfigValue(
            String key,
            CorePlayer player,
            String toServer,
            String fromServer
    ) {
        return replacePlaceholdersSwap(
                discordConfig.getString(key),
                player,
                toServer,
                fromServer
        );
    }

    private String replacePlaceholdersJoinLeave(
            String txt,
            CorePlayer player,
            boolean leaving
    ) {
        return
            MessageHandler.stripColor(
                    MessageHandler.getInstance()
                        .formatMessage(txt, player)
                )
                    .replace("%embedavatarurl%", getEmbedAvatarUrl(player))
                    .replace(
                            "%playercount_server%", MessageHandler.getInstance()
                                    .getServerPlayerCount(
                                            player.getCurrentServer(),
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
            CorePlayer player,
            boolean leaving
    ) {
        return replacePlaceholdersJoinLeave(
                discordConfig.getString(key),
                player,
                leaving
        );
    }

    private String getEmbedAvatarUrl(CorePlayer player) {
        return discordConfig
                .getString("EmbedAvatarUrl")
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%player%", player.getName());
    }

    @Nullable
    private DiscordWebhook.EmbedObject getEmbedFromConfigSwap(
            @NotNull String key,
            @NotNull CorePlayer player,
            @NotNull String toServer,
            @NotNull String fromServer
    ) {
        if (discordConfig.get(key) == null) {
            return null;
        }

        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject();

        String hexColor = discordConfig.getString(key + ".Color");

        if (hexColor.isEmpty()) {
            corePlugin
                    .getCoreLogger()
                    .warn("A color was missing from embed config!");
            hexColor = "#000000";
        }

        hexColor = hexColor.trim();
        if (!hexColor.startsWith("#")) {
            hexColor = "#" + hexColor;
        }
        if (hexColor.length() != 7) {
            corePlugin
                    .getCoreLogger()
                    .warn("An invalid color: " + hexColor + " was provided!");
            hexColor = "#000000";
        }

        // Set the color
        embed.setColor(Color.decode(hexColor));

        // Set the author
        if (discordConfig.get(key + ".Author", null) != null) {
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

        // Set the thumbnail url
        if (!discordConfig.getString(key + ".ThumbnailUrl").isEmpty()) {
            embed.setThumbnail(discordConfig.getString(key + ".ThumbnailUrl"));
        }

        // Set the title
        if (discordConfig.get(key + ".Title", null) != null) {
            if (!discordConfig.getString(key + ".Title.Text").isEmpty()) {
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

        // Set the description
        if (!discordConfig.getString(key + ".Description").isEmpty()) {
            embed.setDescription(
                    getSwapConfigValue(
                            key + ".Description",
                            player,
                            toServer,
                            fromServer
                    )
            );
        }

        // Set the fields
        List<String> fields = discordConfig.getStringList(key + ".Fields");
        if (!fields.isEmpty()) {
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

        // Set the image url
        if (!discordConfig.getString(key + ".ImageUrl").isEmpty()) {
            embed.setImage(
                    getSwapConfigValue(
                            key + ".ImageUrl",
                            player,
                            toServer,
                            fromServer
                    )
            );
        }

        // Set the footer
        if (discordConfig.get(key + ".Footer", null) != null) {
            if (!discordConfig.getString(key + ".Footer.Text").isEmpty()) {
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

        // Set the timestamp
        if (discordConfig.getBoolean(key + ".Timestamp")) {
            embed.setTimestamp((new Date()).toInstant());
        }

        return embed;
    }

    @Nullable
    private DiscordWebhook.EmbedObject getEmbedFromConfigJoinLeave(
            @NotNull String key,
            @NotNull CorePlayer player,
            boolean leaving
    ) {
        if (discordConfig.get(key) == null) {
            return null;
        }

        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject();

        String hexColor = discordConfig.getString(key + ".Color");

        if (hexColor.isEmpty()) {
            corePlugin
                    .getCoreLogger()
                    .warn("A color was missing from embed config!");
            hexColor = "#000000";
        }

        hexColor = hexColor.trim();
        if (!hexColor.startsWith("#")) {
            hexColor = "#" + hexColor;
        }
        if (hexColor.length() != 7) {
            corePlugin
                    .getCoreLogger()
                    .warn("An invalid color: " + hexColor + " was provided!");
            hexColor = "#000000";
        }

        // Set the color
        embed.setColor(Color.decode(hexColor));

        // Set the author
        if (discordConfig.get(key + ".Author", null) != null) {
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

        // Set the thumbnail url
        if (!discordConfig.getString(key + ".ThumbnailUrl").isEmpty()) {
            embed.setThumbnail(
                    getJoinLeaveConfigValue(key + ".ThumbnailUrl", player, leaving)
            );
        }

        // Set the title
        if (discordConfig.get(key + ".Title", null) != null) {
            if (!discordConfig.getString(key + ".Title.Text").isEmpty()) {
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

        // Set the description
        if (!discordConfig.getString(key + ".Description").isEmpty()) {
            embed.setDescription(
                    getJoinLeaveConfigValue(key + ".Description", player, leaving)
            );
        }

        // Set the fields
        List<String> fields = discordConfig.getStringList(key + ".Fields");
        if (!fields.isEmpty()) {
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

        // Set the image url
        if (!discordConfig.getString(key + ".ImageUrl").isEmpty()) {
            embed.setImage(
                    getJoinLeaveConfigValue(key + ".ImageUrl", player, leaving)
            );
        }

        // Set the footer
        if (discordConfig.get(key + ".Footer", null) != null) {
            if (!discordConfig.getString(key + ".Footer.Text").isEmpty()) {
                embed.setFooter(
                        getJoinLeaveConfigValue(
                                key + ".Footer.Text",
                                player,
                                leaving
                        ),
                        getJoinLeaveConfigValue(
                                key + ".Footer.Icon",
                                player,
                                leaving
                        )
                );
            }
        }

        // Set the timestamp
        if (discordConfig.getBoolean(key + ".Timestamp")) {
            embed.setTimestamp((new Date()).toInstant());
        }

        return embed;
    }
}
