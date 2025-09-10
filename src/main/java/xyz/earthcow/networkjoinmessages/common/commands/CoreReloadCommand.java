package xyz.earthcow.networkjoinmessages.common.commands;

import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.Core;
import xyz.earthcow.networkjoinmessages.common.MessageHandler;

import java.util.List;

public class CoreReloadCommand implements Command {

    @Override
    public void execute(CoreCommandSender coreCommandSender, String[] args) {
        if (coreCommandSender.hasPermission("networkjoinmessages.reload")) {
            Core.getInstance().loadConfigs();
            MessageHandler.getInstance().sendMessage(coreCommandSender,
                ConfigManager.getPluginConfig().getString("Messages.Commands.Reload.ConfigReloaded")
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