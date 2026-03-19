package xyz.earthcow.networkjoinmessages.common.storage;

import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.util.DriverShim;

import java.sql.*;
import java.util.Enumeration;

abstract class H2Handler implements AutoCloseable {

    protected final CoreLogger logger;
    protected final String dbPath;
    private final String logPrefix;
    private Connection connection;

    protected H2Handler(CoreLogger logger, String dbPath) throws SQLException {
        this.logger = logger;
        this.dbPath = dbPath;
        this.logPrefix = "[" + getClass().getSimpleName() + "] ";
        registerDriverIfNeeded();
        setUpConnection();
    }

    protected abstract String createTableSql();

    private void setUpConnection() throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:h2:file:" + dbPath);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSql());
        }
    }

    protected synchronized Connection connection() throws SQLException {
        if (isConnectionInvalid()) return null;
        else return connection;
    }

    /**
     * Checks whether the connection is valid, attempting to reconnect if needed.
     */
    public synchronized boolean isConnectionInvalid() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                setUpConnection();
            }
            return false;
        } catch (SQLException e) {
            logger.severe(logPrefix + "Cannot access player data store at '" + dbPath + "'. Does the file exist?");
            return true;
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * Registers the H2 JDBC driver via a {@link DriverShim} if it has not already been registered.
     * This is necessary to avoid issues when the H2 driver class is loaded by a non-system classloader.
     */
    private void registerDriverIfNeeded() {
        try {
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                if (drivers.nextElement().getClass().getName().equals("org.h2.Driver")) {
                    return; // already registered
                }
            }
            Class<?> clazz = Class.forName("org.h2.Driver", true, H2Handler.class.getClassLoader());
            Driver realDriver = (Driver) clazz.getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(new DriverShim(realDriver));
        } catch (Exception e) {
            logger.severe("Failed to manually register the H2 JDBC driver. First-join tracking will be unavailable.");
        }
    }
}
