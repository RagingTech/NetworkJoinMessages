package xyz.earthcow.networkjoinmessages.bungee.abstraction;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;

import java.util.UUID;

public class BungeePlayer implements CorePlayer {
    private final ProxiedPlayer bungeePlayer;
    public BungeePlayer(ProxiedPlayer bungeePlayer) {
        this.bungeePlayer = bungeePlayer;
    }

    @Override
    public String getName() {
        return bungeePlayer.getName();
    }

    @Override
    public void sendMessage(String message) {
        bungeePlayer.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message)));
    }

    @Override
    public boolean hasPermission(String permission) {
        return bungeePlayer.hasPermission(permission);
    }

    @Override
    public @NotNull UUID getUniqueId() {
        return bungeePlayer.getUniqueId();
    }

    @Override
    public @Nullable CoreBackendServer getCurrentServer() {
        Server server = bungeePlayer.getServer();
        if (server == null) {
            return null;
        }
        return new BungeeServer(server.getInfo());
    }
}
