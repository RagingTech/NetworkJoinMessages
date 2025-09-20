package xyz.earthcow.networkjoinmessages.bungee.events;

import net.md_5.bungee.api.plugin.Event;
import xyz.earthcow.networkjoinmessages.common.events.SwapServerEvent;

public class BungeeSwapServerEvent extends Event {
    private final SwapServerEvent data;

    public BungeeSwapServerEvent(SwapServerEvent data) {
        this.data = data;
    }

    public SwapServerEvent getData() {
        return data;
    }
}