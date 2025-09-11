package xyz.earthcow.networkjoinmessages.common;

import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.commands.CoreFakeCommand;
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

    private final CoreImportCommand coreImportCommand = new CoreImportCommand();
    private final CoreFakeCommand coreFakeCommand = new CoreFakeCommand();
    private final CoreReloadCommand coreReloadCommand = new CoreReloadCommand();
    private final CoreToggleJoinCommand coreToggleJoinCommand = new CoreToggleJoinCommand();

    public Core(CorePlugin plugin) {
        this.plugin = plugin;
        this.formatter = new Formatter(this);
        this.storage = new Storage(this);
        this.messageHandler = new MessageHandler(, formatter);

        loadConfigs();
        discordWebhookIntegration = new DiscordWebhookIntegration();

        try {
            firstJoinTracker = new H2PlayerJoinTracker("./" + plugin.getDataFolder().getPath() + "/joined");
        } catch (Exception ex) {
            plugin.getCoreLogger().severe("Failed to load H2 first join tracker!");
        }

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

    public CoreFakeCommand getCoreFakeCommand() {
        return coreFakeCommand;
    }

    public CoreReloadCommand getCoreReloadCommand() {
        return coreReloadCommand;
    }

    public CoreToggleJoinCommand getCoreToggleJoinCommand() {
        return coreToggleJoinCommand;
    }
}
