package xyz.earthcow.networkjoinmessages.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserCacheImporterTest {
    @TempDir
    private Path tempDir;
    private H2PlayerJoinTracker tracker;

    @BeforeEach
    void setUp() throws Exception {
        CoreLogger logger = Mockito.mock(CoreLogger.class);
        tracker = new H2PlayerJoinTracker(logger, tempDir.resolve("joined").toAbsolutePath().toString());
    }

    @Test
    void addUsersFromUserCache_Success() throws Exception {
        // Arrange
        Path testCache = tempDir.resolve("usercache.json");
        String json = """
            [
                {"name": "Notch", "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5"},
                {"name": "jeb_", "uuid": "853c80ef-3c37-49fd-aa49-938b674adae6"}
            ]
            """;
        Files.writeString(testCache, json);

        UUID randomUUID = UUID.randomUUID();

        tracker.markAsJoined(randomUUID, "Player 1");

        boolean result = tracker.addUsersFromUserCache(testCache.toString());

        assertTrue(result);
        assertTrue(tracker.hasJoined(randomUUID));
        assertTrue(tracker.hasJoined(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5")));
        assertTrue(tracker.hasJoined(UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6")));
    }

    @Test
    void addUsersFromUserCache_FileNotFound() {
        assertFalse(tracker.addUsersFromUserCache("/nonexistent/path"));
    }

    @Test
    void addUsersFromUserCache_InvalidJson() throws Exception {
        Path testCache = tempDir.resolve("usercache.json");
        Files.writeString(testCache, "INVALID_JSON");

        assertFalse(tracker.addUsersFromUserCache(testCache.toString()));
    }

    @Test
    void addUsersFromUserCache_EmptyArray() throws Exception {
        Path testCache = tempDir.resolve("usercache.json");
        Files.writeString(testCache, "[]");

        boolean result = tracker.addUsersFromUserCache(testCache.toString());

        assertTrue(result); // Should still be successful but no users added
    }
}
