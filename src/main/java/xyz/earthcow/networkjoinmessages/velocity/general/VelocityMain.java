package xyz.earthcow.networkjoinmessages.velocity.general;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import xyz.earthcow.networkjoinmessages.velocity.commands.FakeCommand;
import xyz.earthcow.networkjoinmessages.velocity.commands.ReloadCommand;
import xyz.earthcow.networkjoinmessages.velocity.commands.ToggleJoinCommand;
import xyz.earthcow.networkjoinmessages.velocity.listeners.PlayerListener;
import xyz.earthcow.networkjoinmessages.velocity.listeners.VanishListener;
import xyz.earthcow.networkjoinmessages.velocity.modules.DiscordWebhookIntegration;
import xyz.earthcow.networkjoinmessages.velocity.util.HexChat;
import xyz.earthcow.networkjoinmessages.velocity.util.MessageHandler;

@Plugin(
    id = "networkjoinmessages",
    name = "NetworkJoinMessages",
    version = "2.1.0",
    url = "https://github.com/RagingTech/NetworkJoinMessages",
    description = "A plugin handling join, leave and switch messages for proxy servers.",
    authors = { "EarthCow" }
)
public class VelocityMain {

    private static VelocityMain instance;

    public static VelocityMain getInstance() {
        return instance;
    }

    private final ProxyServer proxy;

    public ProxyServer getProxy() {
        return proxy;
    }

    private final Logger logger;

    public Logger getLogger() {
        return logger;
    }

    private final Path dataDirectory;

    public Path getDataDirectory() {
        return dataDirectory;
    }

    private CommentedConfigurationNode rootNode;

    public CommentedConfigurationNode getRootNode() {
        return rootNode;
    }

    private DiscordWebhookIntegration discordWebhookIntegration;

    public DiscordWebhookIntegration getDiscordWebhookIntegration() {
        return discordWebhookIntegration;
    }

    public boolean VanishAPI = false;
    public boolean LuckPermsAPI = false;

    @Inject
    public VelocityMain(
        ProxyServer proxy,
        Logger logger,
        @DataDirectory Path dataDirectory
    ) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        instance = this;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        loadConfig();
        discordWebhookIntegration = new DiscordWebhookIntegration();

        MessageHandler.getInstance().setupConfigMessages();
        Storage.getInstance().setUpDefaultValuesFromConfig();
        proxy.getEventManager().register(this, new PlayerListener());

        CommandManager commandManager = proxy.getCommandManager();
        commandManager.register(
            commandManager
                .metaBuilder("fakemessage")
                .aliases("fmr")
                .plugin(this)
                .build(),
            new FakeCommand()
        );
        commandManager.register(
            commandManager
                .metaBuilder("networkjoinreload")
                .aliases("njoinreload")
                .plugin(this)
                .build(),
            new ReloadCommand()
        );
        commandManager.register(
            commandManager
                .metaBuilder("togglejoinmessage")
                .aliases("njointoggle")
                .plugin(this)
                .build(),
            new ToggleJoinCommand()
        );

        if (proxy.getPluginManager().getPlugin("premiumvanish").isPresent()) {
            getLogger().info("Detected PremiumVanish! - Using API.");
            this.VanishAPI = true;
            proxy.getEventManager().register(this, new VanishListener());
        }
        if (proxy.getPluginManager().getPlugin("luckperms").isPresent()) {
            getLogger().info("Detected Luckperms! - Using API.");
            this.LuckPermsAPI = true;
        }
    }

    private void loadConfig() {
        File dataFolder = dataDirectory.toFile();
        if (!dataFolder.exists()) dataFolder.mkdir();

        File file = dataDirectory.resolve("config.yml").toFile();
        if (!file.exists()) {
            try (
                InputStream in = getClass()
                    .getClassLoader()
                    .getResourceAsStream("config.yml")
            ) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
            .path(dataDirectory.resolve("config.yml"))
            .build();

        try {
            rootNode = loader.load();
        } catch (IOException e) {
            System.err.println(
                "An error occurred while loading the configuration: " +
                e.getMessage()
            );
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }
        }
    }

    /**
     * Attempt to load values from configfile.
     */
    public void reloadConfig() {
        loadConfig();
        MessageHandler.getInstance().setupConfigMessages();
        Storage.getInstance().setUpDefaultValuesFromConfig();
    }

    /**
     * Used when there's no specific from/to server.
     * @param type - Type of event
     * @param name - Name of player.
     */
    public void SilentEvent(String type, String name) {
        SilentEvent(type, name, "", "");
    }

    /**
     * Used to send a move message.
     * @param type - The type of event that is silenced.
     * @param name - Name of the player.
     * @param from - Name of the server that is being moved from.
     * @param to - Name of the server that is being moved to.
     */
    public void SilentEvent(String type, String name, String from, String to) {
        String message = "";
        switch (type) {
            case "MOVE":
                message = rootNode
                    .node("Messages", "Misc", "ConsoleSilentMoveEvent")
                    .getString();
                message = message == null || message.isEmpty()
                    ? message =
                        "&1Move Event was silenced. <player> <from> -> <to>"
                    : message;
                message = message.replace("<to>", to);
                message = message.replace("<from>", from);
                break;
            case "QUIT":
                message = rootNode
                    .node("Messages", "Misc", "ConsoleSilentQuitEvent")
                    .getString();
                message = message == null || message.isEmpty()
                    ? message =
                        "&6Quit Event was silenced. <player> left the network."
                    : message;
                break;
            case "JOIN":
                message = rootNode
                    .node("Messages", "Misc", "ConsoleSilentJoinEvent")
                    .getString();
                message = message == null || message.isEmpty()
                    ? message =
                        "&6Join Event was silenced. <player> joined the network."
                    : message;
                break;
            default:
                return;
        }
        message = message.replace("<player>", name);
        getLogger().info(HexChat.translateHexCodes(message));
    }
}
