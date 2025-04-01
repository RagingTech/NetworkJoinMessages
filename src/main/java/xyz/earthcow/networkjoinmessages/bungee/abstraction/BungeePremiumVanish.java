package xyz.earthcow.networkjoinmessages.bungee.abstraction;

import xyz.earthcow.networkjoinmessages.common.abstraction.PremiumVanish;

import java.util.List;
import java.util.UUID;

public class BungeePremiumVanish implements PremiumVanish {

    public BungeePremiumVanish() {

    }

    @Override
    public List<UUID> getInvisiblePlayers() {
        return List.of();
    }

    @Override
    public boolean isVanished(UUID uuid) {
        return false;
    }

}
