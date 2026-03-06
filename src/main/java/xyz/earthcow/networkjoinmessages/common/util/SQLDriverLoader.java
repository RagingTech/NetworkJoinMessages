package xyz.earthcow.networkjoinmessages.common.util;

import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Driver;
import java.sql.DriverManager;

/**
 * Downloads and dynamically loads a JDBC driver JAR at runtime if it is not
 * already registered with {@link DriverManager}.
 *
 * <p>JARs are cached in {@code <pluginDataFolder>/drivers/} so the download
 * only happens once. If the download fails and no cached JAR exists, a
 * {@link DriverLoadException} is thrown so the caller can disable first-join
 * tracking gracefully.
 */
public class SQLDriverLoader {

    /**
     * Thrown when the driver cannot be loaded and no cached copy is available.
     */
    public static class DriverLoadException extends Exception {
        public DriverLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // Known drivers: name -> { Maven jar URL, fully-qualified driver class name }
    private enum KnownDriver {
        MYSQL(
            "mysql",
            "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/9.6.0/mysql-connector-j-9.6.0.jar",
            "mysql-connector-j-9.6.0.jar",
            "com.mysql.cj.jdbc.Driver"
        ),
        MARIADB(
            "mariadb",
            "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.7/mariadb-java-client-3.5.7.jar",
            "mariadb-java-client-3.5.7.jar",
            "org.mariadb.jdbc.Driver"
        ),
        POSTGRESQL(
            "postgresql",
            "https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.9/postgresql-42.7.9.jar",
            "postgresql-42.7.9.jar",
            "org.postgresql.Driver"
        );

        final String configName;
        final String downloadUrl;
        final String jarName;
        final String driverClassName;

        KnownDriver(String configName, String downloadUrl, String jarName, String driverClassName) {
            this.configName = configName;
            this.downloadUrl = downloadUrl;
            this.jarName = jarName;
            this.driverClassName = driverClassName;
        }

        static KnownDriver fromConfigName(String name) {
            for (KnownDriver d : values()) {
                if (d.configName.equalsIgnoreCase(name)) return d;
            }
            throw new IllegalArgumentException("Unknown SQL driver name: " + name);
        }
    }

    private final CoreLogger logger;
    private final Path driversDir;

    public SQLDriverLoader(CoreLogger logger, Path pluginDataFolder) {
        this.logger = logger;
        this.driversDir = pluginDataFolder.resolve("drivers");
    }

    /**
     * Ensures the JDBC driver for {@code driverName} is registered with
     * {@link DriverManager}, downloading it if necessary.
     *
     * @param driverName one of {@code mysql}, {@code mariadb}, {@code postgresql}
     * @throws DriverLoadException if the driver could not be loaded
     */
    public void ensureLoaded(String driverName) throws DriverLoadException {
        KnownDriver driver = KnownDriver.fromConfigName(driverName);

        // Fast path — already registered (e.g. server bundles it, or we loaded it earlier)
        if (isDriverRegistered(driver.driverClassName)) {
            logger.debug("[SQLDriverLoader] Driver " + driver.driverClassName + " already registered, skipping download.");
            return;
        }

        Path jarPath = driversDir.resolve(driver.jarName);

        // Download if not cached
        if (!Files.exists(jarPath)) {
            downloadDriver(driver, jarPath);
        } else {
            logger.debug("[SQLDriverLoader] Using cached driver JAR: " + jarPath.getFileName());
        }

        // Load the JAR into a child class loader and register the driver via DriverShim
        loadFromJar(driver, jarPath);
    }

    // --- Internal helpers ---

    private boolean isDriverRegistered(String className) {
        var drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            if (drivers.nextElement().getClass().getName().equals(className)) return true;
        }
        return false;
    }

    private void downloadDriver(KnownDriver driver, Path destination) throws DriverLoadException {
        logger.info("[SQLDriverLoader] Downloading " + driver.jarName + " from Maven Central...");
        try {
            Files.createDirectories(driversDir);
            URL url = new URL(driver.downloadUrl);
            // Write to a .tmp file first so a partial download is never mistaken for a valid JAR
            Path tmp = destination.resolveSibling(driver.jarName + ".tmp");
            try (InputStream in = url.openStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tmp, destination, StandardCopyOption.REPLACE_EXISTING);
            logger.info("[SQLDriverLoader] Successfully downloaded " + driver.jarName + ".");
        } catch (IOException e) {
            throw new DriverLoadException(
                "Failed to download " + driver.jarName + " from " + driver.downloadUrl + ": " + e.getMessage(), e
            );
        }
    }

    private void loadFromJar(KnownDriver driver, Path jarPath) throws DriverLoadException {
        try {
            URLClassLoader loader = new URLClassLoader(
                new URL[]{ jarPath.toUri().toURL() },
                getClass().getClassLoader()
            );
            Class<?> driverClass = Class.forName(driver.driverClassName, true, loader);
            Driver realDriver = (Driver) driverClass.getDeclaredConstructor().newInstance();
            // Wrap in a DriverShim so DriverManager accepts a driver from a foreign classloader
            DriverManager.registerDriver(new DriverShim(realDriver));
            logger.debug("[SQLDriverLoader] Loaded and registered " + driver.driverClassName + " from " + jarPath.getFileName() + ".");
        } catch (Exception e) {
            throw new DriverLoadException(
                "Failed to load driver class " + driver.driverClassName + " from JAR " + jarPath + ": " + e.getMessage(), e
            );
        }
    }
}
