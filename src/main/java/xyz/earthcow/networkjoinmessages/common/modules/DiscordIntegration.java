package xyz.earthcow.networkjoinmessages.common.modules;

import dev.dejvokep.boostedyaml.YamlDocument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.discordwebhook.DiscordWebhook;
import xyz.earthcow.networkjoinmessages.common.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.MessageHandler;
import xyz.earthcow.networkjoinmessages.common.Storage;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.events.NetworkJoinEvent;
import xyz.earthcow.networkjoinmessages.common.events.NetworkLeaveEvent;
import xyz.earthcow.networkjoinmessages.common.events.SwapServerEvent;
import xyz.earthcow.networkjoinmessages.common.util.Formatter;

import java.awt.*;
import java.util.Date;
import java.util.List;

public class DiscordIntegration {

    private final CorePlugin plugin;
    private final Formatter formatter;
    private final Storage storage;
    private final MessageHandler messageHandler;

    private YamlDocument discordConfig;
    private String webhookUrl;

    public DiscordIntegration(CorePlugin plugin, Storage storage, Formatter formatter, MessageHandler messageHandler) {
        this.plugin = plugin;
        this.storage = storage;
        this.formatter = formatter;
        this.messageHandler = messageHandler;
    }

    public void loadVariables(){
        discordConfig = ConfigManager.getDiscordConfig();
        if (!discordConfig.getBoolean("Enabled")) {
            plugin.unregisterDiscordListener();
            return;
        }
        // Set the main webhook url
        webhookUrl = discordConfig.getString("WebhookUrl");

        plugin.registerDiscordListener(this);
        plugin.getCoreLogger().info("Discord Integration is enabled!");
    }

    private void executeWebhook(DiscordWebhook webhook, CorePlayer parseTarget) {
        formatter.parsePlaceholdersAndThen(webhook.getJsonString(), parseTarget, formatted -> plugin.runTaskAsync(() -> {
            try {
                webhook.execute(formatted);
            } catch (Exception e) {
                plugin
                    .getCoreLogger()
                    .warn(
                        "[DiscordIntegration] There is a problem with your configuration! Verify the webhook url and all config values. Make sure anything that is supposed to be a url is either blank or a valid url."
                    );
            }
        }));
    }

    // Event handlers
    public void onSwapServer(SwapServerEvent event) {
        // Ignore if the event is silenced
        if (event.isSilenced()) return;
        // Ignore if the message is disabled
        if (!discordConfig.getBoolean("Messages.SwapServer.Enabled")) return;
        // Construct the webhook
        DiscordWebhook discordWebhook = new DiscordWebhook(webhookUrl);
        // Define variables
        CorePlayer player = event.player();
        String toServer = event.serverToDisplay();
        String fromServer = event.serverFromDisplay();
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
                    getEmbedFromConfig(
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
        executeWebhook(discordWebhook, player);
    }

    public void onNetworkJoin(NetworkJoinEvent event) {
        // Ignore if the event is silenced
        if (event.isSilenced()) return;
        // Determine the key by checking if this is the first time the player joined
        String key = event.isFirstJoin() ? "Messages.FirstJoinNetwork" : "Messages.JoinNetwork";
        // Ignore if the message is disabled
        if (!discordConfig.getBoolean(key + ".Enabled")) return;
        // Construct the webhook
        DiscordWebhook discordWebhook = new DiscordWebhook(webhookUrl);
        // Define variables
        CorePlayer player = event.player();
        // Check if custom webhook is enabled
        if (discordConfig.getBoolean(key + ".CustomWebhook.Enabled")) {
            discordWebhook.setUsername(
                    getJoinLeaveConfigValue(
                        key + ".CustomWebhook.Name",
                            player,
                            false
                    )
            );
            discordWebhook.setAvatarUrl(
                    getJoinLeaveConfigValue(
                        key + ".CustomWebhook.AvatarUrl",
                            player,
                            false
                    )
            );
        }
        if (discordConfig.getBoolean(key + ".Embed.Enabled")) {
            discordWebhook.addEmbed(
                    getEmbedFromConfig(
                        key + ".Embed",
                            player,
                            "false"
                    )
            );
        }
        if (!discordConfig.getString(key + ".Content").isEmpty()) {
            discordWebhook.setContent(
                    getJoinLeaveConfigValue(
                        key + ".Content",
                            player,
                            false
                    )
            );
        }
        executeWebhook(discordWebhook, player);
    }

    public void onNetworkLeave(NetworkLeaveEvent event) {
        // Ignore if the event is silenced
        if (event.isSilenced()) return;
        // Ignore if the message is disabled
        if (!discordConfig.getBoolean("Messages.LeaveNetwork.Enabled")) return;
        // Construct the webhook
        DiscordWebhook discordWebhook = new DiscordWebhook(webhookUrl);
        // Define variables
        CorePlayer player = event.player();
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
                    getEmbedFromConfig(
                            "Messages.LeaveNetwork.Embed",
                            player,
                            "true"
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
        executeWebhook(discordWebhook, player);
    }

    private String replacePlaceholdersSwap(String txt, CorePlayer player, String toServer, String fromServer) {
        String displayTo = storage.getServerDisplayName(toServer);
        String displayFrom = storage.getServerDisplayName(fromServer);
        return txt
                .replace("%embedavatarurl%", getEmbedAvatarUrl(player))
                .replace("%to%", displayTo)
                .replace("%to_clean%", Formatter.sanitize(displayTo))
                .replace("%from%", displayFrom)
                .replace("%from_clean%", Formatter.sanitize(displayFrom))
                .replace(
                        "%playercount_from%", messageHandler
                                .getServerPlayerCount(fromServer, true, player)
                )
                .replace(
                        "%playercount_to%", messageHandler
                                .getServerPlayerCount(toServer, false, player)
                )
                .replace(
                        "%playercount_network%", messageHandler
                                .getNetworkPlayerCount(player, false)
                );
    }

    private String getSwapConfigValue(String key, CorePlayer player, String toServer, String fromServer) {
        return replacePlaceholdersSwap(
                discordConfig.getString(key),
                player,
                toServer,
                fromServer
        );
    }

    private String replacePlaceholdersJoinLeave(String txt, CorePlayer player, boolean leaving) {
        return txt
                .replace("%embedavatarurl%", getEmbedAvatarUrl(player))
                .replace(
                        "%playercount_server%", messageHandler
                                .getServerPlayerCount(
                                        player.getCurrentServer(),
                                        leaving,
                                        player
                                )
                )
                .replace(
                        "%playercount_network%",
                        messageHandler
                                .getNetworkPlayerCount(player, leaving)
                );
    }

    private String getJoinLeaveConfigValue(String key, CorePlayer player, boolean leaving) {
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

    private String generalConfigFormat(String key, CorePlayer player, String... args) {
        return generalFormat(discordConfig.getString(key), player, args);
    }

    private String generalFormat(String txt, CorePlayer player, String... args) {
        if (args.length == 2) {
            return replacePlaceholdersSwap(
                txt,
                player,
                args[0],
                args[1]
            );
        }
        return replacePlaceholdersJoinLeave(
            txt,
            player,
            Boolean.parseBoolean(args[0])
        );
    }

    @Nullable
    private DiscordWebhook.EmbedObject getEmbedFromConfig(
        @NotNull String key,
        @NotNull CorePlayer player,
        @NotNull String... formatterArgs
    ) {
        if (discordConfig.get(key) == null) {
            return null;
        }

        DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject();

        String hexColor = discordConfig.getString(key + ".Color");

        if (hexColor.isEmpty()) {
            plugin
                .getCoreLogger()
                .warn("A color was missing from embed config!");
            hexColor = "#000000";
        }

        hexColor = hexColor.trim();
        if (!hexColor.startsWith("#")) {
            hexColor = "#" + hexColor;
        }
        if (hexColor.length() != 7) {
            plugin
                .getCoreLogger()
                .warn("An invalid color: " + hexColor + " was provided!");
            hexColor = "#000000";
        }

        // Set the color
        embed.setColor(Color.decode(hexColor));

        // Set the author
        if (discordConfig.get(key + ".Author", null) != null) {
            String authorName = generalConfigFormat(
                key + ".Author.Name",
                player,
                formatterArgs
            );
            if (!authorName.isEmpty()) {
                embed.setAuthor(
                    authorName,
                    generalConfigFormat(
                        key + ".Author.Url",
                        player,
                        formatterArgs
                    ),
                    generalConfigFormat(
                        key + ".Author.ImageUrl",
                        player,
                        formatterArgs
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
                    generalConfigFormat(
                        key + ".Title.Text",
                        player,
                        formatterArgs
                    )
                );
                embed.setUrl(
                    generalConfigFormat(
                        key + ".Title.Url",
                        player,
                        formatterArgs
                    )
                );
            }
        }

        // Set the description
        if (!discordConfig.getString(key + ".Description").isEmpty()) {
            embed.setDescription(
                generalConfigFormat(
                    key + ".Description",
                    player,
                    formatterArgs
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
                        generalFormat(
                            parts[0],
                            player,
                            formatterArgs
                        ),
                        generalFormat(
                            parts[1],
                            player,
                            formatterArgs
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
                generalConfigFormat(
                    key + ".ImageUrl",
                    player,
                    formatterArgs
                )
            );
        }

        // Set the footer
        if (discordConfig.get(key + ".Footer", null) != null) {
            if (!discordConfig.getString(key + ".Footer.Text").isEmpty()) {
                embed.setFooter(
                    generalConfigFormat(
                        key + ".Footer.Text",
                        player,
                        formatterArgs
                    ),
                    generalConfigFormat(
                        key + ".Footer.IconUrl",
                        player,
                        formatterArgs
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
