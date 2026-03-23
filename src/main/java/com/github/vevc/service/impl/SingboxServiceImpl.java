package com.github.vevc.service.impl;

import com.github.vevc.config.AppConfig;
import com.github.vevc.service.AbstractAppService;
import com.github.vevc.util.LogUtil;
import com.github.vevc.util.SingboxConfigBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Sing-box multi-protocol service implementation
 * Supports: Hysteria2, Vmess-WS, AnyTLS, Tuic, Argo
 * @author zv
 */
public class SingboxServiceImpl extends AbstractAppService {

    private AppConfig config;
    private volatile boolean stopping = false;
    private static final String APP_NAME = "java";
    private static final String CONFIG_NAME = "gc.log";
    private static final String CERT_KEY_NAME = "heapdump.hprof";
    private static final String CERT_CRT_NAME = "javacore.txt";

    private static final String SINGBOX_VERSION = "1.10.0";
    private static final String SINGBOX_DOWNLOAD_URL =
        "https://github.com/SagerNet/sing-box/releases/download/v%s/sing-box-%s-linux-%s.tar.gz";

    public String getRemarksPrefix() {
        return config != null ? config.getRemarksPrefix() : "vevc";
    }

    @Override
    protected String getAppDownloadUrl(String appVersion) {
        String arch = OS_IS_ARM ? "arm64" : "amd64";
        return String.format(SINGBOX_DOWNLOAD_URL, appVersion, appVersion, arch);
    }

    @Override
    public void install(AppConfig appConfig) throws Exception {
        this.config = appConfig;
        File workDir = this.initWorkDir();

        File tarGzFile = new File(workDir, "download.tar.gz");
        String downloadUrl = this.getAppDownloadUrl(SINGBOX_VERSION);
        LogUtil.info("Sing-box download url: " + downloadUrl);

        this.download(downloadUrl, tarGzFile);
        LogUtil.info("Sing-box downloaded successfully");

        File appFile = new File(workDir, APP_NAME);
        extractSingbox(tarGzFile, workDir, appFile);
        this.setExecutePermission(appFile.toPath());
        tarGzFile.delete();
        LogUtil.info("Sing-box installed successfully");

        generateSelfSignedCert(workDir, appConfig);
        generateConfig(workDir, appConfig);
    }
    
    public void setStopping(boolean stopping) {
        this.stopping = stopping;
    }

    public void generateSubscriptions() {
        File workDir = this.getWorkDir();
        if (workDir != null && config != null) {
            try {
                LogUtil.info("Regenerating subscription files with updated Argo hostname: " + config.getArgoHostname());
                generateSubscriptionFiles(workDir, config);
            } catch (Exception e) {
                LogUtil.error("Failed to generate subscriptions", e);
            }
        }
    }

    private void extractSingbox(File tarGzFile, File workDir, File destFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
            "tar", "-tzf", tarGzFile.getAbsolutePath()
        );
        pb.directory(workDir);
        Process listProc = pb.start();
        boolean listOk = listProc.waitFor(30, TimeUnit.SECONDS);
        if (listOk) {
            String listing = new String(listProc.getInputStream().readAllBytes());
            String firstLine = listing.isEmpty() ? "(empty)" : listing.split("\n")[0];
            LogUtil.info("Tarball root: " + firstLine);
        }

        pb = new ProcessBuilder(
            "tar", "-xzf", tarGzFile.getAbsolutePath(),
            "-C", workDir.getAbsolutePath()
        );
        pb.directory(workDir);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        boolean completed = process.waitFor(60, TimeUnit.SECONDS);

        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("Sing-box extraction timeout");
        }

        File directBin = new File(workDir, "sing-box");
        if (directBin.exists()) {
            Files.move(directBin.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LogUtil.info("Sing-box extracted directly to workDir");
            return;
        }

        File[] extractedDirs = workDir.listFiles((dir, name) -> name.startsWith("sing-box-") && dir.isDirectory());
        if (extractedDirs == null || extractedDirs.length == 0) {
            String[] contents = workDir.list();
            String msg = "No sing-box directory found after extraction. WorkDir contents: "
                + (contents == null ? "null" : String.join(", ", contents));
            LogUtil.error(msg);
            throw new RuntimeException(msg);
        }
        File extractedDir = extractedDirs[0];
        File[] binaries = extractedDir.listFiles((dir, name) -> name.equals("sing-box"));
        if (binaries == null || binaries.length == 0) {
            String[] binContents = extractedDir.list();
            String msg = "sing-box binary not found in " + extractedDir.getName()
                + ". Contents: " + (binContents == null ? "null" : String.join(", ", binContents));
            LogUtil.error(msg);
            throw new RuntimeException(msg);
        }
        Files.move(binaries[0].toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        deleteDirectory(extractedDir);
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }

    private void generateSelfSignedCert(File workDir, AppConfig config) throws Exception {
        String sni = config.getHy2Sni();
        Process p = Runtime.getRuntime().exec(new String[]{
            "sh", "-c",
            String.format("cd '%s' && openssl req -x509 -nodes -newkey ec -pkeyopt ec_paramgen_curve:prime256v1 " +
                "-keyout '%s' -out '%s' -subj '/CN=%s' -days 36500 2>/dev/null",
                workDir.getAbsolutePath(), CERT_KEY_NAME, CERT_CRT_NAME, sni)
        });
        p.waitFor(10, TimeUnit.SECONDS);
        LogUtil.info("Self-signed certificate generated");
    }

    private void generateConfig(File workDir, AppConfig config) throws Exception {
        SingboxConfigBuilder builder = new SingboxConfigBuilder(config);
        String configJson = builder.build();

        File configFile = new File(workDir, CONFIG_NAME);
        Files.writeString(configFile.toPath(), configJson, StandardCharsets.UTF_8);
        LogUtil.info("Sing-box config generated");
    }

    private void generateSubscriptionFiles(File workDir, AppConfig config) throws Exception {
        String serverIp = getServerIp();
        SingboxConfigBuilder builder = new SingboxConfigBuilder(config);
        String prefix = config.getRemarksPrefix();
        LogUtil.info("Using prefix for node names: " + prefix);

        Map<String, String> links = builder.generateShareLinks(serverIp);

        StringBuilder allLinks = new StringBuilder();
        for (String link : links.values()) {
            allLinks.append(link).append("\n");
        }

        Path allFile = new File(workDir, prefix + "-zv-all").toPath();
        Files.writeString(allFile, allLinks.toString());
        LogUtil.info("Combined subscription file generated: " + prefix + "-zv-all");
    }

    @Override
    public void startup() {
        File workDir = this.getWorkDir();
        File appFile = new File(workDir, APP_NAME);
        File configFile = new File(workDir, CONFIG_NAME);

        try {
            while (Files.exists(appFile.toPath()) && Files.exists(configFile.toPath())) {
                ProcessBuilder pb = new ProcessBuilder(
                    appFile.getAbsolutePath(),
                    "run",
                    "-c", configFile.getAbsolutePath()
                );
                pb.directory(workDir);
                pb.redirectErrorStream(true);

                LogUtil.info("Starting Sing-box server...");
                Process p = pb.start();

                Thread reader = new Thread(() -> {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            LogUtil.info("[sing-box] " + line);
                        }
                    } catch (IOException e) {}
                });
                reader.start();

                int exitCode = p.waitFor();
                reader.join(2000);

                if (exitCode == 0) {
                    LogUtil.info("Sing-box process exited normally");
                    break;
                } else {
                    if (this.stopping) {
                        LogUtil.info("Sing-box process exited during shutdown (code: " + exitCode + ")");
                        break;
                    }
                    LogUtil.info("Sing-box process exited with code: " + exitCode + ", restarting...");
                    TimeUnit.SECONDS.sleep(3);
                }
            }
        } catch (Exception e) {
            LogUtil.error("Sing-box startup failed", e);
        }
    }

    @Override
    public void clean() {
        File workDir = this.getWorkDir();
        File appFile = new File(workDir, APP_NAME);
        File configFile = new File(workDir, CONFIG_NAME);
        File keyFile = new File(workDir, CERT_KEY_NAME);
        File crtFile = new File(workDir, CERT_CRT_NAME);

        try {
            TimeUnit.SECONDS.sleep(30);

            Files.deleteIfExists(appFile.toPath());
            Files.deleteIfExists(configFile.toPath());
            Files.deleteIfExists(keyFile.toPath());
            Files.deleteIfExists(crtFile.toPath());

            File[] files = workDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String name = f.getName();
                    if (name.endsWith(".tar.gz") || name.startsWith("sing-box-")) {
                        Files.deleteIfExists(f.toPath());
                    }
                }
            }

            LogUtil.info("Sing-box evidence files (binary/config) cleaned");

            if (this.config != null) {
                TimeUnit.SECONDS.sleep(270);
                Map<String, String> fileNames = new SingboxConfigBuilder(this.config).generateFileNames();
                for (String fileName : fileNames.values()) {
                    Files.deleteIfExists(new File(workDir, fileName).toPath());
                }
                Files.deleteIfExists(new File(workDir, this.config.getRemarksPrefix() + "-zv-all").toPath());
                LogUtil.info("Node subscription files cleaned after 5 minutes");
            }
        } catch (Exception e) {
            LogUtil.error("Sing-box cleanup failed", e);
        }
    }
}
