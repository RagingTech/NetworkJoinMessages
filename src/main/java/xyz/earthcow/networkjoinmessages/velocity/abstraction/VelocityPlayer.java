package xyz.earthcow.networkjoinmessages.velocity.abstraction;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.velocity.VelocityMain;

import java.util.UUID;

public class VelocityPlayer extends CorePlayer {
    private final Player velocityPlayer;

    public VelocityPlayer(Player velocityPlayer) {
        super(
            velocityPlayer.getCurrentServer().isPresent() ?
                new VelocityServer(velocityPlayer.getCurrentServer().get().getServer())
                : null
            , Audience.audience(velocityPlayer)
        );
        this.velocityPlayer = velocityPlayer;
    }

    @Override
    public String getName() {
        return velocityPlayer.getUsername();
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
    public int getConnectionIdentity() {
        // Velocity player objects are unique to each session/connection
        return System.identityHashCode(velocityPlayer);
    }

    @Override
    public @Nullable CoreBackendServer getCurrentServer() {
        ServerConnection serverConnection = velocityPlayer.getCurrentServer().orElse(null);
        if (serverConnection == null) {
            return getLastKnownConnectedServer();
        }
        return new VelocityServer(serverConnection.getServer());
    }

    @Override
    public boolean isInLimbo() {
        if (!VelocityMain.getInstance().getIsLimboAPIAvailable()) {
            return false;
        }
        //noinspection ConstantValue
        return ((ConnectedPlayer) velocityPlayer).getConnection().getState().name() == null;
    }
}
