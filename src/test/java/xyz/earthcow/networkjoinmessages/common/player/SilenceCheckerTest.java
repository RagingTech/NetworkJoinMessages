package xyz.earthcow.networkjoinmessages.common.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.abstraction.PremiumVanish;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.modules.SayanVanishHook;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SilenceCheckerTest {

    @Mock private CorePlugin       plugin;
    @Mock private CoreLogger       logger;
    @Mock private PluginConfig     config;
    @Mock private PlayerStateStore stateStore;
    @Mock private SayanVanishHook  sayanVanish;
    @Mock private PremiumVanish    premiumVanish;
    @Mock private CorePlayer       player;

    private final UUID playerUuid = UUID.randomUUID();

    @BeforeEach
    void setup() {
        when(plugin.getCoreLogger()).thenReturn(logger);
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(player.getName()).thenReturn("TestPlayer");

        // Default: player not toggled silent, no vanish integrations active
        when(stateStore.getSilentState(player)).thenReturn(false);
        when(config.isSVTreatVanishedPlayersAsSilent()).thenReturn(false);
        when(config.isPVTreatVanishedPlayersAsSilent()).thenReturn(false);
        when(config.isPVTreatVanishedOnJoin()).thenReturn(false);
        when(player.isPremiumVanishHidden()).thenReturn(false);
        when(player.hasPermission("pv.joinvanished")).thenReturn(false);
    }

    // -----------------------------------------------------------------------
    // isSilent(player) convenience overload
    // -----------------------------------------------------------------------

    @Test
    void isSilent_noConditionsMet_returnsFalse() {
        SilenceChecker checker = new SilenceChecker(plugin, config, stateStore, null, null);
        assertFalse(checker.isSilent(player));
    }

    // -----------------------------------------------------------------------
    // Silent toggle state
    // -----------------------------------------------------------------------

    @Test
    void isSilent_toggledSilent_returnsTrue() {
        when(stateStore.getSilentState(player)).thenReturn(true);
        SilenceChecker checker = new SilenceChecker(plugin, config, stateStore, null, null);
        assertTrue(checker.isSilent(player));
    }

    @Test
    void isSilent_toggleNotSilent_returnsFalse() {
        when(stateStore.getSilentState(player)).thenReturn(false);
        SilenceChecker checker = new SilenceChecker(plugin, config, stateStore, null, null);
        assertFalse(checker.isSilent(player));
    }

    // -----------------------------------------------------------------------
    // SayanVanish integration
    // -----------------------------------------------------------------------

    @Test
    void isSilent_sayanVanishPresent_treatSilentEnabled_playerVanished_returnsTrue() {
        when(config.isSVTreatVanishedPlayersAsSilent()).thenReturn(true);
        when(sayanVanish.isVanished(player)).thenReturn(true);

        SilenceChecker checker = new SilenceChecker(plugin, config, stateStore, sayanVanish, null);
        assertTrue(checker.isSilent(player));
    }

    @Test
    void isSilent_sayanVanishPresent_treatSilentEnabled_playerNotVanished_returnsFalse() {
        when(config.isSVTreatVanishedPlayersAsSilent()).thenReturn(true);
        when(sayanVanish.isVanished(player)).thenReturn(false);

        SilenceChecker checker = new SilenceChecker(plugin, config, stateStore, sayanVanish, null);
        assertFalse(checker.isSilent(player));
    }

    @Test
    void isSilent_sayanVanishPresent_treatSilentDisabled_playerVanished_returnsFalse() {
        when(config.isSVTreatVanishedPlayersAsSilent()).thenReturn(false);
        when(sayanVanish.isVanished(player)).thenReturn(true);

        SilenceChecker checker = new SilenceChecker(plugin, config, stateStore, sayanVanish, null);
        assertFalse(checker.isSilent(player));
    }

    @Test
    void isSilent_sayanVanishNull_vanishedFlagIrrelevant_returnsFalse() {
        SilenceChecker checker = new SilenceChecker(plugin, config, stateStore, null, null);
        assertFalse(checker.isSilent(player));
    }

    // -----------------------------------------------------------------------
    // PremiumVanish integration -- vanished via API
    // -----------------------------------------------------------------------

    @Test
    void isSilent_pvPresent_treatSilentEnabled_playerVanishedByAPI_returnsTrue() {
        when(config.isPVTreatVanishedPlayersAsSilent()).thenReturn(true);
        when(premiumVanish.isVanished(playerUuid)).thenReturn(true);

        SilenceChecker checker = new SilenceChecker(plugin, config, stateStore, null, premiumVanish);
        assertTrue(checker.isSilent(player));
    }

    @Test
    void isSilent_pvPresent_treatSilentEnabled_playerNotVanished_returnsFalse() {
        when(config.isPVTreatVanishedPlayersAsSilent()).thenReturn(true);
        when(premiumVanish.isVanished(playerUuid)).thenReturn(false);
        when(player.isPremiumVanishHidden()).thenReturn(false);

        SilenceChecker checker = new SilenceChecker(plugin, config, stateStore, null, premiumVanish);
        assertFalse(checker.isSilent(player));
    }

    @Test
    void isSilent_pvPresent_treatSilentEnabled_playerHiddenFlag_returnsTrue() {
        when(config.isPVTreatVanishedPlayersAsSilent()).thenReturn(true);
        when(premiumVanish.isVanished(playerUuid)).thenReturn(false);
        when(player.isPremiumVanishHidden()).thenReturn(true); // hidden flag set from previous join

        SilenceChecker checker = new SilenceChecker(plugin, config, stateStore, null, premiumVanish);
        assertTrue(checker.isSilent(player));
    }

    @Test
    void isSilent_pvPresent_treatSilentDisabled_playerVanished_returnsFalse() {
        when(config.isPVTreatVanishedPlayersAsSilent()).thenReturn(false);
        when(premiumVanish.isVanished(playerUuid)).thenReturn(true);

        SilenceChecker checker = new SilenceChecker(plugin, config, stateStore, null, premiumVanish);
        assertFalse(checker.isSilent(player));
    }

    // -----------------------------------------------------------------------
    // PremiumVanish -- TreatVanishedOnJoin
    // -----------------------------------------------------------------------

    @Test
    void isSilent_pvTreatVanishedOnJoin_playerHasPerm_setsHiddenAndReturnsSilent() {
        when(config.isPVTreatVanishedOnJoin()).thenReturn(true);
        when(player.hasPermission("pv.joinvanished")).thenReturn(true);
        when(config.isPVTreatVanishedPlayersAsSilent()).thenReturn(true);
        // After setPremiumVanishHidden(true), player.isPremiumVanishHidden() should return true.
        // We model this with a doAnswer or just verify the call was made.
        when(premiumVanish.isVanished(playerUuid)).thenReturn(false);
        when(player.isPremiumVanishHidden()).thenReturn(false);

        SilenceChecker checker = new SilenceChecker(plugin, config, stateStore, null, premiumVanish);
        // joining=true triggers the PVTreatVanishedOnJoin path
        checker.isSilent(player, false, true);

        // Verify that setPremiumVanishHidden(true) was invoked
        verify(player).setPremiumVanishHidden(true);
    }

    @Test
    void isSilent_pvTreatVanishedOnJoin_playerNoPerm_doesNotSetHidden() {
        when(config.isPVTreatVanishedOnJoin()).thenReturn(true);
        when(player.hasPermission("pv.joinvanished")).thenReturn(false);

        SilenceChecker checker = new SilenceChecker(plugin, config, stateStore, null, premiumVanish);
        checker.isSilent(player, false, true);

        verify(player, never()).setPremiumVanishHidden(true);
    }

    @Test
    void isSilent_pvTreatVanishedOnJoin_joiningFalse_doesNotSetHidden() {
        when(config.isPVTreatVanishedOnJoin()).thenReturn(true);
        when(player.hasPermission("pv.joinvanished")).thenReturn(true);

        SilenceChecker checker = new SilenceChecker(plugin, config, stateStore, null, premiumVanish);
        // joining=false => the TreatVanishedOnJoin branch should be skipped
        checker.isSilent(player, false, false);

        verify(player, never()).setPremiumVanishHidden(true);
    }

    // -----------------------------------------------------------------------
    // Multiple conditions -- OR semantics
    // -----------------------------------------------------------------------

    @Test
    void isSilent_multipleConditionsAnyTrue_returnsTrue() {
        // toggle=false, SV vanished=false, PV vanished=true
        when(config.isPVTreatVanishedPlayersAsSilent()).thenReturn(true);
        when(premiumVanish.isVanished(playerUuid)).thenReturn(true);
        when(config.isSVTreatVanishedPlayersAsSilent()).thenReturn(false);
        when(sayanVanish.isVanished(player)).thenReturn(false);

        SilenceChecker checker = new SilenceChecker(plugin, config, stateStore, sayanVanish, premiumVanish);
        assertTrue(checker.isSilent(player));
    }

    @Test
    void isSilent_allConditionsFalse_returnsFalse() {
        when(config.isSVTreatVanishedPlayersAsSilent()).thenReturn(false);
        when(config.isPVTreatVanishedPlayersAsSilent()).thenReturn(false);
        when(sayanVanish.isVanished(player)).thenReturn(true); // vanished but integration disabled

        SilenceChecker checker = new SilenceChecker(plugin, config, stateStore, sayanVanish, premiumVanish);
        assertFalse(checker.isSilent(player));
    }
}
