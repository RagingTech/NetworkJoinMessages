package xyz.earthcow.networkjoinmessages.common.commands;

import net.kyori.adventure.text.Component;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.common.general.ConfigManager;
import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;
import xyz.earthcow.networkjoinmessages.common.util.MessageHandler;

import java.util.List;

public class CoreReloadCommand implements Command {

    @Override
    public void execute(CoreCommandSender coreCommandSender, String[] args) {
        if (coreCommandSender.hasPermission("networkjoinmessages.reload")) {
            NetworkJoinMessagesCore.getInstance().loadConfig();
            Component msg = MessageHandler.getMiniMessage().deserialize(
                ConfigManager.getPluginConfig().getString("Messages.Commands.Reload.ConfigReloaded")
            );
            coreCommandSender.sendMessage(msg);
        }
    }

    @Override
    public String getRequiredPermssion() {
        return "networkjoinmessages.reload";
    }

    @Override
    public List<String> getTabCompletion(CoreCommandSender coreCommandSender, String[] args) {
        return null;
    }
}