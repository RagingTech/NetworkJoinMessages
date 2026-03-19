package xyz.earthcow.networkjoinmessages.common.storage;

import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.util.PlayerDataSnapshot;

import java.util.Map;
import java.util.UUID;

/**
 * Stateless utility that migrates records between storage back-ends.
 *
 * <p>Both migration methods follow the same pattern:
 * <ol>
 *   <li>Bulk-export all records from {@code source} via its {@code exportAll()} method.</li>
 *   <li>Upsert each record into {@code target} one at a time.</li>
 *   <li>Return the number of records successfully written.</li>
 * </ol>
 *
 * <p>Target implementations use upsert semantics, so calling these methods against a
 * partially-populated target is safe — duplicate records are overwritten, not duplicated.
 *
 * <p>Neither method closes the source or target; that responsibility belongs to the caller.
 */
public final class StorageMigrator {

    private StorageMigrator() {}

    /**
     * Copies every first-join record from {@code source} into {@code target}.
     *
     * @param source the backend to read from
     * @param target the backend to write into
     * @param logger used for progress and error messages
     * @return the number of records written to {@code target}
     */
    public static int migrateJoinTracker(
        PlayerJoinTracker source,
        PlayerJoinTracker target,
        CoreLogger logger) {

        logger.info("[StorageMigrator] Exporting first-join records from " + source.getClass().getSimpleName() + "...");
        Map<UUID, String> entries = source.exportAll();

        if (entries.isEmpty()) {
            logger.info("[StorageMigrator] Source contains no first-join records — nothing to migrate.");
            return 0;
        }

        logger.info("[StorageMigrator] Migrating " + entries.size() + " first-join record(s) into "
            + target.getClass().getSimpleName() + "...");
        int count = 0;
        for (Map.Entry<UUID, String> entry : entries.entrySet()) {
            target.markAsJoined(entry.getKey(), entry.getValue());
            count++;
        }

        logger.info("[StorageMigrator] First-join migration complete — " + count + " record(s) written.");
        return count;
    }

    /**
     * Copies every player-data record from {@code source} into {@code target}.
     *
     * @param source the backend to read from
     * @param target the backend to write into
     * @param logger used for progress and error messages
     * @return the number of records written to {@code target}
     */
    public static int migratePlayerDataStore(
        PlayerDataStore source,
        PlayerDataStore target,
        CoreLogger logger) {

        logger.info("[StorageMigrator] Exporting player-data records from " + source.getClass().getSimpleName() + "...");
        Map<UUID, PlayerDataSnapshot> entries = source.exportAll();

        if (entries.isEmpty()) {
            logger.info("[StorageMigrator] Source contains no player-data records — nothing to migrate.");
            return 0;
        }

        logger.info("[StorageMigrator] Migrating " + entries.size() + " player-data record(s) into "
            + target.getClass().getSimpleName() + "...");
        int count = 0;
        for (Map.Entry<UUID, PlayerDataSnapshot> entry : entries.entrySet()) {
            target.saveData(entry.getKey(), entry.getValue());
            count++;
        }

        logger.info("[StorageMigrator] Player-data migration complete — " + count + " record(s) written.");
        return count;
    }
}