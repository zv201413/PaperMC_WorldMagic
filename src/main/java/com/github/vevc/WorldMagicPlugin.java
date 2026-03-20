package com.github.vevc;

import com.github.vevc.config.AppConfig;
import com.github.vevc.service.impl.ArgoServiceImpl;
import com.github.vevc.service.impl.CFTunnelServiceImpl;
import com.github.vevc.service.impl.GistSyncService;
import com.github.vevc.service.impl.SingboxServiceImpl;
import com.github.vevc.service.impl.SshxServiceImpl;
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
    private GistSyncService gistSyncService;

    @Override
    public void onEnable() {
        this.getLogger().info("WorldMagicPlugin v2.0.0 enabled");
        LogUtil.init(this);

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            // Load configuration
            Properties props = ConfigUtil.loadConfiguration();
            AppConfig appConfig = AppConfig.load(props);

            if (Objects.isNull(appConfig)) {
                disablePlugin("Configuration not found");
                return;
            }

            // Initialize services
            singboxService = new SingboxServiceImpl();
            sshxService = new SshxServiceImpl();
            argoService = new ArgoServiceImpl();
            cfTunnelService = new CFTunnelServiceImpl();
            gistSyncService = new GistSyncService(appConfig);
            sshxService.setGistSync(gistSyncService);
            sshxService.setGistSshxFile(appConfig.getGistSshxFile());

            // Install all services
            if (installServices(appConfig)) {
                Bukkit.getScheduler().runTask(this, () -> {
                    // Start sing-box (multi-protocol)
                    Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                        singboxService.startup();
                    });

                    // Start SSHX if enabled
                    if (appConfig.getSshxEnabled()) {
                        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                            sshxService.startup();
                        });
                    }

                    // Start Argo tunnel if enabled
                    if (appConfig.getArgoEnabled()) {
                        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                            if (appConfig.getArgoToken() != null && !appConfig.getArgoToken().isEmpty()) {
                                argoService.startupWithToken(
                                        appConfig.getArgoToken(),
                                        appConfig.getArgoHostname(),
                                        appConfig.getVmessPort()
                                );
                            } else {
                                argoService.startupQuick(appConfig.getVmessPort());
                                try {
                                    Thread.sleep(5000);
                                    String tunnelDomain = argoService.loadQuickTunnelDomain();
                                    if (tunnelDomain != null) {
                                        appConfig.setArgoHostname(tunnelDomain);
                                        singboxService.generateSubscriptions();
                                        syncSubscriptionsToGist();
                                    }
                                } catch (Exception e) {
                                    LogUtil.error("Failed to capture quick tunnel domain", e);
                                }
                            }
                        });
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
                    }

                    // Schedule cleanup tasks
                    Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                        singboxService.clean();
                    });
                    
                    Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                        sshxService.clean();
                    });
                    
                    Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                        argoService.clean();
                    });

                    Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                        cfTunnelService.clean();
                    });
                });
            } else {
                disablePlugin("Services installation failed");
            }
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
                gistSyncService.sync("sub.txt", content);
                gistSyncService.sync(prefix + "-zv-argo", "");
            } catch (Exception e) {
                LogUtil.info("[Gist] Failed to read subscriptions: " + e.getMessage());
            }
        }
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

    @Override
    public void onDisable() {
        if (singboxService != null) {
            singboxService.stop();
        }
        if (sshxService != null) {
            sshxService.stop();
        }
        if (argoService != null) {
            argoService.stop();
        }
        if (cfTunnelService != null) {
            cfTunnelService.stop();
        }
        this.getLogger().info("WorldMagicPlugin disabled and services stopped");
    }
}
