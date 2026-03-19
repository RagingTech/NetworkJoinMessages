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
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreBackendServer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;
import xyz.earthcow.networkjoinmessages.common.config.PluginConfig;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FormatterTest {

    // PlaceholderAPI cannot be mocked, so PAPI-dependent paths are excluded from tests.

    @Mock private CorePlugin mockPlugin;
    @Mock private PluginConfig mockConfig;
    @Mock private CoreLogger mockLogger;
    @Mock private CorePlayer mockPlayer;
    @Mock private CoreBackendServer mockServer;
    @Mock private LuckPerms mockLuckPerms;
    @Mock private UserManager mockUserManager;
    @Mock private User mockUser;
    @Mock private CachedDataManager mockCachedUserData;
    @Mock private CachedMetaData mockMetaData;

    private final UUID testUUID = UUID.randomUUID();

    private void setupMockPlayer() {
        when(mockPlayer.getName()).thenReturn("TestPlayer");
        when(mockPlayer.getUniqueId()).thenReturn(testUUID);
        when(mockPlayer.getCurrentServer()).thenReturn(mockServer);
        when(mockServer.getName()).thenReturn("TestServer");
        when(mockConfig.getServerDisplayName("TestServer")).thenReturn("Test Server Display");
    }

    private PlaceholderResolver buildResolver() {
        when(mockPlugin.getCoreLogger()).thenReturn(mockLogger);
        return new PlaceholderResolver(mockPlugin, mockConfig);
    }

    // --- PlaceholderResolver construction ---

    @Test
    void testResolver_WithLuckPerms() {
        try (MockedStatic<LuckPermsProvider> mockedProvider = mockStatic(LuckPermsProvider.class)) {
            mockedProvider.when(LuckPermsProvider::get).thenReturn(mockLuckPerms);
            when(mockPlugin.isPluginLoaded("LuckPerms")).thenReturn(true);
            when(mockPlugin.getCoreLogger()).thenReturn(mockLogger);

            new PlaceholderResolver(mockPlugin, mockConfig);

            verify(mockLogger).info("Successfully hooked into LuckPerms!");
        }
    }

    @Test
    void testResolver_LuckPermsThrows() {
        try (MockedStatic<LuckPermsProvider> mockedProvider = mockStatic(LuckPermsProvider.class)) {
            when(mockPlugin.isPluginLoaded("LuckPerms")).thenReturn(true);
            mockedProvider.when(LuckPermsProvider::get).thenThrow(new IllegalStateException("not loaded"));
            when(mockPlugin.getCoreLogger()).thenReturn(mockLogger);

            new PlaceholderResolver(mockPlugin, mockConfig);

            verify(mockLogger).warn("Could not find LuckPerms. Corresponding placeholders will be unavailable.");
        }
    }

    @Test
    void testResolver_LuckPermsNotPresent() {
        try (MockedStatic<LuckPermsProvider> mockedProvider = mockStatic(LuckPermsProvider.class)) {
            when(mockPlugin.isPluginLoaded("LuckPerms")).thenReturn(false);
            mockedProvider.when(LuckPermsProvider::get).thenReturn(mockLuckPerms);
            when(mockPlugin.getCoreLogger()).thenReturn(mockLogger);

            new PlaceholderResolver(mockPlugin, mockConfig);

            verify(mockLogger).warn("Could not find LuckPerms. Corresponding placeholders will be unavailable.");
        }
    }

    @Test
    void testResolver_MiniPlaceholders() {
        when(mockPlugin.isPluginLoaded("MiniPlaceholders")).thenReturn(true);
        when(mockPlugin.getCoreLogger()).thenReturn(mockLogger);

        new PlaceholderResolver(mockPlugin, mockConfig);

        verify(mockLogger).info("Successfully hooked into MiniPlaceholders!");
    }

    // --- Formatter (static) ---

    @Test
    void testDeserialize_SimpleString() {
        Component result = Formatter.deserialize("Hello World");
        assertNotNull(result);
        assertEquals("Hello World", PlainTextComponentSerializer.plainText().serialize(result));
    }

    @Test
    void testDeserialize_WithLegacyColorCodes() {
        Component result = Formatter.deserialize("&cRed Text &aGreen Text");
        assertEquals("Red Text Green Text", PlainTextComponentSerializer.plainText().serialize(result));
    }

    @Test
    void testDeserialize_WithHexColors() {
        Component result = Formatter.deserialize("&#FF0000Red Text");
        assertEquals("Red Text", PlainTextComponentSerializer.plainText().serialize(result));
    }

    @Test
    void testDeserialize_WithEssentialsHex() {
        Component result = Formatter.deserialize("§x§f§b§6§3§f§5Hello!");
        assertEquals("Hello!", PlainTextComponentSerializer.plainText().serialize(result));
    }

    @Test
    void testDeserialize_EmptyString() {
        Component result = Formatter.deserialize("");
        assertNotNull(result);
        assertEquals("", PlainTextComponentSerializer.plainText().serialize(result));
    }

    @Test
    void testSerialize() {
        Component component = Component.text("Hello World").color(NamedTextColor.RED);
        String result = Formatter.serialize(component);
        assertNotNull(result);
        assertTrue(result.contains("Hello World"));
        assertNotEquals("Hello World", result); // should have formatting tags
    }

    @Test
    void testSanitize_String() {
        assertEquals("Hello World", Formatter.sanitize("<red>Hello World</red>"));
    }

    @Test
    void testSanitize_Component() {
        assertEquals("Hello World", Formatter.sanitize(Component.text("Hello World").color(NamedTextColor.RED)));
    }

    // --- LegacyColorTranslator ---

    @Test
    void testLegacyTranslator_EssentialsPattern() {
        String input = "§x§f§b§6§3§f§5Hello!";
        assertTrue(LegacyColorTranslator.ESSENTIALS_HEX_PATTERN.matcher(input).find());
        assertFalse(LegacyColorTranslator.ESSENTIALS_HEX_PATTERN.matcher("Regular text").find());
    }

    @Test
    void testLegacyTranslator_AllCodeTypesDeserializeCleanly() {
        // All these should round-trip through deserialize without throwing
        assertDoesNotThrow(() -> Formatter.deserialize("&cRed &aGreen &bAqua &#FF0000Hex §x§f§f§0§0§0§0EssHex"));
    }

    // --- PlaceholderResolver.resolve ---

    @Test
    void testResolve_BuiltinPlaceholders() {
        setupMockPlayer();
        PlaceholderResolver resolver = buildResolver();

        AtomicReference<String> result = new AtomicReference<>();
        resolver.resolve("Hello %player% on %server_name%!", mockPlayer, result::set);

        assertEquals("Hello TestPlayer on Test Server Display!", result.get());
    }

    @Test
    void testResolve_AllBuiltins() {
        setupMockPlayer();
        PlaceholderResolver resolver = buildResolver();

        AtomicReference<String> result = new AtomicReference<>();
        resolver.resolve(
            "Player: %player%, Display: %displayname%, Server: %server_name%, Clean: %server_name_clean%",
            mockPlayer, result::set
        );

        assertEquals(
            "Player: TestPlayer, Display: TestPlayer, Server: Test Server Display, Clean: TestServer",
            result.get()
        );
    }

    @Test
    void testResolve_WithLuckPerms() {
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
            PlaceholderResolver resolver = buildResolver();

            AtomicReference<String> result = new AtomicReference<>();
            resolver.resolve("%player_prefix% %player% %player_suffix%", mockPlayer, result::set);

            assertEquals("[VIP] TestPlayer [Donor]", result.get());
        }
    }

    @Test
    void testResolve_LuckPermsNullUser() {
        try (MockedStatic<LuckPermsProvider> mockedProvider = mockStatic(LuckPermsProvider.class)) {
            when(mockPlugin.isPluginLoaded("LuckPerms")).thenReturn(true);
            mockedProvider.when(LuckPermsProvider::get).thenReturn(mockLuckPerms);
            when(mockLuckPerms.getUserManager()).thenReturn(mockUserManager);
            when(mockUserManager.getUser(testUUID)).thenReturn(null);

            setupMockPlayer();
            PlaceholderResolver resolver = buildResolver();

            AtomicReference<String> result = new AtomicReference<>();
            resolver.resolve("%player_prefix% %player% %player_suffix%", mockPlayer, result::set);

            assertEquals(" TestPlayer ", result.get());
        }
    }

    @Test
    void testResolve_LuckPermsNullPrefixSuffix() {
        try (MockedStatic<LuckPermsProvider> mockedProvider = mockStatic(LuckPermsProvider.class)) {
            when(mockPlugin.isPluginLoaded("LuckPerms")).thenReturn(true);
            mockedProvider.when(LuckPermsProvider::get).thenReturn(mockLuckPerms);
            when(mockLuckPerms.getUserManager()).thenReturn(mockUserManager);
            when(mockUserManager.getUser(testUUID)).thenReturn(mockUser);
            when(mockUser.getCachedData()).thenReturn(mockCachedUserData);
            when(mockCachedUserData.getMetaData()).thenReturn(mockMetaData);
            when(mockMetaData.getPrefix()).thenReturn(null);
            when(mockMetaData.getSuffix()).thenReturn(null);

            setupMockPlayer();
            PlaceholderResolver resolver = buildResolver();

            AtomicReference<String> result = new AtomicReference<>();
            resolver.resolve("%player_prefix% %player% %player_suffix%", mockPlayer, result::set);

            assertEquals(" TestPlayer ", result.get());
        }
    }

    @Test
    void testResolver_ConstructorDoesNotThrow() {
        when(mockPlugin.getCoreLogger()).thenReturn(mockLogger);
        assertDoesNotThrow(() -> new PlaceholderResolver(mockPlugin, mockConfig));
    }
}
