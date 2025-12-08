package xyz.earthcow.networkjoinmessages.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import de.myzelyam.api.vanish.VelocityPlayerHideEvent;
import de.myzelyam.api.vanish.VelocityPlayerShowEvent;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.PlayerManager;
import xyz.earthcow.networkjoinmessages.common.listeners.CorePremiumVanishListener;

public class VelocityPremiumVanishListener {

    private final CorePremiumVanishListener corePremiumVanishListener;
    private final PlayerManager manager;

    public VelocityPremiumVanishListener(CorePremiumVanishListener corePremiumVanishListener, PlayerManager manager) {
        this.corePremiumVanishListener = corePremiumVanishListener;
        this.manager = manager;
    }

    @Subscribe
    public void premiumVanishShowEvent(VelocityPlayerShowEvent event) {
        CorePlayer p = manager.getPlayer(event.getPlayer().getUniqueId());
        if (p == null) return;
        corePremiumVanishListener.handlePremiumVanishShow(p);
    }

    @Subscribe
    public void premiumVanishHideEvent(VelocityPlayerHideEvent event) {
        CorePlayer p = manager.getPlayer(event.getPlayer().getUniqueId());
        if (p == null) return;
        corePremiumVanishListener.handlePremiumVanishHide(p);
    }

}
