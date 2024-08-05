package xyz.earthcow.networkjoinmessages.bungee.listeners;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeePlayer;
import xyz.earthcow.networkjoinmessages.bungee.abstraction.BungeeServer;
import xyz.earthcow.networkjoinmessages.common.listeners.CorePlayerListener;

public class PlayerListener implements Listener {

    private final CorePlayerListener corePlayerListener = new CorePlayerListener();

    @EventHandler
    public void prePlayerSwitchServer(ServerConnectEvent e) {
        ProxiedPlayer player = e.getPlayer();
        if (player == null) {
            return;
        }

        Server server = player.getServer();
        if (server != null) {
            corePlayerListener.onPreConnect(new BungeePlayer(player), server.getInfo().getName());
        }
    }

    @EventHandler
    public void onPlayerSwitchServer(ServerConnectedEvent e) {
        corePlayerListener.onServerConnected(new BungeePlayer(e.getPlayer()), new BungeeServer(e.getServer().getInfo()));
    }

    @EventHandler
    public void onPostQuit(PlayerDisconnectEvent event) {
        corePlayerListener.onDisconnect(new BungeePlayer(event.getPlayer()));
    }
}
