package xyz.earthcow.networkjoinmessages.velocity.abstraction;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.velocity.general.VelocityMain;

import java.util.List;
import java.util.stream.Collectors;

public class VelocityServer implements CoreBackendServer {
    private final RegisteredServer velocityServer;

    public VelocityServer(RegisteredServer velocityServer) {
        this.velocityServer = velocityServer;
    }

    @Override
    public String getName() {
        if (velocityServer == null) {
            return null;
        }
        return velocityServer.getServerInfo().getName();
    }

    @Override
    public List<CorePlayer> getPlayersConnected() {
        if (velocityServer == null) {
            return null;
        }
        return velocityServer.getPlayersConnected().stream().map(player -> VelocityMain.getInstance().getOrCreatePlayer(player.getUniqueId())).collect(Collectors.toList());
    }
}
