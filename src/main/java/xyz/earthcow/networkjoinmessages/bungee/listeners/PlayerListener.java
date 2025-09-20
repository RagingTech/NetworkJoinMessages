package xyz.earthcow.networkjoinmessages.bungee.listeners;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import xyz.earthcow.networkjoinmessages.bungee.BungeeMain;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeePlayer;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeeServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.listeners.CorePlayerListener;

public class PlayerListener implements Listener {

    private final CorePlayerListener corePlayerListener;

    public PlayerListener(CorePlayerListener corePlayerListener) {
        this.corePlayerListener = corePlayerListener;
    }

    @EventHandler
    public void onPreServerConnected(ServerConnectEvent e) {
        ProxiedPlayer player = e.getPlayer();
        if (player == null) {
            return;
        }

        Server server = player.getServer();
        if (server != null) {
            corePlayerListener.onPreConnect(BungeeMain.getInstance().getOrPutPlayer(new BungeePlayer(player)), server.getInfo().getName());
        }
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent e) {
        corePlayerListener.onServerConnected(BungeeMain.getInstance().getOrPutPlayer(new BungeePlayer(e.getPlayer())), new BungeeServer(e.getServer().getInfo()), null);
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        // Check that the player disconnected is not a duplicate user session (the same account tries to join the server while already joined)
        CorePlayer corePlayer = BungeeMain.getInstance().getOrPutPlayer(new BungeePlayer(event.getPlayer()));
        if (corePlayer.getConnectionIdentity() != System.identityHashCode(event.getPlayer())) {
            // TODO: Debug message here
            return;
        }

        corePlayerListener.onDisconnect(corePlayer);
    }
}
