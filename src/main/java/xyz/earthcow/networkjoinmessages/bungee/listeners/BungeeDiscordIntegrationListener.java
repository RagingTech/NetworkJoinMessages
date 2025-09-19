package xyz.earthcow.networkjoinmessages.bungee.listeners;

import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import xyz.earthcow.networkjoinmessages.bungee.events.BungeeNetworkJoinEvent;
import xyz.earthcow.networkjoinmessages.bungee.events.BungeeNetworkLeaveEvent;
import xyz.earthcow.networkjoinmessages.bungee.events.BungeeSwapServerEvent;
import xyz.earthcow.networkjoinmessages.common.modules.DiscordWebhookIntegration;

public class BungeeDiscordIntegrationListener implements Listener {

    private final DiscordWebhookIntegration discordIntegration;

    public BungeeDiscordIntegrationListener(DiscordWebhookIntegration discordIntegration) {
        this.discordIntegration = discordIntegration;
    }

    @EventHandler
    public void onSwapServerEvent(BungeeSwapServerEvent event) {
        discordIntegration.onSwapServer(event.getData());
    }

    @EventHandler
    public void onNetworkJoinEvent(BungeeNetworkJoinEvent event) {
        discordIntegration.onNetworkJoin(event.getData());
    }

    @EventHandler
    public void onNetworkLeaveEvent(BungeeNetworkLeaveEvent event) {
        discordIntegration.onNetworkQuit(event.getData());
    }

}
