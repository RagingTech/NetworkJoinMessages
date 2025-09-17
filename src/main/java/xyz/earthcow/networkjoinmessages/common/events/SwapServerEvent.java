package xyz.earthcow.networkjoinmessages.common.events;

import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;

/**
 * Called when a player swaps from one server to another on the network
 */
public record SwapServerEvent(CorePlayer player, String serverFrom, String serverTo, boolean isSilenced, String message,
                              String cleanMessage) {

}
