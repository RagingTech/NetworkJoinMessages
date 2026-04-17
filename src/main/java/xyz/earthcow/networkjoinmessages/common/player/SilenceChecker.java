package xyz.earthcow.networkjoinmessages.common.player;

import org.jetbrains.annotations.NotNull;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.abstraction.PremiumVanish;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.modules.SayanVanishHook;

import org.jetbrains.annotations.Nullable;

/**
 * Determines whether a player's network event (join/leave/swap) should be silent.
 * Aggregates signals from: per-player toggle, SayanVanish, and PremiumVanish.
 */
public final class SilenceChecker {

    private static final String PV_JOIN_VANISHED_PERM = "pv.joinvanished";

    private final CorePlugin plugin;
    private final PluginConfig config;
    private final PlayerStateStore stateStore;

    @Nullable private final SayanVanishHook sayanVanishHook;
    @Nullable private final PremiumVanish premiumVanish;

    public SilenceChecker(
            CorePlugin plugin,
            PluginConfig config,
            PlayerStateStore stateStore,
            @Nullable SayanVanishHook sayanVanishHook,
            @Nullable PremiumVanish premiumVanish
    ) {
        this.plugin = plugin;
        this.config = config;
        this.stateStore = stateStore;
        this.sayanVanishHook = sayanVanishHook;
        this.premiumVanish = premiumVanish;
    }

    public boolean isSilent(@NotNull CorePlayer player) {
        return isSilent(player, true, false);
    }

    /**
     * Returns true if the given player's event should be broadcast silently.
     *
     * <p>Silent when any of the following is true:
     * <ul>
     *   <li>The player has toggled their join messages off</li>
     *   <li>SayanVanish is present, treat-vanished-as-silent is enabled, and the player is vanished</li>
     *   <li>PremiumVanish is present, treat-vanished-as-silent is enabled, and the player is vanished
     *       or has the {@code pv.joinvanished} permission with TreatVanishedOnJoin enabled</li>
     * </ul>
     */
    public boolean isSilent(@NotNull CorePlayer player, boolean logDebug, boolean joining) {
        if (logDebug) logDebugState(player);

        if (joining && config.isPVTreatVanishedOnJoin() && player.hasPermission(PV_JOIN_VANISHED_PERM)) {
            player.setPremiumVanishHidden(true);
        }

        return stateStore.getSilentState(player)
            || isSayanVanishSilent(player)
            || isPremiumVanishSilent(player);
    }

    private boolean isSayanVanishSilent(CorePlayer player) {
        return sayanVanishHook != null
            && config.isSVTreatVanishedPlayersAsSilent()
            && sayanVanishHook.isVanished(player);
    }

    private boolean isPremiumVanishSilent(CorePlayer player) {
        return premiumVanish != null
            && config.isPVTreatVanishedPlayersAsSilent()
            && (premiumVanish.isVanished(player.getUniqueId()) || player.getPremiumVanishHidden());
    }

    private void logDebugState(CorePlayer player) {
        plugin.getCoreLogger().debug("Checking silence for player " + player.getName() + ":");
        plugin.getCoreLogger().debug(String.format(
            "  silentToggle=%s | SV[present=%s, treatSilent=%s, vanished=%s] | PV[present=%s, treatSilent=%s, vanished=%s, hidden=%s, treatOnJoin=%s, hasPerm=%s]",
            stateStore.getSilentState(player),
            sayanVanishHook != null,
            config.isSVTreatVanishedPlayersAsSilent(),
            sayanVanishHook != null ? sayanVanishHook.isVanished(player) : "N/A",
            premiumVanish != null,
            config.isPVTreatVanishedPlayersAsSilent(),
            premiumVanish != null ? premiumVanish.isVanished(player.getUniqueId()) : "N/A",
            player.getPremiumVanishHidden(),
            config.isPVTreatVanishedOnJoin(),
            player.hasPermission(PV_JOIN_VANISHED_PERM)
        ));
    }
}
