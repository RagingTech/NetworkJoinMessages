package xyz.earthcow.networkjoinmessages.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import xyz.earthcow.networkjoinmessages.common.events.NetworkJoinEvent;
import xyz.earthcow.networkjoinmessages.common.events.NetworkLeaveEvent;
import xyz.earthcow.networkjoinmessages.common.events.SwapServerEvent;
import xyz.earthcow.networkjoinmessages.common.modules.DiscordWebhookIntegration;

public class VelocityDiscordListener {

    private final DiscordWebhookIntegration discordIntegration;

    public VelocityDiscordListener(DiscordWebhookIntegration discordIntegration) {
        this.discordIntegration = discordIntegration;
    }

    @Subscribe
    public void onSwapServer(SwapServerEvent event) {
        discordIntegration.onSwapServer(event);
    }

    @Subscribe
    public void onNetworkJoin(NetworkJoinEvent event) {
        discordIntegration.onNetworkJoin(event);
    }

    @Subscribe
    public void onNetworkLeave(NetworkLeaveEvent event) {
        discordIntegration.onNetworkQuit(event);
    }

}
