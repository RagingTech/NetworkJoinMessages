package xyz.earthcow.networkjoinmessages.common.listeners;

import org.jetbrains.annotations.NotNull;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.player.PlayerStateStore;
import xyz.earthcow.networkjoinmessages.common.util.SpoofManager;

public class CorePremiumVanishListener {

    private final CoreLogger logger;
    private final PluginConfig config;
    private final SpoofManager spoofManager;
    private final PlayerStateStore stateStore;

    public CorePremiumVanishListener(@NotNull CoreLogger logger, @NotNull PluginConfig config, @NotNull SpoofManager spoofManager, @NotNull PlayerStateStore stateStore) {
        this.logger = logger;
        this.config = config;
        this.spoofManager = spoofManager;
        this.stateStore = stateStore;
    }

    public void handlePremiumVanishShow(@NotNull CorePlayer player) {
        if (!player.getPremiumVanishHidden()) return;
        logger.debug("Setting PremiumVanishHidden to FALSE for " + player.getName());
        player.setPremiumVanishHidden(false);
        if (config.isPVSpoofJoinMessageOnShow() && !stateStore.getSilentState(player)) {
            spoofManager.spoofJoin(player);
        }
    }

    public void handlePremiumVanishHide(@NotNull CorePlayer player) {
        if (player.getPremiumVanishHidden()) return;
        logger.debug("Setting PremiumVanishHidden to TRUE for " + player.getName());
        player.setPremiumVanishHidden(true);
        if (config.isPVSpoofLeaveMessageOnHide() && !stateStore.getSilentState(player)) {
            spoofManager.spoofLeave(player);
        }
    }
}
