package xyz.earthcow.networkjoinmessages.common.general;

import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.commands.CoreFakeCommand;
import xyz.earthcow.networkjoinmessages.common.commands.CoreImportCommand;
import xyz.earthcow.networkjoinmessages.common.commands.CoreReloadCommand;
import xyz.earthcow.networkjoinmessages.common.commands.CoreToggleJoinCommand;
import xyz.earthcow.networkjoinmessages.common.modules.DiscordWebhookIntegration;
import xyz.earthcow.networkjoinmessages.common.util.H2PlayerJoinTracker;
import xyz.earthcow.networkjoinmessages.common.util.MessageHandler;

public class NetworkJoinMessagesCore {
    private static NetworkJoinMessagesCore instance;

    public static NetworkJoinMessagesCore getInstance() {
        return instance;
    }

    private final CorePlugin plugin;

    public CorePlugin getPlugin() {
        return plugin;
    }

    private final DiscordWebhookIntegration discordWebhookIntegration;
    private H2PlayerJoinTracker firstJoinTracker;

    public DiscordWebhookIntegration getDiscordWebhookIntegration() {
        return discordWebhookIntegration;
    }

    public final CoreImportCommand coreImportCommand = new CoreImportCommand();
    public final CoreFakeCommand coreFakeCommand = new CoreFakeCommand();
    public final CoreReloadCommand coreReloadCommand = new CoreReloadCommand();
    public final CoreToggleJoinCommand coreToggleJoinCommand = new CoreToggleJoinCommand();

    public NetworkJoinMessagesCore(CorePlugin plugin) {
        this.plugin = plugin;

        instance = this;

        loadConfig();
        discordWebhookIntegration = new DiscordWebhookIntegration();

        try {
            firstJoinTracker = new H2PlayerJoinTracker("./" + plugin.getDataFolder().getPath() + "/joined");
        } catch (Exception ex) {
            plugin.getCoreLogger().severe("Failed to load H2 first join tracker!");
        }

    }

    public void loadConfig() {
        ConfigManager.setupConfig(plugin.getCoreLogger(), plugin.getDataFolder());
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
        String message;
        switch (type) {
            case "MOVE":
                message = ConfigManager.getPluginConfig().getString("Messages.Misc.ConsoleSilentMoveEvent")
                    .replace("<to>", to)
                    .replace("<from>", from);
                break;
            case "QUIT":
                message = ConfigManager.getPluginConfig().getString("Messages.Misc.ConsoleSilentQuitEvent");
                break;
            case "JOIN":
                message = ConfigManager.getPluginConfig().getString("Messages.Misc.ConsoleSilentJoinEvent");
                break;
            default:
                return;
        }
        message = message.replace("<player>", name);
        plugin.getCoreLogger().info(message);
    }

    public H2PlayerJoinTracker getFirstJoinTracker() {
        return firstJoinTracker;
    }
}
