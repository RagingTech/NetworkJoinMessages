package xyz.earthcow.networkjoinmessages.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.listeners.CorePlayerListener;
import xyz.earthcow.networkjoinmessages.velocity.VelocityMain;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityPlayer;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityServer;

public class PlayerListener {

    private final CorePlayerListener corePlayerListener;

    public PlayerListener(CorePlayerListener corePlayerListener) {
        this.corePlayerListener = corePlayerListener;
    }

    @Subscribe
    public void onPreConnect(ServerPreConnectEvent event) {
        if (event.getPreviousServer() == null) {
            return;
        }

        corePlayerListener.onPreConnect(VelocityMain.getInstance().getOrPutPlayer(new VelocityPlayer(event.getPlayer())), event.getPreviousServer().getServerInfo().getName());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        VelocityServer previousServer = event.getPreviousServer().isPresent() ? new VelocityServer(event.getPreviousServer().get()) : null;
        corePlayerListener.onServerConnected(VelocityMain.getInstance().getOrPutPlayer(new VelocityPlayer(event.getPlayer())), new VelocityServer(event.getServer()), previousServer);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        // Check that the player disconnected is not a duplicate user session (the same account tries to join the server while already joined)
        CorePlayer corePlayer = VelocityMain.getInstance().getOrPutPlayer(new VelocityPlayer(event.getPlayer()));
        if (corePlayer.getConnectionIdentity() != System.identityHashCode(event.getPlayer())) {
            return;
        }

        corePlayerListener.onDisconnect(corePlayer);
    }
}
