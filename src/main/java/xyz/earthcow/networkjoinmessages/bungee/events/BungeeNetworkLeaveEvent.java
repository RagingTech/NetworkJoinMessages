package xyz.earthcow.networkjoinmessages.bungee.events;

import net.md_5.bungee.api.plugin.Event;
import xyz.earthcow.networkjoinmessages.common.events.NetworkLeaveEvent;

public class BungeeNetworkLeaveEvent extends Event {
    private final NetworkLeaveEvent data;

    public BungeeNetworkLeaveEvent(NetworkLeaveEvent data) {
        this.data = data;
    }

    public NetworkLeaveEvent getData() {
        return data;
    }
}