package xyz.earthcow.networkjoinmessages.common.broadcast;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.abstraction.PremiumVanish;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;
import xyz.earthcow.networkjoinmessages.common.modules.SayanVanishHook;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageFormatterTest {

    @Mock private CorePlugin   plugin;
    @Mock private PluginConfig config;
    @Mock private CorePlayer   player;
    @Mock private CoreBackendServer server;
    @Mock private SayanVanishHook sayanVanish;
    @Mock private PremiumVanish   premiumVanish;

    private final UUID playerUuid = UUID.randomUUID();

    // A second online player used for counting tests
    @Mock private CorePlayer otherPlayer;
    private final UUID otherUuid = UUID.randomUUID();

    @BeforeEach
    void setup() {
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(player.getCurrentServer()).thenReturn(server);
        when(server.getName()).thenReturn("lobby");
        when(server.getPlayersConnected()).thenReturn(List.of(player));

        when(otherPlayer.getUniqueId()).thenReturn(otherUuid);

        when(plugin.getServer("lobby")).thenReturn(server);
        when(plugin.getAllPlayers()).thenReturn(List.of(player));

        // Disable vanish integrations by default
        when(config.isPVRemoveVanishedPlayersFromPlayerCount()).thenReturn(false);
        when(config.isSVRemoveVanishedPlayersFromPlayerCount()).thenReturn(false);
        when(plugin.getVanishAPI()).thenReturn(null);
    }

    // -----------------------------------------------------------------------
    // formatJoinMessage
    // -----------------------------------------------------------------------

    @Test
    void formatJoinMessage_noPlaceholders_returnsRaw() {
        when(config.getJoinNetworkMessage()).thenReturn("Welcome!");
        MessageFormatter fmt = new MessageFormatter(plugin, config, null);
        assertEquals("Welcome!", fmt.formatJoinMessage(player));
    }

    @Test
    void formatJoinMessage_playerCountServer_playerAlreadyPresent_joining() {
        // Player is in the server list. Joining means count is used as-is (player IS already counted).
        when(config.getJoinNetworkMessage()).thenReturn("%playercount_server% players online");
        when(server.getPlayersConnected()).thenReturn(List.of(player));

        MessageFormatter fmt = new MessageFormatter(plugin, config, null);
        // 1 player on server, not leaving => count stays 1
        assertEquals("1 players online", fmt.formatJoinMessage(player));
    }

    @Test
    void formatJoinMessage_playerCountNetwork() {
        when(config.getJoinNetworkMessage()).thenReturn("%playercount_network% online");
        when(plugin.getAllPlayers()).thenReturn(List.of(player));

        MessageFormatter fmt = new MessageFormatter(plugin, config, null);
        assertEquals("1 online", fmt.formatJoinMessage(player));
    }

    @Test
    void formatJoinMessage_playerNotYetInList_countIsIncrementedByOne() {
        // Simulates the state just before the player is added to the server list
        when(config.getJoinNetworkMessage()).thenReturn("%playercount_server% players");
        when(server.getPlayersConnected()).thenReturn(Collections.emptyList()); // player not yet counted
        when(plugin.getServer("lobby")).thenReturn(server);

        MessageFormatter fmt = new MessageFormatter(plugin, config, null);
        // Player absent + !leaving => count should be 0+1 = 1
        assertEquals("1 players", fmt.formatJoinMessage(player));
    }

    // -----------------------------------------------------------------------
    // formatLeaveMessage
    // -----------------------------------------------------------------------

    @Test
    void formatLeaveMessage_playerCountServer_leavingReducesCountByOne() {
        when(config.getLeaveNetworkMessage()).thenReturn("%playercount_server% remain");
        when(server.getPlayersConnected()).thenReturn(List.of(player)); // still in list at leave time

        MessageFormatter fmt = new MessageFormatter(plugin, config, null);
        // Player present + leaving => 1-1 = 0
        assertEquals("0 remain", fmt.formatLeaveMessage(player));
    }

    @Test
    void formatLeaveMessage_noPlaceholders_returnsRaw() {
        when(config.getLeaveNetworkMessage()).thenReturn("Goodbye!");
        MessageFormatter fmt = new MessageFormatter(plugin, config, null);
        assertEquals("Goodbye!", fmt.formatLeaveMessage(player));
    }

    // -----------------------------------------------------------------------
    // formatFirstJoinMessage
    // -----------------------------------------------------------------------

    @Test
    void formatFirstJoinMessage_playerCountServer_delegatesToJoinLogic() {
        when(config.getFirstJoinNetworkMessage()).thenReturn("%playercount_server% players");
        when(server.getPlayersConnected()).thenReturn(Collections.emptyList());

        MessageFormatter fmt = new MessageFormatter(plugin, config, null);
        // Player absent, joining => 0+1 = 1
        assertEquals("1 players", fmt.formatFirstJoinMessage(player));
    }

    // -----------------------------------------------------------------------
    // formatSwapMessage
    // -----------------------------------------------------------------------

    @Test
    void formatSwapMessage_serverNamePlaceholders() {
        when(config.getSwapServerMessage()).thenReturn("%from% -> %to%");
        when(config.getServerDisplayName("lobby")).thenReturn("Lobby");
        when(config.getServerDisplayName("survival")).thenReturn("Survival");

        MessageFormatter fmt = new MessageFormatter(plugin, config, null);
        assertEquals("Lobby -> Survival", fmt.formatSwapMessage(player, "lobby", "survival"));
    }

    @Test
    void formatSwapMessage_cleanServerNamePlaceholders() {
        when(config.getSwapServerMessage()).thenReturn("%from_clean% to %to_clean%");
        when(config.getServerDisplayName(anyString())).thenAnswer(inv -> inv.getArgument(0));

        MessageFormatter fmt = new MessageFormatter(plugin, config, null);
        assertEquals("lobby to survival", fmt.formatSwapMessage(player, "lobby", "survival"));
    }

    @Test
    void formatSwapMessage_playerCountFromPlaceholder() {
        when(config.getSwapServerMessage()).thenReturn("%playercount_from% leaving");
        when(config.getServerDisplayName(anyString())).thenAnswer(inv -> inv.getArgument(0));

        CoreBackendServer fromServer = mock(CoreBackendServer.class);
        when(fromServer.getPlayersConnected()).thenReturn(List.of(player));
        when(plugin.getServer("lobby")).thenReturn(fromServer);

        MessageFormatter fmt = new MessageFormatter(plugin, config, null);
        // player IS in fromServer list, leaving => 1-1 = 0
        assertEquals("0 leaving", fmt.formatSwapMessage(player, "lobby", "survival"));
    }

    @Test
    void formatSwapMessage_playerCountToPlaceholder() {
        when(config.getSwapServerMessage()).thenReturn("%playercount_to% in dest");
        when(config.getServerDisplayName(anyString())).thenAnswer(inv -> inv.getArgument(0));

        CoreBackendServer toServer = mock(CoreBackendServer.class);
        when(toServer.getPlayersConnected()).thenReturn(Collections.emptyList());
        when(plugin.getServer("survival")).thenReturn(toServer);

        MessageFormatter fmt = new MessageFormatter(plugin, config, null);
        // player NOT in toServer, not leaving => 0+1 = 1
        assertEquals("1 in dest", fmt.formatSwapMessage(player, "lobby", "survival"));
    }

    @Test
    void formatSwapMessage_networkCountPlaceholder() {
        when(config.getSwapServerMessage()).thenReturn("%playercount_network% total");
        when(config.getServerDisplayName(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(plugin.getAllPlayers()).thenReturn(List.of(player, otherPlayer));

        MessageFormatter fmt = new MessageFormatter(plugin, config, null);
        // 2 players, player is present and NOT leaving => count = 2
        assertEquals("2 total", fmt.formatSwapMessage(player, "lobby", "survival"));
    }

    // -----------------------------------------------------------------------
    // computePlayerCount -- vanish integration
    // -----------------------------------------------------------------------

    @Test
    void playerCount_vanishedPlayerIsExcludedWhenPVEnabled() {
        when(config.getJoinNetworkMessage()).thenReturn("%playercount_network% online");
        when(config.isPVRemoveVanishedPlayersFromPlayerCount()).thenReturn(true);
        when(plugin.getVanishAPI()).thenReturn(premiumVanish);
        // Both players online, but otherPlayer is vanished
        when(plugin.getAllPlayers()).thenReturn(List.of(player, otherPlayer));
        when(premiumVanish.getInvisiblePlayers()).thenReturn(List.of(otherUuid));

        MessageFormatter fmt = new MessageFormatter(plugin, config, null);
        // Only player is visible; player not present in server list, joining => 0+1 = 1
        assertEquals("1 online", fmt.formatJoinMessage(player));
    }

    @Test
    void playerCount_vanishedPlayerIsExcludedWhenSVEnabled() {
        when(config.getJoinNetworkMessage()).thenReturn("%playercount_network% online");
        when(config.isSVRemoveVanishedPlayersFromPlayerCount()).thenReturn(true);
        when(plugin.getAllPlayers()).thenReturn(List.of(player, otherPlayer));
        when(sayanVanish.getVanishedPlayers()).thenReturn(List.of(otherUuid));

        MessageFormatter fmt = new MessageFormatter(plugin, config, sayanVanish);
        // player joining (absent from list), other is vanished
        when(plugin.getAllPlayers()).thenReturn(List.of(otherPlayer));
        // only otherPlayer in list, otherPlayer is vanished => visible = 0; subject absent, joining => 1
        assertEquals("1 online", fmt.formatJoinMessage(player));
    }

    @Test
    void playerCount_vanishedSubjectIsNotCountedForOthers() {
        when(config.getJoinNetworkMessage()).thenReturn("%playercount_network% online");
        when(config.isPVRemoveVanishedPlayersFromPlayerCount()).thenReturn(true);
        when(plugin.getVanishAPI()).thenReturn(premiumVanish);
        when(plugin.getAllPlayers()).thenReturn(List.of(player));
        // Subject (player) is vanished
        when(premiumVanish.getInvisiblePlayers()).thenReturn(List.of(playerUuid));

        MessageFormatter fmt = new MessageFormatter(plugin, config, null);
        // player is vanished => visible list is empty, and subject is vanished so no +1 => 0
        assertEquals("0 online", fmt.formatJoinMessage(player));
    }

    // -----------------------------------------------------------------------
    // getServerPlayerCount -- null server
    // -----------------------------------------------------------------------

    @Test
    void getServerPlayerCount_nullServer_leavingReturnsZero() {
        MessageFormatter fmt = new MessageFormatter(plugin, config, null);
        String result = fmt.getServerPlayerCount(
                (CoreBackendServer) null, true, player);
        assertEquals("0", result);
    }

    @Test
    void getServerPlayerCount_nullServer_joiningReturnsOne() {
        MessageFormatter fmt = new MessageFormatter(plugin, config, null);
        String result = fmt.getServerPlayerCount(
                (CoreBackendServer) null, false, player);
        assertEquals("1", result);
    }

    // -----------------------------------------------------------------------
    // prepareDiscordJoinLeaveTemplate
    // -----------------------------------------------------------------------

    @Test
    void prepareDiscordJoinLeaveTemplate_replacesAvatarUrl() {
        when(config.isPVRemoveVanishedPlayersFromPlayerCount()).thenReturn(false);
        when(config.isSVRemoveVanishedPlayersFromPlayerCount()).thenReturn(false);
        when(plugin.getVanishAPI()).thenReturn(null);

        MessageFormatter fmt = new MessageFormatter(plugin, config, null);
        String result = fmt.prepareDiscordJoinLeaveTemplate(
                "Avatar: %embedavatarurl%", player, false, "https://example.com/avatar.png");
        assertEquals("Avatar: https://example.com/avatar.png", result);
    }

    @Test
    void prepareDiscordJoinLeaveTemplate_replacesPlayerCountServer() {
        when(config.isPVRemoveVanishedPlayersFromPlayerCount()).thenReturn(false);
        when(config.isSVRemoveVanishedPlayersFromPlayerCount()).thenReturn(false);
        when(plugin.getVanishAPI()).thenReturn(null);
        when(server.getPlayersConnected()).thenReturn(List.of(player));

        MessageFormatter fmt = new MessageFormatter(plugin, config, null);
        String result = fmt.prepareDiscordJoinLeaveTemplate(
                "%playercount_server% online", player, false, "");
        // player present, joining (not leaving): 1
        assertEquals("1 online", result);
    }

    // -----------------------------------------------------------------------
    // prepareDiscordSwapTemplate
    // -----------------------------------------------------------------------

    @Test
    void prepareDiscordSwapTemplate_replacesAllServerPlaceholders() {
        when(config.getServerDisplayName("lobby")).thenReturn("Lobby");
        when(config.getServerDisplayName("survival")).thenReturn("Survival");
        when(config.isPVRemoveVanishedPlayersFromPlayerCount()).thenReturn(false);
        when(config.isSVRemoveVanishedPlayersFromPlayerCount()).thenReturn(false);
        when(plugin.getVanishAPI()).thenReturn(null);

        MessageFormatter fmt = new MessageFormatter(plugin, config, null);
        String result = fmt.prepareDiscordSwapTemplate(
                "%from% -> %to% (%from_clean% to %to_clean%) %embedavatarurl%",
                player, "lobby", "survival", "http://img");

        assertEquals("Lobby -> Survival (lobby to survival) http://img", result);
    }
}
