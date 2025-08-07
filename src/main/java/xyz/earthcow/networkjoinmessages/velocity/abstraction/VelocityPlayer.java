package xyz.earthcow.networkjoinmessages.velocity.abstraction;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.velocity.general.VelocityMain;

import java.util.UUID;

public class VelocityPlayer implements CorePlayer {
    private final Player velocityPlayer;
    private CoreBackendServer lastKnownConnectedServer;
    private final Audience audience;

    public VelocityPlayer(Player velocityPlayer) {
        this.velocityPlayer = velocityPlayer;
        if (velocityPlayer.getCurrentServer().isPresent()) {
            this.lastKnownConnectedServer = new VelocityServer(velocityPlayer.getCurrentServer().get().getServer());
        }
        this.audience = Audience.audience(velocityPlayer);
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
    public void sendMessage(Component component) {
        velocityPlayer.sendMessage(component);
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

    @Override
    public @NotNull Audience getAudience() {
        return audience;
    }

    @Override
    public boolean isInLimbo() {
        if (!VelocityMain.getInstance().getIsLimboAPIAvailable()) {
            return false;
        }
        return ((ConnectedPlayer) velocityPlayer).getConnection().getState().name() == null;
    }
}
