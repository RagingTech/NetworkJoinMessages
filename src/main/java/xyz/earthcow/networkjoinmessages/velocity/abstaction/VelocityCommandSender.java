package xyz.earthcow.networkjoinmessages.velocity.abstaction;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreCommandSender;
import xyz.earthcow.networkjoinmessages.velocity.general.VelocityMain;

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
    public void sendMessage(String message) {
        source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
    }

    @Override
    public boolean hasPermission(String permission) {
        return source.hasPermission(permission);
    }
}
