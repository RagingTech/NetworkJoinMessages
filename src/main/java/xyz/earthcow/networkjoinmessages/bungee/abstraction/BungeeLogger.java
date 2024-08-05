package xyz.earthcow.networkjoinmessages.bungee.abstraction;

import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;

import java.util.logging.Logger;

public class BungeeLogger implements CoreLogger {

    private final Logger bungeeLogger;

    public BungeeLogger(Logger bungeeLogger) {
        this.bungeeLogger = bungeeLogger;
    }

    @Override
    public void info(String message) {
        bungeeLogger.info(message);
    }

    @Override
    public void warn(String message) {
        bungeeLogger.warning(message);
    }

    @Override
    public void severe(String message) {
        bungeeLogger.severe(message);
    }
}
