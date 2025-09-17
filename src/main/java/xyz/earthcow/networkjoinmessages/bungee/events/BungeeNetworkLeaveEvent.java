package xyz.earthcow.networkjoinmessages.bungee.events;

import net.md_5.bungee.api.plugin.Event;
import xyz.earthcow.networkjoinmessages.common.events.NetworkQuitEvent;

public class BungeeNetworkLeaveEvent extends Event {
    private final NetworkQuitEvent data;

    public BungeeNetworkLeaveEvent(NetworkQuitEvent data) {
        this.data = data;
    }

    public NetworkQuitEvent getData() {
        return data;
    }
}