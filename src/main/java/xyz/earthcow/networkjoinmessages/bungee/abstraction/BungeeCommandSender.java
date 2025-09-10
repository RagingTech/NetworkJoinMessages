package xyz.earthcow.networkjoinmessages.bungee.abstraction;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import xyz.earthcow.networkjoinmessages.bungee.BungeeMain;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;

public class BungeeCommandSender implements CoreCommandSender {
    private final CommandSender source;
    public BungeeCommandSender(CommandSender bungeeCommandSource) {
        this.source = bungeeCommandSource;
    }

    @Override
    public String getName() {
        if (source instanceof ProxiedPlayer) {
            return source.getName();
        } else if (source == BungeeMain.getInstance().getProxy().getConsole()) {
            return "Console";
        } else {
            return "Unknown";
        }
    }

    @Override
    public void sendMessage(String message) {
        source.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message)));
    }

    @Override
    public void sendMessage(Component component) {
        BungeeMain.getInstance().getAudiences().sender(source).sendMessage(component);
    }

    @Override
    public boolean hasPermission(String permission) {
        return source.hasPermission(permission);
    }
}
