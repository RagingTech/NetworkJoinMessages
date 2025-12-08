package xyz.earthcow.networkjoinmessages.common.listeners;

import org.jetbrains.annotations.NotNull;
import xyz.earthcow.networkjoinmessages.common.Storage;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.util.SpoofManager;

public class CorePremiumVanishListener {
    private final CoreLogger logger;
    private final Storage storage;
    private final SpoofManager spoofManager;

    public CorePremiumVanishListener(@NotNull CoreLogger logger, @NotNull Storage storage, @NotNull SpoofManager spoofManager) {
        this.logger = logger;
        this.storage = storage;
        this.spoofManager = spoofManager;
    }

    public void handlePremiumVanishShow(@NotNull CorePlayer p) {
        logger.debug("Setting PremiumVanishHidden to FALSE for player " + p.getName());
        p.setPremiumVanishHidden(false);
        if (storage.isPVSpoofJoinMessageOnShow()) {
            spoofManager.spoofJoin(p);
        }
    }

    public void handlePremiumVanishHide(@NotNull CorePlayer p) {
        logger.debug("Setting PremiumVanishHidden to TRUE for player " + p.getName());
        p.setPremiumVanishHidden(true);
        if (storage.isPVSpoofLeaveMessageOnHide()) {
            spoofManager.spoofLeave(p);
        }
    }
}
