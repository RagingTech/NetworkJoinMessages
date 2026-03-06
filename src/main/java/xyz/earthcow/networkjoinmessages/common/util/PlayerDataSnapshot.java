package xyz.earthcow.networkjoinmessages.common.util;

import org.jetbrains.annotations.Nullable;

/**
 * Holds preferences/states set by the player or null if not set
 * @param silentState Silent/vanish state
 * @param ignoreJoin  Ignoring join messages
 * @param ignoreSwap  Ignoring swap messages
 * @param ignoreLeave Ignoring leave messages
 */
public record PlayerDataSnapshot(
    @Nullable Boolean silentState,
    @Nullable Boolean ignoreJoin,
    @Nullable Boolean ignoreSwap,
    @Nullable Boolean ignoreLeave
) {}
