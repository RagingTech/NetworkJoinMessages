package xyz.earthcow.networkjoinmessages.common.events;

/**
 * Called when a player swaps from one server to another on the network
 */
public record SwapServerEvent(java.util.UUID playerUniqueId, String serverFrom, String serverTo, boolean isSilenced, String message,
                              String cleanMessage) {

}
