package xyz.earthcow.networkjoinmessages.bungee.abstraction;

import net.md_5.bungee.api.config.ServerInfo;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;

import java.util.List;
import java.util.stream.Collectors;

public class BungeeServer implements CoreBackendServer {
    private final ServerInfo bungeeServer;

    public BungeeServer(ServerInfo bungeeServer) {
        this.bungeeServer = bungeeServer;
    }

    @Override
    public String getName() {
        if (bungeeServer == null) {
            return null;
        }
        return bungeeServer.getName();
    }

    @Override
    public List<CorePlayer> getPlayersConnected() {
        if (bungeeServer == null) {
            return null;
        }
        return bungeeServer.getPlayers().stream().map(BungeePlayer::new).collect(Collectors.toList());
    }
}
