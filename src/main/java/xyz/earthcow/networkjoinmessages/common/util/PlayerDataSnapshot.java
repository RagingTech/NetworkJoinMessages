package xyz.earthcow.networkjoinmessages.common.util;

import org.jetbrains.annotations.Nullable;

/**
 * Holds preferences/states set by the player or null if not set
 * @param playerName  The last known username and is stored so that offline players can be resolved by name
 * @param silentState Silent/vanish state
 * @param ignoreJoin  Ignoring join messages
 * @param ignoreSwap  Ignoring swap messages
 * @param ignoreLeave Ignoring leave messages
 */
public record PlayerDataSnapshot(
    String            playerName,
    @Nullable Boolean silentState,
    @Nullable Boolean ignoreJoin,
    @Nullable Boolean ignoreSwap,
    @Nullable Boolean ignoreLeave
) {}
