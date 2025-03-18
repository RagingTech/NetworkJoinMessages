package xyz.earthcow.networkjoinmessages.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import xyz.earthcow.networkjoinmessages.common.listeners.CorePlayerListener;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityPlayer;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityServer;

public class PlayerListener {

    private final CorePlayerListener corePlayerListener = new CorePlayerListener();

    @Subscribe
    public void onPreConnect(ServerPreConnectEvent event) {
        if (event.getPreviousServer() == null) {
            return;
        }

        corePlayerListener.onPreConnect(new VelocityPlayer(event.getPlayer()), event.getPreviousServer().getServerInfo().getName());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        corePlayerListener.onServerConnected(new VelocityPlayer(event.getPlayer()), new VelocityServer(event.getServer()));
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        corePlayerListener.onDisconnect(new VelocityPlayer(event.getPlayer()));
    }
}
