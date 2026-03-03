package xyz.earthcow.networkjoinmessages.bungee.abstraction;

import net.md_5.bungee.api.config.ServerInfo;
import org.jetbrains.annotations.NotNull;
import xyz.earthcow.networkjoinmessages.bungee.BungeeMain;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BungeeServer implements CoreBackendServer {
    private final ServerInfo bungeeServer;

    public BungeeServer(@NotNull ServerInfo bungeeServer) {
        this.bungeeServer = Objects.requireNonNull(bungeeServer, "bungeeServer must not be null");
    }

    @Override
    public String getName() {
        return bungeeServer.getName();
    }

    @Override
    public List<CorePlayer> getPlayersConnected() {
        return bungeeServer.getPlayers().stream()
            .map(proxiedPlayer -> BungeeMain.getInstance().getOrCreatePlayer(proxiedPlayer.getUniqueId()))
            .collect(Collectors.toList());
    }
}
