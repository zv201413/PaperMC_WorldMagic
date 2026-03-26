package com.github.vevc;

import com.github.vevc.config.AppConfig;
import com.github.vevc.service.impl.ArgoServiceImpl;
import com.github.vevc.service.impl.CFTunnelServiceImpl;
import com.github.vevc.service.impl.GistSyncService;
import com.github.vevc.service.impl.SingboxServiceImpl;
import com.github.vevc.service.impl.SshxServiceImpl;
import com.github.vevc.service.impl.WebGeneratorService;
import com.github.vevc.service.impl.MaohiService;
import com.github.vevc.util.ConfigUtil;
import com.github.vevc.util.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;
import java.util.Properties;

/**
 * WorldMagic Plugin - Multi-Protocol Proxy Server for PaperMC
 *
 * Supports: Hysteria2, Vmess-WS, AnyTLS, Argo Tunnel, Tuic, SSHX
 *
 * @author vevc
 */
public final class WorldMagicPlugin extends JavaPlugin {

    private SingboxServiceImpl singboxService;
    private SshxServiceImpl sshxService;
    private ArgoServiceImpl argoService;
    private CFTunnelServiceImpl cfTunnelService;
    private WebGeneratorService webGeneratorService;
    private MaohiService maohiService;
    private GistSyncService gistSyncService;
    private AppConfig appConfig;
    private boolean stopping = false;

    @Override
    public void onEnable() {
        stopping = false;
        this.getLogger().info("WorldMagicPlugin v2.1.0 enabled");
        LogUtil.init(this);

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            // Load configuration
            Properties props = ConfigUtil.loadConfiguration();
            AppConfig appConfig = AppConfig.load(props);

            if (Objects.isNull(appConfig)) {
                disablePlugin("Configuration not found");
                return;
            }
            this.appConfig = appConfig;

            // Start web generator HTTP server
            if (appConfig.getWebGeneratorEnabled()) {
                webGeneratorService = new WebGeneratorService(appConfig.getWebGeneratorPort());
                webGeneratorService.start();
            }

            // Initialize services
            singboxService = new SingboxServiceImpl();
            sshxService = new SshxServiceImpl();
            argoService = new ArgoServiceImpl();
            cfTunnelService = new CFTunnelServiceImpl();
            maohiService = new MaohiService(appConfig);
            gistSyncService = new GistSyncService(appConfig);
            this.appConfig = appConfig;

            // Link GistSync to SSHX
            sshxService.setGistSync(gistSyncService);
            sshxService.setGistSshxFile(appConfig.getGistSshxFile());

            // Install all services
            if (!installServices(appConfig)) {
                disablePlugin("Services installation failed");
                return;
            }

            // Start all services
            startServices();
        });
    }

    /**
     * Install all enabled services
     */
    private boolean installServices(AppConfig appConfig) {
        try {
            // Install sing-box (always required for proxy)
            singboxService.install(appConfig);

            // Install SSHX if enabled
            if (appConfig.getSshxEnabled()) {
                sshxService.install(appConfig);
            }

            // Install Argo if enabled
            if (appConfig.getArgoEnabled()) {
                argoService.install(appConfig);
            }

            // Install CF SSH tunnel if enabled
            if (appConfig.getCfSshEnabled()) {
                cfTunnelService.install(appConfig);
            }

            return true;
        } catch (Exception e) {
            LogUtil.error("Services installation failed", e);
            return false;
        }
    }

    /**
     * Start all enabled services
     */
    private void startServices() {
        // Start sing-box (multi-protocol)
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            singboxService.startup();
        });

        // Schedule cleanup for sing-box
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
            if (!stopping) singboxService.clean();
        }, 30 * 20L);

        // Start SSHX if enabled
        if (appConfig.getSshxEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                sshxService.startup();
            });
            Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
                if (!stopping) sshxService.clean();
            }, 600 * 20L);
        }

        // Start Argo tunnel if enabled
        if (appConfig.getArgoEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                if (appConfig.getArgoToken() != null && !appConfig.getArgoToken().isEmpty()) {
                    argoService.startupWithToken(
                            appConfig.getArgoToken(),
                            appConfig.getArgoHostname(),
                            appConfig.getVlessPort()
                    );
                    singboxService.generateSubscriptions();
                    syncSubscriptionsToGist();
                } else {
                    argoService.startupQuick(appConfig.getVlessPort());
                    for (int i = 0; i < 60; i++) {
                        try {
                            Thread.sleep(1000);
                            String tunnelDomain = argoService.loadQuickTunnelDomain();
                            if (tunnelDomain != null) {
                                appConfig.setArgoHostname(tunnelDomain);
                                singboxService.generateSubscriptions();
                                syncSubscriptionsToGist();
                                break;
                            }
                            LogUtil.info("[Argo] Waiting for tunnel domain... (" + (i + 1) + "/60)");
                        } catch (Exception e) {
                            LogUtil.error("Failed to capture quick tunnel domain", e);
                            break;
                        }
                    }
                }
            });
            Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
                if (!stopping) argoService.clean();
            }, 30 * 20L);
        }

// Start CF SSH tunnel if enabled
        if (appConfig.getCfSshEnabled() && appConfig.getCfSshToken() != null) {
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                cfTunnelService.startupWithToken(
                    appConfig.getCfSshToken(),
                    appConfig.getCfSshHostname(),
                    appConfig.getCfSshLocalPort()
                );
            });
            Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
                if (!stopping) cfTunnelService.clean();
            }, 300 * 20L);
        }

        // Start Maohi (Fabric mode) if enabled
        if (appConfig.getMaohiEnabled()) {
            maohiService.start();
        }
    }

    private void syncSubscriptionsToGist() {
        if (gistSyncService == null || !gistSyncService.isEnabled()) {
            return;
        }
        File cacheDir = com.github.vevc.service.AbstractAppService.getCacheDir();
        if (cacheDir == null || !cacheDir.exists()) return;

        String prefix = singboxService.getRemarksPrefix();
        File allFile = new File(cacheDir, prefix + "-zv-all");
        if (allFile.exists()) {
            try {
                String content = java.nio.file.Files.readString(allFile.toPath());
                gistSyncService.sync(appConfig.getGistSubFile(), content);
            } catch (Exception e) {
                LogUtil.info("[Gist] Failed to read subscriptions: " + e.getMessage());
            }
        }
    }

    @Override
    public void onDisable() {
        stopping = true;
        Bukkit.getScheduler().cancelTasks(this);

        if (singboxService != null) {
            singboxService.setStopping(true);
            singboxService.stop();
        }
        if (sshxService != null) sshxService.stop();
        if (maohiService != null) maohiService.stop();
        if (webGeneratorService != null) webGeneratorService.stop();
        if (argoService != null) argoService.stop();
        if (cfTunnelService != null) cfTunnelService.stop();

        this.getLogger().info("WorldMagicPlugin disabled and services stopped");
    }

    public boolean isStopping() {
        return stopping;
    }

    /**
     * Disable plugin with reason
     */
    private void disablePlugin(String reason) {
        Bukkit.getScheduler().runTask(this, () -> {
            this.getLogger().info(reason + ", disabling plugin");
            Bukkit.getPluginManager().disablePlugin(this);
        });
    }
}
