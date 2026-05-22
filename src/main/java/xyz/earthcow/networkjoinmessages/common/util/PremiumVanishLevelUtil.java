package xyz.earthcow.networkjoinmessages.common.util;

import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;

public final class PremiumVanishLevelUtil {

    private final int max_level;

    public PremiumVanishLevelUtil(int max_level) {
        this.max_level = max_level;
    }

    /**
     * Determines a player's vanish level by scanning down from {@link PremiumVanishLevelUtil#max_level} to obtain
     * their highest level
     * @param player The player to analyze
     * @param forUse If {@code true}, we use {@code pv.use} otherwise we use {@code pv.see}
     * @return The determined highest vanish level for the specified player
     */
    private int determineVanishLevel(CorePlayer player, boolean forUse) {
        String base = forUse ? "pv.use" : "pv.see";
        String prefix = base + ".level";
        for (int i = max_level; i >= 1; i--) {
            if (player.hasPermission(prefix + i)) {
                return i;
            }
        }
        // The base permission (pv.use or pv.see) is a level of 1
        return player.hasPermission(base) ? 1 : 0;
    }

    /**
     * Updates a player's PremiumVanish use and see vanish levels. Uses
     * {@link PremiumVanishLevelUtil#determineVanishLevel(CorePlayer, boolean)} to do so. The logic is unpreventably
     * costly as it iterates down from {@link PremiumVanishLevelUtil#max_level} checking player permission each time.
     * <b>Should be called asynchronously to the main thread.</b>
     * @param player The player in which vanish levels will be updated for
     */
    public void updateVanishLevels(CorePlayer player) {
        player.setPremiumVanishUseLevel(determineVanishLevel(player, true));
        player.setPremiumVanishSeeLevel(determineVanishLevel(player, false));
    }

}
