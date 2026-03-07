package xyz.earthcow.networkjoinmessages.common.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.storage.H2PlayerJoinTracker;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class H2PlayerJoinTrackerTest {

    @TempDir
    private Path tempDir;

    private File tempDbFile;
    private H2PlayerJoinTracker tracker;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = tempDir.resolve("joined").toFile();
        CoreLogger logger = Mockito.mock(CoreLogger.class);
        tracker = new H2PlayerJoinTracker(logger, tempDbFile.getAbsolutePath());
    }

    @AfterEach
    void tearDown() throws Exception {
        tracker.close();
        if (tempDbFile.exists()) tempDbFile.delete();
    }

    @Test
    void testNewUUIDHasNotJoined() {
        UUID uuid = UUID.randomUUID();
        assertFalse(tracker.hasJoined(uuid), "New UUID should not be marked as joined");
    }

    @Test
    void testMarkAsJoined() {
        UUID uuid = UUID.randomUUID();
        assertFalse(tracker.hasJoined(uuid));
        tracker.markAsJoined(uuid, "Player 1");
        assertTrue(tracker.hasJoined(uuid), "UUID should be marked as joined after insert");
    }

    @Test
    void testDoubleInsert() {
        UUID uuid = UUID.randomUUID();
        tracker.markAsJoined(uuid, "Player 2");
        tracker.markAsJoined(uuid, "Player 2"); // Should not throw any exceptions
        assertTrue(tracker.hasJoined(uuid));
    }

    @Test
    void testMultipleUUIDs() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        tracker.markAsJoined(uuid1, "Player 3");
        assertTrue(tracker.hasJoined(uuid1));
        assertFalse(tracker.hasJoined(uuid2));
    }

    // --- User cache importer tests

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
