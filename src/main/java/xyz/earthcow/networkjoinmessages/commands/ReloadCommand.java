package xyz.earthcow.networkjoinmessages.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import xyz.earthcow.networkjoinmessages.BungeeJoinMessages.Main;
import xyz.earthcow.networkjoinmessages.util.HexChat;

public class ReloadCommand extends Command {
    public ReloadCommand() {
        super("bungeejoinreload","bungeejoinmessages.reload","bjoinreload");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(sender.hasPermission("bungeejoinmessages.reload")) {
            Main.getInstance().reloadConfig();
            Main.getInstance().getDiscordWebhookIntegration().loadConfig();
			String msg = Main.getInstance().getConfig().getString("Messages.Commands.Reload.ConfigReloaded", 
					"Config Reloaded!");
            sender.sendMessage( new TextComponent(HexChat.translateHexCodes(msg)));
        }
    }
}
