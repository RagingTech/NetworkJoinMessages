package xyz.earthcow.networkjoinmessages.bungee.listeners;

import de.myzelyam.api.vanish.BungeePlayerHideEvent;
import de.myzelyam.api.vanish.BungeePlayerShowEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.PlayerManager;
import xyz.earthcow.networkjoinmessages.common.listeners.CorePremiumVanishListener;

public class BungeePremiumVanishListener implements Listener {

    private final CorePremiumVanishListener corePremiumVanishListener;
    private final PlayerManager manager;

    public BungeePremiumVanishListener(CorePremiumVanishListener corePremiumVanishListener, PlayerManager manager) {
        this.corePremiumVanishListener = corePremiumVanishListener;
        this.manager = manager;
    }

    @EventHandler
    public void premiumVanishShowEvent(BungeePlayerShowEvent event) {
        CorePlayer p = manager.getPlayer(event.getPlayer().getUniqueId());
        if (p == null) return;
        corePremiumVanishListener.handlePremiumVanishShow(p);
    }

    @EventHandler
    public void premiumVanishHideEvent(BungeePlayerHideEvent event) {
        CorePlayer p = manager.getPlayer(event.getPlayer().getUniqueId());
        if (p == null) return;
        corePremiumVanishListener.handlePremiumVanishHide(p);
    }

}
