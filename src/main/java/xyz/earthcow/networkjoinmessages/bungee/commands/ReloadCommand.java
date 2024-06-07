package xyz.earthcow.networkjoinmessages.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import xyz.earthcow.networkjoinmessages.bungee.general.BungeeMain;
import xyz.earthcow.networkjoinmessages.bungee.util.HexChat;

public class ReloadCommand extends Command {

    public ReloadCommand() {
        super("bungeejoinreload", "networkjoinmessages.reload", "bjoinreload");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender.hasPermission("networkjoinmessages.reload")) {
            BungeeMain.getInstance().reloadConfig();
            BungeeMain.getInstance()
                .getDiscordWebhookIntegration()
                .loadConfig();
            String msg = BungeeMain.getInstance()
                .getConfig()
                .getString(
                    "Messages.Commands.Reload.ConfigReloaded",
                    "Config Reloaded!"
                );
            sender.sendMessage(
                new TextComponent(HexChat.translateHexCodes(msg))
            );
        }
    }
}
