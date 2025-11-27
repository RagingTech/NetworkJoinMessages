package xyz.earthcow.networkjoinmessages.velocity.listeners;

import de.myzelyam.api.vanish.VelocityPlayerHideEvent;
import de.myzelyam.api.vanish.VelocityPlayerShowEvent;
import me.neznamy.tab.api.event.Subscribe;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.PlayerManager;

public class PremiumVanishListener {

    private final PlayerManager manager;
    private final CoreLogger logger;

    public PremiumVanishListener(PlayerManager manager, CoreLogger logger) {
        this.manager = manager;
        this.logger = logger;
    }

    @Subscribe
    public void premiumVanishHideEvent(VelocityPlayerHideEvent event) {
        CorePlayer p = manager.getPlayer(event.getPlayer().getUniqueId());
        if (p == null) return;
        logger.debug("Setting PremiumVanishHidden to TRUE for player " + p.getName());
        p.setPremiumVanishHidden(true);
    }

    @Subscribe
    public void premiumVanishShowEvent(VelocityPlayerShowEvent event) {
        CorePlayer p = manager.getPlayer(event.getPlayer().getUniqueId());
        if (p == null) return;
        logger.debug("Setting PremiumVanishHidden to FALSE for player " + p.getName());
        p.setPremiumVanishHidden(false);
    }

}
