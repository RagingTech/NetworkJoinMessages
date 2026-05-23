package xyz.earthcow.networkjoinmessages.common.util;

import org.junit.jupiter.api.BeforeEach;
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

    private static final int MAX_LEVEL = 100;

    @Mock private CorePlayer player;

    private PremiumVanishLevelUtil util;

    @BeforeEach
    void setUp() {
        util = new PremiumVanishLevelUtil(MAX_LEVEL);
    }

    private void grantUseLevel(int level) {
        when(player.hasPermission("pv.use")).thenReturn(level >= 1);
        for (int i = 1; i <= MAX_LEVEL; i++) {
            when(player.hasPermission("pv.use.level" + i)).thenReturn(i <= level);
        }
    }

    private void grantSeeLevel(int level) {
        when(player.hasPermission("pv.see")).thenReturn(level >= 1);
        for (int i = 1; i <= MAX_LEVEL; i++) {
            when(player.hasPermission("pv.see.level" + i)).thenReturn(i <= level);
        }
    }

    // -----------------------------------------------------------------------
    // updateVanishLevels -- use level
    // -----------------------------------------------------------------------

    @Test
    void updateVanishLevels_noPermissions_useLevelIsZero() {
        when(player.hasPermission(anyString())).thenReturn(false);
        util.updateVanishLevels(player);
        verify(player).setPremiumVanishUseLevel(0);
    }

    @Test
    void updateVanishLevels_baseUsePerm_useLevelIsOne() {
        when(player.hasPermission(anyString())).thenReturn(false);
        when(player.hasPermission("pv.use")).thenReturn(true);
        util.updateVanishLevels(player);
        verify(player).setPremiumVanishUseLevel(1);
    }

    @ParameterizedTest(name = "pv.use.level{0} granted => useLevel={0}")
    @ValueSource(ints = {1, 2, 5, 10, 50, 99, 100})
    void updateVanishLevels_numberedUsePerm_useLevelMatchesHighest(int level) {
        grantUseLevel(level);
        util.updateVanishLevels(player);
        verify(player).setPremiumVanishUseLevel(level);
    }

    // -----------------------------------------------------------------------
    // updateVanishLevels -- see level
    // -----------------------------------------------------------------------

    @Test
    void updateVanishLevels_noPermissions_seeLevelIsZero() {
        when(player.hasPermission(anyString())).thenReturn(false);
        util.updateVanishLevels(player);
        verify(player).setPremiumVanishSeeLevel(0);
    }

    @Test
    void updateVanishLevels_baseSeePerm_seeLevelIsOne() {
        when(player.hasPermission(anyString())).thenReturn(false);
        when(player.hasPermission("pv.see")).thenReturn(true);
        util.updateVanishLevels(player);
        verify(player).setPremiumVanishSeeLevel(1);
    }

    @ParameterizedTest(name = "pv.see.level{0} granted => seeLevel={0}")
    @ValueSource(ints = {1, 3, 7, 25, 75, 100})
    void updateVanishLevels_numberedSeePerm_seeLevelMatchesHighest(int level) {
        grantSeeLevel(level);
        util.updateVanishLevels(player);
        verify(player).setPremiumVanishSeeLevel(level);
    }

    // -----------------------------------------------------------------------
    // updateVanishLevels -- both levels set in a single call
    // -----------------------------------------------------------------------

    @Test
    void updateVanishLevels_bothLevelsSetAtOnce() {
        grantUseLevel(3);
        grantSeeLevel(7);
        util.updateVanishLevels(player);
        verify(player).setPremiumVanishUseLevel(3);
        verify(player).setPremiumVanishSeeLevel(7);
    }

    // -----------------------------------------------------------------------
    // updateVanishLevels -- highest level is truly the highest
    // -----------------------------------------------------------------------

    @Test
    void updateVanishLevels_onlyHighestLevelGranted_returnsCorrectLevel() {
        when(player.hasPermission(anyString())).thenReturn(false);
        when(player.hasPermission("pv.use.level50")).thenReturn(true);
        util.updateVanishLevels(player);
        verify(player).setPremiumVanishUseLevel(50);
    }

    // -----------------------------------------------------------------------
    // updateVanishLevels -- no NPE when player has no permissions
    // -----------------------------------------------------------------------

    @Test
    void updateVanishLevels_doesNotThrow() {
        when(player.hasPermission(anyString())).thenReturn(false);
        assertDoesNotThrow(() -> util.updateVanishLevels(player));
    }

    // -----------------------------------------------------------------------
    // Boundary -- level 100 is the maximum
    // -----------------------------------------------------------------------

    @Test
    void updateVanishLevels_levelCappedAt100() {
        grantUseLevel(MAX_LEVEL);
        util.updateVanishLevels(player);
        verify(player).setPremiumVanishUseLevel(MAX_LEVEL);
        verify(player, never()).hasPermission("pv.use.level" + (MAX_LEVEL + 1));
    }

    // -----------------------------------------------------------------------
    // Constructor -- max_level is respected
    // -----------------------------------------------------------------------

    @Test
    void updateVanishLevels_customMaxLevel_doesNotCheckBeyondMax() {
        int customMax = 10;
        PremiumVanishLevelUtil smallUtil = new PremiumVanishLevelUtil(customMax);
        when(player.hasPermission(anyString())).thenReturn(false);
        smallUtil.updateVanishLevels(player);
        verify(player, never()).hasPermission("pv.use.level11");
        verify(player, never()).hasPermission("pv.see.level11");
    }
}