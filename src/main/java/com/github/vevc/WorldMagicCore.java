package com.github.vevc;

import com.github.vevc.config.AppConfig;
import com.github.vevc.service.impl.*;
import com.github.vevc.util.ConfigUtil;
import com.github.vevc.util.LogUtil;

import java.io.File;

public class WorldMagicCore {
    private AppConfig config;
    private SingboxService singboxService;
    private ArgoService argoService;
    private SshxService sshxService;
    private GistSyncService gistSyncService;
    private MaohiService maohiService;
    private boolean stopping = false;

    public void start() throws Exception {
        LogUtil.info("Loading configuration...");
        
        config = ConfigUtil.loadConfiguration();
        
        if (config == null) {
            LogUtil.error("Failed to load configuration");
            return;
        }

        if (config.getMaohiEnabled()) {
            LogUtil.info("Starting in Maohi mode...");
            maohiService = new MaohiService(config);
            maohiService.start();
            return;
        }

        singboxService = new SingboxServiceImpl(config);
        argoService = new ArgoServiceImpl();
        sshxService = new SshxServiceImpl();
        gistSyncService = new GistSyncService(config);

        singboxService.install();
        singboxService.startup();

        if (config.getSshxEnabled()) {
            sshxService.startup();
        }

        if (config.getArgoEnabled()) {
            argoService.install(config);
            argoService.startupQuick(config.getVlessPort());
        }

        if (config.getGistId() != null && !config.getGistId().isEmpty()) {
            gistSyncService.syncAll();
        }

        LogUtil.info("WorldMagic core started successfully");
    }

    public void stop() {
        stopping = true;
        LogUtil.info("WorldMagic core stopping...");
        
        if (singboxService != null) {
            singboxService.shutdown();
        }
        if (argoService != null) {
            argoService.shutdown();
        }
        if (sshxService != null) {
            sshxService.shutdown();
        }
        if (maohiService != null) {
            maohiService.stop();
        }
        
        LogUtil.info("WorldMagic core stopped");
    }
}
