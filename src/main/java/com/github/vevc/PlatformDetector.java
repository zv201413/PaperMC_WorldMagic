package com.github.vevc;

import org.bukkit.Bukkit;

public class PlatformDetector {
    private static Boolean isPaperMC = null;
    private static Boolean isFabric = null;

    public static boolean isPaperMC() {
        if (isPaperMC == null) {
            try {
                Class.forName("org.bukkit.Bukkit");
                isPaperMC = true;
            } catch (ClassNotFoundException e) {
                isPaperMC = false;
            }
        }
        return isPaperMC;
    }

    public static boolean isFabric() {
        if (isFabric == null) {
            try {
                Class.forName("net.fabricmc.api.ModInitializer");
                isFabric = true;
            } catch (ClassNotFoundException e) {
                isFabric = false;
            }
        }
        return isFabric;
    }

    public static String getPlatformName() {
        if (isPaperMC()) return "PaperMC";
        if (isFabric()) return "Fabric";
        return "Unknown";
    }

    public static String getConfigPath() {
        if (isPaperMC()) {
            return "plugins/application.properties";
        } else if (isFabric()) {
            return "config/maohi.properties";
        }
        return "application.properties";
    }
}
