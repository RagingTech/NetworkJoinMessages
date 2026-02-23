package xyz.earthcow.networkjoinmessages.common;

/**
 * @deprecated Replaced by {@link xyz.earthcow.networkjoinmessages.common.config.PluginConfig}
 * (for config values) and {@link xyz.earthcow.networkjoinmessages.common.player.PlayerStateStore}
 * (for runtime player state). This class has been removed as part of the separation-of-concerns
 * refactor. Update call sites to use the appropriate focused class directly.
 */
@Deprecated
public final class Storage {
    private Storage() {
        throw new UnsupportedOperationException(
            "Storage has been split into PluginConfig and PlayerStateStore."
        );
    }
}
