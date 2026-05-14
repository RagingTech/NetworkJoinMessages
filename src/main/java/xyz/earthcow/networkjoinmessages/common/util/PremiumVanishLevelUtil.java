package xyz.earthcow.networkjoinmessages.common.util;

import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;

public final class PremiumVanishLevelUtil {

    private static final int MAX_LEVEL = 100;

    /**
     * Determines a player's vanish level by scanning down from {@link PremiumVanishLevelUtil#MAX_LEVEL} to obtain
     * their highest level
     * @param player The player to analyze
     * @param forUse If {@code true}, we use {@code pv.use} otherwise we use {@code pv.see}
     * @return The determined highest vanish level for the specified player
     */
    private static int determineVanishLevel(CorePlayer player, boolean forUse) {
        String base = forUse ? "pv.use" : "pv.see";
        String prefix = base + ".level";
        for (int i = MAX_LEVEL; i >= 1; i--) {
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
     * costly as it iterates down from {@link PremiumVanishLevelUtil#MAX_LEVEL} checking player permission each time.
     * <b>Should be called asynchronously to the main thread.</b>
     * @param player The player in which vanish levels will be updated for
     */
    public static void updateVanishLevels(CorePlayer player) {
        player.setPremiumVanishUseLevel(determineVanishLevel(player, true));
        player.setPremiumVanishSeeLevel(determineVanishLevel(player, false));
    }

}
