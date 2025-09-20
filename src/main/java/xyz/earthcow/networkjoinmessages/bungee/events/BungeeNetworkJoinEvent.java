package xyz.earthcow.networkjoinmessages.bungee.events;

import net.md_5.bungee.api.plugin.Event;
import xyz.earthcow.networkjoinmessages.common.events.NetworkJoinEvent;

public class BungeeNetworkJoinEvent extends Event {
    private final NetworkJoinEvent data;

    public BungeeNetworkJoinEvent(NetworkJoinEvent data) {
        this.data = data;
    }

    public NetworkJoinEvent getData() {
        return data;
    }
}