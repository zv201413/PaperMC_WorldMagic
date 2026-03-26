package com.github.vevc.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logging utility - supports both PaperMC and Fabric
 * @author vevc
 */
public final class LogUtil {

    private static Logger logger;
    private static boolean initialized = false;

    public static void init(JavaPlugin plugin) {
        logger = plugin.getLogger();
        initialized = true;
    }
    
    public static void initStandalone() {
        logger = Logger.getLogger("WorldMagic");
        initialized = true;
    }

    public static void info(String message) {
        if (logger != null) {
            logger.info(message);
        }
        System.out.println("[WorldMagic] " + message);
    }

    public static void warning(String message) {
        if (logger != null) {
            logger.warning(message);
        }
        System.out.println("[WorldMagic] WARNING: " + message);
    }

    public static void error(String message) {
        if (logger != null) {
            logger.severe(message);
        }
        System.err.println("[WorldMagic] ERROR: " + message);
    }

    public static void error(String message, Throwable e) {
        if (logger != null) {
            logger.log(Level.SEVERE, message, e);
        }
        System.err.println("[WorldMagic] ERROR: " + message);
        if (e != null) e.printStackTrace(System.err);
    }

    private LogUtil() {
        throw new IllegalStateException("Utility class");
    }
}
