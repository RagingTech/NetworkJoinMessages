package xyz.earthcow.networkjoinmessages.common.storage;

import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.util.SQLDriverLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;

/**
 * Entry point for all storage initialisation at plugin startup.
 *
 * <h4>Migration detection</h4>
 * Rather than maintaining a separate state file, migration is triggered by the
 * presence of leftover data files from a previous backend.
 *
 * <h4>SQL source limitation</h4>
 * If the configured type is H2 or TEXT but the previous type was SQL, there are
 * no local files to detect — the data lives on a remote server. Automatic
 * migration in that direction is not supported. Administrators who need to move
 * data <em>from</em> SQL <em>to</em> a local backend should use the
 * {@code /njimport} command after startup.
 *
 * <h4>Multiple remnant files</h4>
 * If both {@code joined.mv.db} and {@code joined.txt} exist simultaneously
 * (which should not happen under normal operation), the H2 file takes priority
 * since it was the default backend and is more likely to be authoritative.
 *
 * <h4>File archiving</h2>
 * After a successful migration the source files are moved into a
 * {@code migrate/} subdirectory. This provides a recoverable backup and
 * prevents the same files from triggering a re-migration on the next restart.
 *
 * <h4>Error handling</h4>
 * Failure to open the <em>source</em> backend skips migration with a warn;
 * the new backend starts empty. Failure to open the <em>target</em> backend is
 * fatal and propagates as {@link StorageInitializationException}.
 */
public final class StorageInitializer {

    // H2 appends these suffixes to the base path passed to its constructor
    private static final String H2_MAIN_SUFFIX  = ".mv.db";
    private static final String H2_TRACE_SUFFIX = ".trace.db";
    private static final String TEXT_JOINED_FILE = "joined.txt";

    // Base names passed to H2 constructors (relative to dataFolder)
    private static final String H2_FIRSTJOIN_BASE  = "joined";
    private static final String H2_PLAYERDATA_BASE = "player_data";

    private StorageInitializer() {}

    /**
     * Initializes both storage backends, automatically migrating data if
     * leftover files from a previous backend are detected.
     *
     * @param firstJoinType  the first-join tracker backend from config
     * @param playerDataType the player-data store backend from config (TEXT is rejected)
     * @param sqlConfig      SQL connection parameters
     * @param dataFolder     the plugin's data directory
     * @param logger         plugin logger
     * @return the fully initialised, ready-to-use backends
     * @throws StorageInitializationException if a target backend cannot be created,
     *         or if {@code playerDataType} is {@link StorageType#TEXT}
     */
    public static ActiveStorageBackends initialize(
        StorageType firstJoinType,
        StorageType playerDataType,
        SQLConfig sqlConfig,
        Path dataFolder,
        CoreLogger logger) throws StorageInitializationException {

        if (playerDataType == StorageType.TEXT) {
            throw new StorageInitializationException(
                "PlayerDataStorageType cannot be TEXT — player-data storage does not support "
                    + "plain-text files. Valid options are H2 and SQL.", null);
        }

        PlayerJoinTracker joinTracker     = initJoinTracker(firstJoinType,  sqlConfig, dataFolder, logger);
        PlayerDataStore   playerDataStore = initPlayerDataStore(playerDataType, sqlConfig, dataFolder, logger);

        return new ActiveStorageBackends(joinTracker, playerDataStore);
    }

    // --- Per-interface initialisation ---

    private static PlayerJoinTracker initJoinTracker(
        StorageType configuredType,
        SQLConfig sqlConfig,
        Path dataFolder,
        CoreLogger logger) throws StorageInitializationException {

        PlayerJoinTracker target = buildJoinTracker(configuredType, sqlConfig, dataFolder, logger);

        // Detect what, if any, legacy backend files are sitting alongside us.
        boolean h2Remnant   = Files.exists(dataFolder.resolve(H2_FIRSTJOIN_BASE + H2_MAIN_SUFFIX));
        boolean textRemnant = Files.exists(dataFolder.resolve(TEXT_JOINED_FILE));

        if (configuredType == StorageType.H2 && !h2Remnant && !textRemnant) {
            // Normal H2 startup — no migration needed regardless.
            return target;
        }

        // H2 remnant takes priority if both somehow coexist.
        if (h2Remnant && configuredType != StorageType.H2) {
            logger.info("[StorageInitializer] Detected joined.mv.db alongside "
                + configuredType + " — migrating H2 → " + configuredType + ".");
            runJoinTrackerMigration(StorageType.H2, sqlConfig, dataFolder, logger, target);
        } else if (textRemnant && configuredType != StorageType.TEXT) {
            logger.info("[StorageInitializer] Detected joined.txt alongside "
                + configuredType + " — migrating TEXT → " + configuredType + ".");
            runJoinTrackerMigration(StorageType.TEXT, sqlConfig, dataFolder, logger, target);
        }

        return target;
    }

    private static PlayerDataStore initPlayerDataStore(
        StorageType configuredType,
        SQLConfig sqlConfig,
        Path dataFolder,
        CoreLogger logger) throws StorageInitializationException {

        PlayerDataStore target = buildPlayerDataStore(configuredType, sqlConfig, dataFolder, logger);

        boolean h2Remnant = Files.exists(dataFolder.resolve(H2_PLAYERDATA_BASE + H2_MAIN_SUFFIX));

        if (h2Remnant && configuredType != StorageType.H2) {
            logger.info("[StorageInitializer] Detected player_data.mv.db alongside "
                + configuredType + " — migrating H2 → " + configuredType + ".");
            runPlayerDataMigration(sqlConfig, dataFolder, logger, target);
        }

        return target;
    }

    // --- Migration runners ---

    private static void runJoinTrackerMigration(
        StorageType sourceType,
        SQLConfig sqlConfig,
        Path dataFolder,
        CoreLogger logger,
        PlayerJoinTracker target) {

        PlayerJoinTracker source = null;
        try {
            source = buildJoinTracker(sourceType, sqlConfig, dataFolder, logger);
        } catch (StorageInitializationException e) {
            logger.warn("[StorageInitializer] Could not open " + sourceType
                + " first-join source for migration: " + e.getMessage());
            logger.warn("[StorageInitializer] First-join migration skipped.");
            return;
        }

        try {
             int count = StorageMigrator.migrateJoinTracker(source, target, logger);
             logger.info("[StorageInitializer] First-join migration complete — "
                 + count + " record(s) transferred.");
             archiveJoinTrackerFiles(sourceType, dataFolder, logger);
        } finally {
            closeSilently(source, logger, "source first-join tracker");
        }
    }

    private static void runPlayerDataMigration(
        SQLConfig sqlConfig,
        Path dataFolder,
        CoreLogger logger,
        PlayerDataStore target) {

        // The only detectable source for player data is H2 (TEXT is invalid,
        // SQL leaves no local files).
        PlayerDataStore source = null;
        try {
            source = buildPlayerDataStore(StorageType.H2, sqlConfig, dataFolder, logger);
        } catch (StorageInitializationException e) {
            logger.warn("[StorageInitializer] Could not open H2 player-data source for migration: "
                + e.getMessage());
            logger.warn("[StorageInitializer] Player-data migration skipped.");
            return;
        }

        try {
             int count = StorageMigrator.migratePlayerDataStore(source, target, logger);
             logger.info("[StorageInitializer] Player-data migration complete — "
                 + count + " record(s) transferred.");
             archivePlayerDataFiles(dataFolder, logger);
        } finally {
            closeSilently(source, logger, "source player-data store");
        }
    }

    // -------------------------------------------------------------------------
    // File archiving
    // -------------------------------------------------------------------------

    private static void archiveJoinTrackerFiles(
        StorageType sourceType, Path dataFolder, CoreLogger logger) {
        switch (sourceType) {
            case H2 -> {
                archiveFile(dataFolder.resolve(H2_FIRSTJOIN_BASE + H2_MAIN_SUFFIX),  dataFolder, logger);
                archiveFile(dataFolder.resolve(H2_FIRSTJOIN_BASE + H2_TRACE_SUFFIX), dataFolder, logger);
            }
            case TEXT ->
                archiveFile(dataFolder.resolve(TEXT_JOINED_FILE), dataFolder, logger);
            default ->
                logger.debug("[StorageInitializer] No local files to archive for source type " + sourceType + ".");
        }
    }

    private static void archivePlayerDataFiles(Path dataFolder, CoreLogger logger) {
        archiveFile(dataFolder.resolve(H2_PLAYERDATA_BASE + H2_MAIN_SUFFIX),  dataFolder, logger);
        archiveFile(dataFolder.resolve(H2_PLAYERDATA_BASE + H2_TRACE_SUFFIX), dataFolder, logger);
    }

    /**
     * Moves {@code file} into {@code <dataFolder>/migrate/}, creating that
     * directory if needed. Silently skips files that do not exist (H2 only
     * creates the {@code .trace.db} file when debug logging is active).
     * Appends a numeric suffix if an archive of the same name already exists.
     */
    private static void archiveFile(Path file, Path dataFolder, CoreLogger logger) {
        if (!Files.exists(file)) return;
        try {
            Path archiveDir = dataFolder.resolve("migrate");
            Files.createDirectories(archiveDir);
            Path destination = resolveNonConflicting(archiveDir.resolve(file.getFileName()));
            Files.move(file, destination, StandardCopyOption.ATOMIC_MOVE);
            logger.info("[StorageInitializer] Archived " + file.getFileName()
                + " → migrate/" + destination.getFileName());
        } catch (IOException e) {
            logger.warn("[StorageInitializer] Could not archive " + file.getFileName()
                + ": " + e.getMessage() + " — file remains in place.");
        }
    }

    /**
     * Returns {@code path} unchanged if it does not exist, otherwise appends
     * {@code .1}, {@code .2}, … until a free name is found.
     */
    private static Path resolveNonConflicting(Path path) {
        if (!Files.exists(path)) return path;
        String name   = path.getFileName().toString();
        Path   parent = path.getParent();
        int    n      = 1;
        Path   candidate;
        do {
            candidate = parent.resolve(name + "." + n++);
        } while (Files.exists(candidate));
        return candidate;
    }

    // -------------------------------------------------------------------------
    // Backend factories
    // -------------------------------------------------------------------------

    private static PlayerJoinTracker buildJoinTracker(
        StorageType type,
        SQLConfig sqlConfig,
        Path dataFolder,
        CoreLogger logger) throws StorageInitializationException {
        return switch (type) {
            case H2 -> {
                try {
                    yield new H2PlayerJoinTracker(
                        logger,
                        dataFolder.resolve(H2_FIRSTJOIN_BASE).toAbsolutePath().toString()
                    );
                } catch (SQLException e) {
                    throw new StorageInitializationException("Failed to open H2PlayerJoinTracker", e);
                }
            }
            case SQL -> {
                try {
                    yield new SQLPlayerJoinTracker(logger, sqlConfig, dataFolder);
                } catch (SQLException | SQLDriverLoader.DriverLoadException e) {
                    throw new StorageInitializationException("Failed to open SQLPlayerJoinTracker", e);
                }
            }
            case TEXT -> {
                try {
                    yield new TextPlayerJoinTracker(
                        logger,
                        dataFolder.resolve(TEXT_JOINED_FILE)
                    );
                } catch (IOException e) {
                    throw new StorageInitializationException("Failed to open TextPlayerJoinTracker", e);
                }
            }
        };
    }

    private static PlayerDataStore buildPlayerDataStore(
        StorageType type,
        SQLConfig sqlConfig,
        Path dataFolder,
        CoreLogger logger) throws StorageInitializationException {
        return switch (type) {
            case H2 -> {
                try {
                    yield new H2PlayerDataStore(
                        logger,
                        dataFolder.resolve(H2_PLAYERDATA_BASE).toAbsolutePath().toString()
                    );
                } catch (SQLException e) {
                    throw new StorageInitializationException("Failed to open H2PlayerDataStore", e);
                }
            }
            case SQL -> {
                try {
                    yield new SQLPlayerDataStore(logger, sqlConfig, dataFolder);
                } catch (SQLException | SQLDriverLoader.DriverLoadException e) {
                    throw new StorageInitializationException("Failed to open SQLPlayerDataStore", e);
                }
            }
            case TEXT -> throw new StorageInitializationException(
                "TEXT is not a valid PlayerDataStorageType.", null);
        };
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void closeSilently(AutoCloseable c, CoreLogger logger, String label) {
        if (c == null) return;
        try {
            c.close();
        } catch (Exception e) {
            logger.warn("[StorageInitializer] Error closing " + label + ": " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Checked exception
    // -------------------------------------------------------------------------

    /**
     * Thrown when a storage backend cannot be created during plugin startup.
     * Wraps the underlying cause so callers need only catch one type.
     */
    public static final class StorageInitializationException extends Exception {
        public StorageInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}