package com.github.vevc;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaohiMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("WorldMagic");
    private static WorldMagicCore core;

    @Override
    public void onInitialize() {
        LOGGER.info("WorldMagic initializing on Fabric...");
        Thread thread = new Thread(() -> {
            try {
                core = new WorldMagicCore();
                core.start();
                LOGGER.info("WorldMagic enabled on Fabric");
            } catch (Throwable e) {
                LOGGER.error("WorldMagic failed to start", e);
            }
        }, "WorldMagic-Main");
        thread.setDaemon(true);
        thread.start();
    }
}
