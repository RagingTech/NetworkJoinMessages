package xyz.earthcow.networkjoinmessages.common.commands;

import xyz.earthcow.networkjoinmessages.common.MessageHandler;
import xyz.earthcow.networkjoinmessages.common.Storage;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.modules.DiscordIntegration;

import java.util.List;

public class CoreReloadCommand implements Command {

    private final Storage storage;
    private final DiscordIntegration discordIntegration;
    private final MessageHandler messageHandler;

    public CoreReloadCommand(Storage storage, DiscordIntegration discordIntegration, MessageHandler messageHandler) {
        this.storage = storage;
        this.discordIntegration = discordIntegration;
        this.messageHandler = messageHandler;
    }

    @Override
    public void execute(CoreCommandSender coreCommandSender, String[] args) {
        if (coreCommandSender.hasPermission("networkjoinmessages.reload")) {
            storage.setUpDefaultValuesFromConfig();
            discordIntegration.loadVariables();

            messageHandler.sendMessage(coreCommandSender,
                storage.getReloadConfirmation()
            );
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