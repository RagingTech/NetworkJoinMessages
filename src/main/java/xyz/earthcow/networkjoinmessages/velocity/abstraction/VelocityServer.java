package xyz.earthcow.networkjoinmessages.velocity.abstraction;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.jetbrains.annotations.NotNull;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.velocity.VelocityMain;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class VelocityServer implements CoreBackendServer {
    private final RegisteredServer velocityServer;

    public VelocityServer(@NotNull RegisteredServer velocityServer) {
        this.velocityServer = Objects.requireNonNull(velocityServer, "velocityServer must not be null");
    }

    @Override
    public String getName() {
        return velocityServer.getServerInfo().getName();
    }

    @Override
    public List<CorePlayer> getPlayersConnected() {
        return velocityServer.getPlayersConnected().stream()
            .map(player -> VelocityMain.getInstance().getOrCreatePlayer(player.getUniqueId()))
            .collect(Collectors.toList());
    }
}
