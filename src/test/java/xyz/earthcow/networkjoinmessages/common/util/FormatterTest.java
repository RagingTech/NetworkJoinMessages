package xyz.earthcow.networkjoinmessages.common.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import xyz.earthcow.networkjoinmessages.common.Storage;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FormatterTest {

    // Unable to mock PlaceholderAPI.class so it is exempt from tests

    @Mock
    private CorePlugin mockPlugin;
    @Mock
    private Storage mockStorage;
    @Mock
    private CoreLogger mockLogger;
    @Mock
    private CorePlayer mockPlayer;
    @Mock
    private CoreBackendServer mockServer;
    @Mock
    private LuckPerms mockLuckPerms;
    @Mock
    private UserManager mockUserManager;
    @Mock
    private User mockUser;
    @Mock
    private CachedDataManager mockCachedUserData;
    @Mock
    private CachedMetaData mockMetaData;

    private final UUID testUUID = UUID.randomUUID();

    // Helper function for test clarity and duplicate code reduction
    void setupMockPlayer() {
        when(mockPlayer.getName()).thenReturn("TestPlayer");
        when(mockPlayer.getUniqueId()).thenReturn(testUUID);
        when(mockPlayer.getCurrentServer()).thenReturn(mockServer);
        when(mockServer.getName()).thenReturn("TestServer");
        when(mockStorage.getServerDisplayName("TestServer")).thenReturn("Test Server Display");
    }

    @Test
    void testFormatterConstructor_WithLuckPerms() {
        try (MockedStatic<LuckPermsProvider> mockedProvider = mockStatic(LuckPermsProvider.class)) {
            mockedProvider.when(LuckPermsProvider::get).thenReturn(mockLuckPerms);
            when(mockPlugin.isPluginLoaded("LuckPerms")).thenReturn(true);
            when(mockPlugin.getCoreLogger()).thenReturn(mockLogger);

            new Formatter(mockPlugin, mockStorage);

            verify(mockLogger).info("Successfully hooked into LuckPerms!");
        }
    }

    @Test
    void testFormatterConstructor_WithoutLuckPermsThrow() {
        try (MockedStatic<LuckPermsProvider> mockedProvider = mockStatic(LuckPermsProvider.class)) {
            when(mockPlugin.isPluginLoaded("LuckPerms")).thenReturn(true);
            mockedProvider.when(LuckPermsProvider::get).thenThrow(new IllegalStateException("LuckPerms not found"));
            when(mockPlugin.getCoreLogger()).thenReturn(mockLogger);

            new Formatter(mockPlugin, mockStorage);

            verify(mockLogger).warn("Could not find LuckPerms. Corresponding placeholders will be unavailable.");
        }
    }

    @Test
    void testFormatterConstructor_WithoutLuckPermsNoThrow() {
        try (MockedStatic<LuckPermsProvider> mockedProvider = mockStatic(LuckPermsProvider.class)) {
            when(mockPlugin.isPluginLoaded("LuckPerms")).thenReturn(false);
            mockedProvider.when(LuckPermsProvider::get).thenReturn(mockLuckPerms);
            when(mockPlugin.getCoreLogger()).thenReturn(mockLogger);

            new Formatter(mockPlugin, mockStorage);

            verify(mockLogger).warn("Could not find LuckPerms. Corresponding placeholders will be unavailable.");
        }
    }

    @Test
    void testFormatterConstructor_WithMiniPlaceholders() {
        when(mockPlugin.isPluginLoaded("MiniPlaceholders")).thenReturn(true);
        when(mockPlugin.getCoreLogger()).thenReturn(mockLogger);

        new Formatter(mockPlugin, mockStorage);

        verify(mockLogger).info("Successfully hooked into MiniPlaceholders!");
    }

    @Test
    void testDeserialize_SimpleString() {
        String input = "Hello World";
        Component result = Formatter.deserialize(input);

        assertNotNull(result);
        String plainText = PlainTextComponentSerializer.plainText().serialize(result);
        assertEquals("Hello World", plainText);
    }

    @Test
    void testDeserialize_WithLegacyColorCodes() {
        String input = "&cRed Text &aGreen Text";
        Component result = Formatter.deserialize(input);

        assertNotNull(result);
        String plainText = PlainTextComponentSerializer.plainText().serialize(result);
        assertEquals("Red Text Green Text", plainText);
    }

    @Test
    void testDeserialize_WithHexColors() {
        String input = "&#FF0000Red Text";
        Component result = Formatter.deserialize(input);

        assertNotNull(result);
        String plainText = PlainTextComponentSerializer.plainText().serialize(result);
        assertEquals("Red Text", plainText);
    }

    @Test
    void testDeserialize_WithEssentialsColorCodes() {
        String input = "§x§f§b§6§3§f§5Hello!";
        Component result = Formatter.deserialize(input);

        assertNotNull(result);
        String plainText = PlainTextComponentSerializer.plainText().serialize(result);
        assertEquals("Hello!", plainText);
    }

    @Test
    void testSerialize() {
        Component component = Component.text("Hello World").color(NamedTextColor.RED);
        String result = Formatter.serialize(component);

        assertNotNull(result);
        assertNotEquals("Hello World", result);
        assertTrue(result.contains("Hello World"));
    }

    @Test
    void testSanitize_String() {
        String input = "<red>Hello World</red>";
        String result = Formatter.sanitize(input);

        assertEquals("Hello World", result);
    }

    @Test
    void testSanitize_Component() {
        Component component = Component.text("Hello World").color(NamedTextColor.RED);
        String result = Formatter.sanitize(component);

        assertEquals("Hello World", result);
    }

    @Test
    void testParsePlaceholdersAndThen_BasicPlaceholders() {
        setupMockPlayer();

        String message = "Hello %player% on %server_name%!";

        when(mockPlugin.getCoreLogger()).thenReturn(mockLogger);
        (new Formatter(mockPlugin, mockStorage)).parsePlaceholdersAndThen(message, mockPlayer, result -> {
            assertEquals("Hello TestPlayer on Test Server Display!", result);
        });
    }

    @Test
    void testParsePlaceholdersAndThen_WithLuckPerms() {
        // Setup LuckPerms mocks
        try (MockedStatic<LuckPermsProvider> mockedProvider = mockStatic(LuckPermsProvider.class)) {
            when(mockPlugin.isPluginLoaded("LuckPerms")).thenReturn(true);
            mockedProvider.when(LuckPermsProvider::get).thenReturn(mockLuckPerms);

            when(mockLuckPerms.getUserManager()).thenReturn(mockUserManager);
            when(mockUserManager.getUser(testUUID)).thenReturn(mockUser);
            when(mockUser.getCachedData()).thenReturn(mockCachedUserData);
            when(mockCachedUserData.getMetaData()).thenReturn(mockMetaData);
            when(mockMetaData.getPrefix()).thenReturn("[VIP]");
            when(mockMetaData.getSuffix()).thenReturn("[Donor]");

            setupMockPlayer();

            when(mockPlugin.getCoreLogger()).thenReturn(mockLogger);

            Formatter testFormatter = new Formatter(mockPlugin, mockStorage);
            String message = "%player_prefix% %player% %player_suffix%";

            testFormatter.parsePlaceholdersAndThen(message, mockPlayer, result -> {
                assertEquals("[VIP] TestPlayer [Donor]", result);
            });
        }
    }

    @Test
    void testParsePlaceholdersAndThen_WithLuckPermsNullUser() {
        try (MockedStatic<LuckPermsProvider> mockedProvider = mockStatic(LuckPermsProvider.class)) {
            when(mockPlugin.isPluginLoaded("LuckPerms")).thenReturn(true);
            mockedProvider.when(LuckPermsProvider::get).thenReturn(mockLuckPerms);

            when(mockLuckPerms.getUserManager()).thenReturn(mockUserManager);
            when(mockUserManager.getUser(testUUID)).thenReturn(null);

            when(mockPlugin.getCoreLogger()).thenReturn(mockLogger);

            setupMockPlayer();

            Formatter testFormatter = new Formatter(mockPlugin, mockStorage);
            String message = "%player_prefix% %player% %player_suffix%";

            testFormatter.parsePlaceholdersAndThen(message, mockPlayer, result -> {
                // Should replace with empty strings when user is null
                assertEquals(" TestPlayer ", result);
            });
        }
    }

    @Test
    void testParsePlaceholdersAndThen_WithLuckPermsNullPrefixSuffix() {
        try (MockedStatic<LuckPermsProvider> mockedProvider = mockStatic(LuckPermsProvider.class)) {
            when(mockPlugin.isPluginLoaded("LuckPerms")).thenReturn(true);
            mockedProvider.when(LuckPermsProvider::get).thenReturn(mockLuckPerms);

            when(mockLuckPerms.getUserManager()).thenReturn(mockUserManager);
            when(mockUserManager.getUser(testUUID)).thenReturn(mockUser);
            when(mockUser.getCachedData()).thenReturn(mockCachedUserData);
            when(mockCachedUserData.getMetaData()).thenReturn(mockMetaData);
            when(mockMetaData.getPrefix()).thenReturn(null);
            when(mockMetaData.getSuffix()).thenReturn(null);

            when(mockPlugin.getCoreLogger()).thenReturn(mockLogger);

            setupMockPlayer();

            Formatter testFormatter = new Formatter(mockPlugin, mockStorage);
            String message = "%player_prefix% %player% %player_suffix%";

            testFormatter.parsePlaceholdersAndThen(message, mockPlayer, result -> {
                // Should replace with empty strings when prefix/suffix are null
                assertEquals(" TestPlayer ", result);
            });
        }
    }

    @Test
    void testParsePlaceholdersAndThen_AllPlaceholders() {
        setupMockPlayer();

        String message = "Player: %player%, Display: %displayname%, Server: %server_name%, Clean: %server_name_clean%";

        when(mockPlugin.getCoreLogger()).thenReturn(mockLogger);

        (new Formatter(mockPlugin, mockStorage)).parsePlaceholdersAndThen(message, mockPlayer, result -> {
            assertEquals("Player: TestPlayer, Display: TestPlayer, Server: Test Server Display, Clean: TestServer", result);
        });
    }

    @Test
    void testEssentialsPattern() {
        String input = "§x§f§b§6§3§f§5Hello!";
        assertTrue(Formatter.essentialsPattern.matcher(input).find());

        String nonMatch = "Regular text";
        assertFalse(Formatter.essentialsPattern.matcher(nonMatch).find());
    }

    @Test
    void testDeserialize_EmptyString() {
        Component result = Formatter.deserialize("");
        assertNotNull(result);
        String plainText = PlainTextComponentSerializer.plainText().serialize(result);
        assertEquals("", plainText);
    }

    @Test
    void testFormatterConstructor_HandlesExceptions() {
        // Test that constructor handles various exceptions gracefully
        when(mockPlugin.getCoreLogger()).thenReturn(mockLogger);

        assertDoesNotThrow(() -> {
            new Formatter(mockPlugin, mockStorage);
        });
    }
}