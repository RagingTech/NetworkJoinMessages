package xyz.earthcow.networkjoinmessages.common;

import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.commands.CoreSpoofCommand;
import xyz.earthcow.networkjoinmessages.common.commands.CoreImportCommand;
import xyz.earthcow.networkjoinmessages.common.commands.CoreReloadCommand;
import xyz.earthcow.networkjoinmessages.common.commands.CoreToggleJoinCommand;
import xyz.earthcow.networkjoinmessages.common.modules.DiscordWebhookIntegration;
import xyz.earthcow.networkjoinmessages.common.util.Formatter;
import xyz.earthcow.networkjoinmessages.common.util.H2PlayerJoinTracker;

public class Core {
    private final CorePlugin plugin;
    private final Formatter formatter;
    private final Storage storage;
    private final MessageHandler messageHandler;

    private final DiscordWebhookIntegration discordWebhookIntegration;
    private H2PlayerJoinTracker firstJoinTracker;

    private final CoreImportCommand coreImportCommand;
    private final CoreSpoofCommand coreSpoofCommand;
    private final CoreReloadCommand coreReloadCommand;
    private final CoreToggleJoinCommand coreToggleJoinCommand;

    public Core(CorePlugin plugin) {
        this.plugin = plugin;
        this.storage = new Storage(plugin);
        this.formatter = new Formatter(plugin, storage);
        this.messageHandler = new MessageHandler(plugin, storage, formatter);

        loadConfigs();
        discordWebhookIntegration = new DiscordWebhookIntegration(plugin, storage, formatter, messageHandler);

        try {
            firstJoinTracker = new H2PlayerJoinTracker(plugin.getCoreLogger(), "./" + plugin.getDataFolder().getPath() + "/joined");
        } catch (Exception ex) {
            plugin.getCoreLogger().severe("Failed to load H2 first join tracker!");
        }

        this.coreImportCommand = new CoreImportCommand(this);
        this.coreSpoofCommand = new CoreSpoofCommand(storage, messageHandler);
        this.coreReloadCommand = new CoreReloadCommand(this);
        this.coreToggleJoinCommand = new CoreToggleJoinCommand(storage, messageHandler);

    }

    public void loadConfigs() {
        ConfigManager.setupConfigs(plugin);
        storage.setUpDefaultValuesFromConfig();
        if (discordWebhookIntegration != null) {
            discordWebhookIntegration.loadVariables();
        }
    }

    public CorePlugin getPlugin() {
        return plugin;
    }

    public Formatter getFormatter() {
        return formatter;
    }

    public Storage getStorage() {
        return storage;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public H2PlayerJoinTracker getFirstJoinTracker() {
        return firstJoinTracker;
    }

    public DiscordWebhookIntegration getDiscordWebhookIntegration() {
        return discordWebhookIntegration;
    }

    public CoreImportCommand getCoreImportCommand() {
        return coreImportCommand;
    }

    public CoreSpoofCommand getCoreSpoofCommand() {
        return coreSpoofCommand;
    }

    public CoreReloadCommand getCoreReloadCommand() {
        return coreReloadCommand;
    }

    public CoreToggleJoinCommand getCoreToggleCommand() {
        return coreToggleJoinCommand;
    }
}
