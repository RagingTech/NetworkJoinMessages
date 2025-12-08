package xyz.earthcow.networkjoinmessages.common.commands;

import xyz.earthcow.networkjoinmessages.common.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.MessageHandler;
import xyz.earthcow.networkjoinmessages.common.Storage;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.modules.DiscordIntegration;
import xyz.earthcow.networkjoinmessages.common.util.Formatter;

import java.util.List;

public class CoreReloadCommand implements Command {

    private final CorePlugin plugin;
    private final ConfigManager configManager;
    private final Storage storage;
    private final Formatter formatter;
    private final MessageHandler messageHandler;
    private final DiscordIntegration discordIntegration;

    public CoreReloadCommand(CorePlugin plugin, ConfigManager configManager, Storage storage, Formatter formatter, MessageHandler messageHandler, DiscordIntegration discordIntegration) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.storage = storage;
        this.formatter = formatter;
        this.messageHandler = messageHandler;
        this.discordIntegration = discordIntegration;
    }

    @Override
    public void execute(CoreCommandSender coreCommandSender, String[] args) {
        if (coreCommandSender.hasPermission("networkjoinmessages.reload")) {
            configManager.reload();
            storage.setUpDefaultValuesFromConfig();
            formatter.setPPBRequestTimeout(storage.getPPBRequestTimeout());
            discordIntegration.loadVariables();
            messageHandler.initCacheTasks();

            // Update all player's cached leave message
            plugin.runTaskAsync(() -> {
                plugin.getAllPlayers().forEach(messageHandler::updateCachedLeaveMessage);
                messageHandler.sendMessage(coreCommandSender,
                        storage.getReloadConfirmation()
                );
            });
        }
    }

    @Override
    public String getRequiredPermission() {
        return "networkjoinmessages.reload";
    }

    @Override
    public List<String> getTabCompletion(CoreCommandSender coreCommandSender, String[] args) {
        return null;
    }
}