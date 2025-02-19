package xyz.earthcow.networkjoinmessages.velocity.abstraction;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;

import java.util.UUID;

public class VelocityPlayer implements CorePlayer {
    private final Player velocityPlayer;
    private CoreBackendServer lastKnownConnectedServer;
    public VelocityPlayer(Player velocityPlayer) {
        this.velocityPlayer = velocityPlayer;
        if (velocityPlayer.getCurrentServer().isPresent()) {
            this.lastKnownConnectedServer = new VelocityServer(velocityPlayer.getCurrentServer().get().getServer());
        }
    }

    @Override
    public String getName() {
        return velocityPlayer.getUsername();
    }

    @Override
    public void sendMessage(String message) {
        velocityPlayer.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
    }

    @Override
    public boolean hasPermission(String permission) {
        return velocityPlayer.hasPermission(permission);
    }

    @Override
    public @NotNull UUID getUniqueId() {
        return velocityPlayer.getUniqueId();
    }

    @Override
    public @Nullable CoreBackendServer getCurrentServer() {
        ServerConnection serverConnection = velocityPlayer.getCurrentServer().orElse(null);
        if (serverConnection == null) {
            return lastKnownConnectedServer;
        }
        return new VelocityServer(serverConnection.getServer());
    }

    @Override
    public @Nullable CoreBackendServer getLastKnownConnectedServer() {
        return lastKnownConnectedServer;
    }

    @Override
    public void setLastKnownConnectedServer(CoreBackendServer server) {
        lastKnownConnectedServer = server;
    }
}
