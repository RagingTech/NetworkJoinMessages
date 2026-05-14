package xyz.earthcow.networkjoinmessages.bungee.abstraction;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.bungee.BungeeMain;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;

import java.util.UUID;

public class BungeePlayer extends CorePlayer {
    private final ProxiedPlayer bungeePlayer;

    public BungeePlayer(ProxiedPlayer bungeePlayer) {
        super(
            new BungeeServer(bungeePlayer.getServer().getInfo()),
            BungeeMain.getInstance().getAudiences().player(bungeePlayer)
        );
        this.bungeePlayer = bungeePlayer;
    }

    @Override
    public String getName() {
        return bungeePlayer.getName();
    }

    @Override
    public void sendMessage(Component component) {
        getAudience().sendMessage(component);
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
    public int getConnectionIdentity() {
        // Bungee player objects are unique to each session/connection
        return System.identityHashCode(bungeePlayer);
    }

    @Override
    public @Nullable CoreBackendServer getCurrentServer() {
        Server server = bungeePlayer.getServer();
        if (server == null) {
            return getLastKnownConnectedServer();
        }
        return new BungeeServer(server.getInfo());
    }

    @Override
    public boolean isInLimbo() {
        return false;
    }
}
