package com.github.vevc;

import com.github.vevc.util.LogUtil;

public class Main {
    public static void main(String[] args) {
        String platform = PlatformDetector.getPlatformName();
        LogUtil.info("Detected platform: " + platform);
        
        if (PlatformDetector.isPaperMC()) {
            LogUtil.info("Running as PaperMC plugin - plugin.yml will be loaded by server");
            LogUtil.info("If you see this message, you may have started the JAR directly.");
            LogUtil.info("Please place the JAR in the plugins/ folder and start the PaperMC server.");
        } else if (PlatformDetector.isFabric()) {
            LogUtil.info("Running as Fabric mod - fabric.mod.json will be loaded by server");
            LogUtil.info("If you see this message, you may have started the JAR directly.");
            LogUtil.info("Please place the JAR in the mods/ folder and start the Fabric server.");
        } else {
            LogUtil.info("No Minecraft platform detected.");
            LogUtil.info("Starting WorldMagic in standalone mode...");
            
            try {
                WorldMagicCore core = new WorldMagicCore();
                core.start();
                
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    LogUtil.info("Shutting down WorldMagic...");
                    core.stop();
                }));
                
                Thread.currentThread().join();
            } catch (Exception e) {
                LogUtil.error("Failed to start WorldMagic", e);
                System.exit(1);
            }
        }
    }
}
