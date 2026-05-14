package xyz.earthcow.networkjoinmessages.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PremiumVanishLevelUtilTest {

    @Mock private CorePlayer player;

    // Helper: configure player to have exactly one numbered use level
    private void grantUseLevel(int level) {
        // Base permission
        when(player.hasPermission("pv.use")).thenReturn(level >= 1);
        // All numbered levels
        for (int i = 1; i <= 100; i++) {
            when(player.hasPermission("pv.use.level" + i)).thenReturn(i <= level);
        }
    }

    private void grantSeeLevel(int level) {
        when(player.hasPermission("pv.see")).thenReturn(level >= 1);
        for (int i = 1; i <= 100; i++) {
            when(player.hasPermission("pv.see.level" + i)).thenReturn(i <= level);
        }
    }

    // -----------------------------------------------------------------------
    // updateVanishLevels -- use level
    // -----------------------------------------------------------------------

    @Test
    void updateVanishLevels_noPermissions_useLevelIsZero() {
        when(player.hasPermission(anyString())).thenReturn(false);
        PremiumVanishLevelUtil.updateVanishLevels(player);
        verify(player).setPremiumVanishUseLevel(0);
    }

    @Test
    void updateVanishLevels_baseUsePerm_useLevelIsOne() {
        when(player.hasPermission(anyString())).thenReturn(false);
        when(player.hasPermission("pv.use")).thenReturn(true);
        // No numbered levels
        PremiumVanishLevelUtil.updateVanishLevels(player);
        verify(player).setPremiumVanishUseLevel(1);
    }

    @ParameterizedTest(name = "pv.use.level{0} granted => useLevel={0}")
    @ValueSource(ints = {1, 2, 5, 10, 50, 99, 100})
    void updateVanishLevels_numberedUsePerm_useLevelMatchesHighest(int level) {
        grantUseLevel(level);
        PremiumVanishLevelUtil.updateVanishLevels(player);
        verify(player).setPremiumVanishUseLevel(level);
    }

    // -----------------------------------------------------------------------
    // updateVanishLevels -- see level
    // -----------------------------------------------------------------------

    @Test
    void updateVanishLevels_noPermissions_seeLevelIsZero() {
        when(player.hasPermission(anyString())).thenReturn(false);
        PremiumVanishLevelUtil.updateVanishLevels(player);
        verify(player).setPremiumVanishSeeLevel(0);
    }

    @Test
    void updateVanishLevels_baseSeePerm_seeLevelIsOne() {
        when(player.hasPermission(anyString())).thenReturn(false);
        when(player.hasPermission("pv.see")).thenReturn(true);
        PremiumVanishLevelUtil.updateVanishLevels(player);
        verify(player).setPremiumVanishSeeLevel(1);
    }

    @ParameterizedTest(name = "pv.see.level{0} granted => seeLevel={0}")
    @ValueSource(ints = {1, 3, 7, 25, 75, 100})
    void updateVanishLevels_numberedSeePerm_seeLevelMatchesHighest(int level) {
        grantSeeLevel(level);
        PremiumVanishLevelUtil.updateVanishLevels(player);
        verify(player).setPremiumVanishSeeLevel(level);
    }

    // -----------------------------------------------------------------------
    // updateVanishLevels -- both levels set in a single call
    // -----------------------------------------------------------------------

    @Test
    void updateVanishLevels_bothLevelsSetAtOnce() {
        grantUseLevel(3);
        grantSeeLevel(7);
        PremiumVanishLevelUtil.updateVanishLevels(player);
        verify(player).setPremiumVanishUseLevel(3);
        verify(player).setPremiumVanishSeeLevel(7);
    }

    // -----------------------------------------------------------------------
    // updateVanishLevels -- highest level is truly the highest
    // -----------------------------------------------------------------------

    @Test
    void updateVanishLevels_onlyHighestLevelGranted_returnsCorrectLevel() {
        // Player has ONLY pv.use.level50 (no 1-49, no 51-100, no base pv.use)
        when(player.hasPermission(anyString())).thenReturn(false);
        when(player.hasPermission("pv.use.level50")).thenReturn(true);

        PremiumVanishLevelUtil.updateVanishLevels(player);
        verify(player).setPremiumVanishUseLevel(50);
    }

    // -----------------------------------------------------------------------
    // updateVanishLevels -- no NPE when player has no permissions
    // -----------------------------------------------------------------------

    @Test
    void updateVanishLevels_doesNotThrow() {
        when(player.hasPermission(anyString())).thenReturn(false);
        assertDoesNotThrow(() -> PremiumVanishLevelUtil.updateVanishLevels(player));
    }

    // -----------------------------------------------------------------------
    // Boundary -- level 100 is the maximum
    // -----------------------------------------------------------------------

    @Test
    void updateVanishLevels_levelCappedAt100() {
        grantUseLevel(100);
        PremiumVanishLevelUtil.updateVanishLevels(player);
        verify(player).setPremiumVanishUseLevel(100);
        // No call should be made for level 101
        verify(player, never()).hasPermission("pv.use.level101");
    }
}
