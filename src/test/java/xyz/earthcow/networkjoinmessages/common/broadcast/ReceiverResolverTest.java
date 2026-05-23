package xyz.earthcow.networkjoinmessages.common.broadcast;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import xyz.earthcow.networkjoinmessages.common.MessageType;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReceiverResolverTest {

    @Mock private CorePlugin   plugin;
    @Mock private PluginConfig config;
    @Mock private CoreLogger   logger;
    @Mock private CorePlayer   lobbyPlayer;
    @Mock private CorePlayer   survivalPlayer;
    @Mock private CorePlayer   hubPlayer;
    @Mock private CoreBackendServer lobbyServer;
    @Mock private CoreBackendServer survivalServer;
    @Mock private CoreBackendServer hubServer;

    private final UUID lobbyUuid    = UUID.randomUUID();
    private final UUID survivalUuid = UUID.randomUUID();
    private final UUID hubUuid      = UUID.randomUUID();

    @BeforeEach
    void setup() {
        when(plugin.getCoreLogger()).thenReturn(logger);

        when(lobbyPlayer.getUniqueId()).thenReturn(lobbyUuid);
        when(survivalPlayer.getUniqueId()).thenReturn(survivalUuid);
        when(hubPlayer.getUniqueId()).thenReturn(hubUuid);

        when(lobbyPlayer.getCurrentServer()).thenReturn(lobbyServer);
        when(survivalPlayer.getCurrentServer()).thenReturn(survivalServer);
        when(hubPlayer.getCurrentServer()).thenReturn(hubServer);

        when(lobbyServer.getName()).thenReturn("lobby");
        when(survivalServer.getName()).thenReturn("survival");
        when(hubServer.getName()).thenReturn("hub");

        when(lobbyServer.getPlayersConnected()).thenReturn(List.of(lobbyPlayer));
        when(survivalServer.getPlayersConnected()).thenReturn(List.of(survivalPlayer));
        when(hubServer.getPlayersConnected()).thenReturn(List.of(hubPlayer));

        when(plugin.getServer("lobby")).thenReturn(lobbyServer);
        when(plugin.getServer("survival")).thenReturn(survivalServer);
        when(plugin.getServer("hub")).thenReturn(hubServer);
        when(plugin.getAllPlayers()).thenReturn(List.of(lobbyPlayer, survivalPlayer, hubPlayer));

        // Default: no blacklisted servers, blacklist mode
        when(config.getBlacklistedServers()).thenReturn(Collections.emptyList());
        when(config.isUseBlacklistAsWhitelist()).thenReturn(false);
        when(config.getSwapServerMessageRequires()).thenReturn("ANY");

        // Default suppression lists empty
        when(config.getServerFirstJoinMessageDisabled()).thenReturn(Collections.emptyList());
        when(config.getServerJoinMessageDisabled()).thenReturn(Collections.emptyList());
        when(config.getServerLeaveMessageDisabled()).thenReturn(Collections.emptyList());
    }

    // -----------------------------------------------------------------------
    // getJoinReceivers -- viewableByJoined / viewableByOther combinations
    // -----------------------------------------------------------------------

    @Test
    void getJoinReceivers_allFlagsTrue_returnsAllPlayers() {
        when(config.isJoinViewableByJoined()).thenReturn(true);
        when(config.isJoinViewableByOther()).thenReturn(true);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        List<CorePlayer> receivers = resolver.getJoinReceivers("lobby");

        assertEquals(3, receivers.size());
    }

    @Test
    void getJoinReceivers_onlyJoinedCanSee_returnsLobbyPlayersOnly() {
        when(config.isJoinViewableByJoined()).thenReturn(true);
        when(config.isJoinViewableByOther()).thenReturn(false);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        List<CorePlayer> receivers = resolver.getJoinReceivers("lobby");

        assertEquals(1, receivers.size());
        assertTrue(receivers.contains(lobbyPlayer));
    }

    @Test
    void getJoinReceivers_onlyOthersCanSee_excludesLobbyPlayers() {
        when(config.isJoinViewableByJoined()).thenReturn(false);
        when(config.isJoinViewableByOther()).thenReturn(true);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        List<CorePlayer> receivers = resolver.getJoinReceivers("lobby");

        assertFalse(receivers.contains(lobbyPlayer));
        assertTrue(receivers.contains(survivalPlayer));
        assertTrue(receivers.contains(hubPlayer));
    }

    @Test
    void getJoinReceivers_allFlagsFalse_returnsEmpty() {
        when(config.isJoinViewableByJoined()).thenReturn(false);
        when(config.isJoinViewableByOther()).thenReturn(false);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        List<CorePlayer> receivers = resolver.getJoinReceivers("lobby");

        assertTrue(receivers.isEmpty());
    }

    // -----------------------------------------------------------------------
    // getLeaveReceivers
    // -----------------------------------------------------------------------

    @Test
    void getLeaveReceivers_viewableByLeftOnly_returnsOriginServer() {
        when(config.isLeaveViewableByLeft()).thenReturn(true);
        when(config.isLeaveViewableByOther()).thenReturn(false);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        List<CorePlayer> receivers = resolver.getLeaveReceivers("lobby");

        assertEquals(1, receivers.size());
        assertTrue(receivers.contains(lobbyPlayer));
    }

    @Test
    void getLeaveReceivers_viewableByOtherOnly_excludesOriginServer() {
        when(config.isLeaveViewableByLeft()).thenReturn(false);
        when(config.isLeaveViewableByOther()).thenReturn(true);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        List<CorePlayer> receivers = resolver.getLeaveReceivers("lobby");

        assertFalse(receivers.contains(lobbyPlayer));
        assertTrue(receivers.contains(survivalPlayer));
    }

    // -----------------------------------------------------------------------
    // getSwapReceivers
    // -----------------------------------------------------------------------

    @Test
    void getSwapReceivers_allFlagsTrue_returnsAllPlayers() {
        when(config.isSwapViewableByJoined()).thenReturn(true);
        when(config.isSwapViewableByLeft()).thenReturn(true);
        when(config.isSwapViewableByOther()).thenReturn(true);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        List<CorePlayer> receivers = resolver.getSwapReceivers("survival", "lobby");

        assertEquals(3, receivers.size());
    }

    @Test
    void getSwapReceivers_joinedAndLeftOnly_returnsOriginAndDestination() {
        when(config.isSwapViewableByJoined()).thenReturn(true);
        when(config.isSwapViewableByLeft()).thenReturn(true);
        when(config.isSwapViewableByOther()).thenReturn(false);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        List<CorePlayer> receivers = resolver.getSwapReceivers("survival", "lobby");

        assertTrue(receivers.contains(survivalPlayer));
        assertTrue(receivers.contains(lobbyPlayer));
        assertFalse(receivers.contains(hubPlayer));
    }

    @Test
    void getSwapReceivers_othersOnly_excludesBothEndpoints() {
        when(config.isSwapViewableByJoined()).thenReturn(false);
        when(config.isSwapViewableByLeft()).thenReturn(false);
        when(config.isSwapViewableByOther()).thenReturn(true);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        List<CorePlayer> receivers = resolver.getSwapReceivers("survival", "lobby");

        assertFalse(receivers.contains(survivalPlayer));
        assertFalse(receivers.contains(lobbyPlayer));
        assertTrue(receivers.contains(hubPlayer));
    }

    // -----------------------------------------------------------------------
    // getFirstJoinReceivers
    // -----------------------------------------------------------------------

    @Test
    void getFirstJoinReceivers_joinedAndOther_returnsAll() {
        when(config.isFirstJoinViewableByJoined()).thenReturn(true);
        when(config.isFirstJoinViewableByOther()).thenReturn(true);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        List<CorePlayer> receivers = resolver.getFirstJoinReceivers("lobby");

        assertEquals(3, receivers.size());
    }

    // -----------------------------------------------------------------------
    // isBlacklisted(CorePlayer) -- blacklist mode
    // -----------------------------------------------------------------------

    @Test
    void isBlacklisted_blacklistMode_serverIsListed_returnsTrue() {
        when(config.getBlacklistedServers()).thenReturn(List.of("lobby"));
        when(config.isUseBlacklistAsWhitelist()).thenReturn(false);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        assertTrue(resolver.isBlacklisted(lobbyPlayer));
    }

    @Test
    void isBlacklisted_blacklistMode_serverNotListed_returnsFalse() {
        when(config.getBlacklistedServers()).thenReturn(List.of("lobby"));
        when(config.isUseBlacklistAsWhitelist()).thenReturn(false);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        assertFalse(resolver.isBlacklisted(survivalPlayer));
    }

    // -----------------------------------------------------------------------
    // isBlacklisted(CorePlayer) -- whitelist mode
    // -----------------------------------------------------------------------

    @Test
    void isBlacklisted_whitelistMode_serverIsListed_returnsFalse() {
        when(config.getBlacklistedServers()).thenReturn(List.of("lobby"));
        when(config.isUseBlacklistAsWhitelist()).thenReturn(true);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        // In whitelist mode: listed = allowed = NOT blacklisted
        assertFalse(resolver.isBlacklisted(lobbyPlayer));
    }

    @Test
    void isBlacklisted_whitelistMode_serverNotListed_returnsTrue() {
        when(config.getBlacklistedServers()).thenReturn(List.of("lobby"));
        when(config.isUseBlacklistAsWhitelist()).thenReturn(true);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        // survival is NOT listed => not in whitelist => blacklisted
        assertTrue(resolver.isBlacklisted(survivalPlayer));
    }

    // -----------------------------------------------------------------------
    // isBlacklisted(from, to) -- swap requires modes
    // -----------------------------------------------------------------------

    @ParameterizedTest(name = "requires={0}, fromListed={1}, toListed={2}, whitelistMode={3}, expected={4}")
    @CsvSource({
        // BLACKLIST mode
        "ANY,  true,  false, false, true",   // from listed => blocked
        "ANY,  false, true,  false, true",   // to listed => blocked
        "ANY,  false, false, false, false",  // neither listed => not blocked
        "BOTH, true,  false, false, false",  // only from listed, need both => not blocked
        "BOTH, true,  true,  false, true",   // both listed => blocked
        "JOINED, false, true,  false, true", // to (joined) listed => blocked
        "JOINED, true,  false, false, false",// only from listed => not blocked for JOINED
        "LEFT, true,  false, false, true",   // from (left) listed => blocked
        "LEFT, false, true,  false, false",  // only to listed => not blocked for LEFT
        // WHITELIST mode
        "ANY,  false, false, true,  true",   // neither in whitelist => blocked
        "ANY,  true,  true,  true,  false",  // both in whitelist => not blocked (ANY: from||to listed = true; whitelist XOR => false)
    })
    void isBlacklisted_swap_requiresModes(
            String requires, boolean fromListed, boolean toListed,
            boolean whitelistMode, boolean expected) {

        String from = fromListed ? "lobby"    : "hub";
        String to   = toListed   ? "survival" : "other";

        // Configure listed servers: always include lobby and survival in the list
        when(config.getBlacklistedServers()).thenReturn(List.of("lobby", "survival"));
        when(config.isUseBlacklistAsWhitelist()).thenReturn(whitelistMode);
        when(config.getSwapServerMessageRequires()).thenReturn(requires);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        assertEquals(expected, resolver.isBlacklisted(from, to));
    }

    @Test
    void isBlacklisted_swap_unknownRequiresValue_returnsFalseInBlacklistMode() {
        when(config.getBlacklistedServers()).thenReturn(List.of("lobby"));
        when(config.isUseBlacklistAsWhitelist()).thenReturn(false);
        when(config.getSwapServerMessageRequires()).thenReturn("GARBAGE");

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        // result = XOR(false, false) = false... but in whitelist mode it would be true
        // In blacklist mode: isUseBlacklistAsWhitelist=false XOR result=false => false
        assertFalse(resolver.isBlacklisted("lobby", "survival"));
    }

    @Test
    void isBlacklisted_swap_nullServers_neverListed() {
        when(config.getBlacklistedServers()).thenReturn(List.of("lobby"));
        when(config.isUseBlacklistAsWhitelist()).thenReturn(false);
        when(config.getSwapServerMessageRequires()).thenReturn("ANY");

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        assertFalse(resolver.isBlacklisted(null, null));
    }

    // -----------------------------------------------------------------------
    // getServerSuppressedPlayers
    // -----------------------------------------------------------------------

    @Test
    void getServerSuppressedPlayers_joinType_returnsPlayersOnDisabledServers() {
        when(config.getServerJoinMessageDisabled()).thenReturn(List.of("lobby"));

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        Set<UUID> suppressed = resolver.getServerSuppressedPlayers(MessageType.JOIN);

        assertTrue(suppressed.contains(lobbyUuid));
        assertFalse(suppressed.contains(survivalUuid));
    }

    @Test
    void getServerSuppressedPlayers_leaveType_returnsPlayersOnDisabledServers() {
        when(config.getServerLeaveMessageDisabled()).thenReturn(List.of("survival"));

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        Set<UUID> suppressed = resolver.getServerSuppressedPlayers(MessageType.LEAVE);

        assertTrue(suppressed.contains(survivalUuid));
        assertFalse(suppressed.contains(lobbyUuid));
    }

    @Test
    void getServerSuppressedPlayers_firstJoinType_usesFirstJoinList() {
        when(config.getServerFirstJoinMessageDisabled()).thenReturn(List.of("hub"));

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        Set<UUID> suppressed = resolver.getServerSuppressedPlayers(MessageType.FIRST_JOIN);

        assertTrue(suppressed.contains(hubUuid));
        assertFalse(suppressed.contains(lobbyUuid));
    }

    @Test
    void getServerSuppressedPlayers_swapType_alwaysEmpty() {
        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        Set<UUID> suppressed = resolver.getServerSuppressedPlayers(MessageType.SWAP);
        assertTrue(suppressed.isEmpty());
    }

    @Test
    void getServerSuppressedPlayers_unknownServer_isSkipped() {
        when(config.getServerJoinMessageDisabled()).thenReturn(List.of("nonexistent"));
        when(plugin.getServer("nonexistent")).thenReturn(null);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        Set<UUID> suppressed = resolver.getServerSuppressedPlayers(MessageType.JOIN);
        assertTrue(suppressed.isEmpty());
    }

    // -----------------------------------------------------------------------
    // isSilentReceiver
    // -----------------------------------------------------------------------

    @Test
    void isSilentReceiver_adminPermission_returnsTrue() {
        when(config.isNotifyAdminsOnSilentMove()).thenReturn(true);
        when(lobbyPlayer.hasPermission("networkjoinmessages.silent")).thenReturn(true);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        assertTrue(resolver.isSilentReceiver(lobbyPlayer, survivalPlayer));
    }

    @Test
    void isSilentReceiver_adminPermDisabled_returnsFalse() {
        when(config.isNotifyAdminsOnSilentMove()).thenReturn(false);
        when(lobbyPlayer.hasPermission("networkjoinmessages.silent")).thenReturn(true);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        assertFalse(resolver.isSilentReceiver(lobbyPlayer, survivalPlayer));
    }

    @Test
    void isSilentReceiver_sayanVanishEnabled_vanishPermission_returnsTrue() {
        when(config.isNotifyAdminsOnSilentMove()).thenReturn(false);
        when(config.isSVNotifyVanishEnabledPlayersOnSilentMove()).thenReturn(true);
        when(lobbyPlayer.hasPermission("sayanvanish.vanish.use")).thenReturn(true);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, true, false);
        assertTrue(resolver.isSilentReceiver(lobbyPlayer, survivalPlayer));
    }

    @Test
    void isSilentReceiver_pvEnabled_noRespectLevels_pvUsePerm_returnsTrue() {
        when(config.isNotifyAdminsOnSilentMove()).thenReturn(false);
        when(config.isSVNotifyVanishEnabledPlayersOnSilentMove()).thenReturn(false);
        when(config.isPVNotifyVanishEnabledPlayersOnSilentMove()).thenReturn(true);
        when(config.isPVNotifyRespectVanishLevels()).thenReturn(false);
        when(lobbyPlayer.hasPermission("pv.use")).thenReturn(true);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, true);
        assertTrue(resolver.isSilentReceiver(lobbyPlayer, survivalPlayer));
    }

    @Test
    void isSilentReceiver_pvEnabled_respectLevels_sufficientSeeLevel_returnsTrue() {
        when(config.isNotifyAdminsOnSilentMove()).thenReturn(false);
        when(config.isPVNotifyVanishEnabledPlayersOnSilentMove()).thenReturn(true);
        when(config.isPVNotifyRespectVanishLevels()).thenReturn(true);
        // observer see level 5, trigger player use level 5 => observer qualifies
        when(lobbyPlayer.getPremiumVanishSeeLevel()).thenReturn(5);
        when(survivalPlayer.getPremiumVanishUseLevel()).thenReturn(5);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, true);
        assertTrue(resolver.isSilentReceiver(lobbyPlayer, survivalPlayer));
    }

    @Test
    void isSilentReceiver_pvEnabled_respectLevels_insufficientSeeLevel_returnsFalse() {
        when(config.isNotifyAdminsOnSilentMove()).thenReturn(false);
        when(config.isPVNotifyVanishEnabledPlayersOnSilentMove()).thenReturn(true);
        when(config.isPVNotifyRespectVanishLevels()).thenReturn(true);
        // observer see level 3 < trigger use level 5 => does not qualify
        when(lobbyPlayer.getPremiumVanishSeeLevel()).thenReturn(3);
        when(survivalPlayer.getPremiumVanishUseLevel()).thenReturn(5);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, true);
        assertFalse(resolver.isSilentReceiver(lobbyPlayer, survivalPlayer));
    }

    @Test
    void isSilentReceiver_pvEnabled_respectLevels_triggerPlayerUseLevel0_returnsFalse() {
        when(config.isNotifyAdminsOnSilentMove()).thenReturn(false);
        when(config.isPVNotifyVanishEnabledPlayersOnSilentMove()).thenReturn(true);
        when(config.isPVNotifyRespectVanishLevels()).thenReturn(true);
        // trigger player has no PV permissions, use level defaults to 0
        when(lobbyPlayer.getPremiumVanishSeeLevel()).thenReturn(0);
        when(survivalPlayer.getPremiumVanishUseLevel()).thenReturn(0);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, true);
        assertFalse(resolver.isSilentReceiver(lobbyPlayer, survivalPlayer));
    }

    @Test
    void isSilentReceiver_noConditionsMet_returnsFalse() {
        when(config.isNotifyAdminsOnSilentMove()).thenReturn(false);
        when(config.isSVNotifyVanishEnabledPlayersOnSilentMove()).thenReturn(false);
        when(config.isPVNotifyVanishEnabledPlayersOnSilentMove()).thenReturn(false);

        ReceiverResolver resolver = new ReceiverResolver(plugin, config, false, false);
        assertFalse(resolver.isSilentReceiver(lobbyPlayer, survivalPlayer));
    }
}
