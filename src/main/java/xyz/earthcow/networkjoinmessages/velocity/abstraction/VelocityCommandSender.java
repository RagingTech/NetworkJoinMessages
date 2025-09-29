package xyz.earthcow.networkjoinmessages.velocity.abstraction;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.velocity.VelocityMain;

public class VelocityCommandSender implements CoreCommandSender {
    private final CommandSource source;
    public VelocityCommandSender(CommandSource velocityCommandSource) {
        this.source = velocityCommandSource;
    }

    @Override
    public String getName() {
        if (source instanceof Player) {
            return ((Player) source).getUsername();
        } else if (source == VelocityMain.getInstance().getProxy().getConsoleCommandSource()) {
            return "Console";
        } else {
            return "Unknown";
        }
    }

    @Override
    public void sendMessage(Component component) {
        source.sendMessage(component);
    }

    @Override
    public boolean hasPermission(String permission) {
        return source.hasPermission(permission);
    }
}
