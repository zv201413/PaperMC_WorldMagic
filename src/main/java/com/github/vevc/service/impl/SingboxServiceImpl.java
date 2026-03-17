package com.github.vevc.service.impl;

import com.github.vevc.config.AppConfig;
import com.github.vevc.service.AbstractAppService;
import com.github.vevc.util.CountryCodeUtil;
import com.github.vevc.util.LogUtil;
import com.github.vevc.util.SingboxConfigBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Sing-box multi-protocol service implementation
 * Supports: Hysteria2, Vmess-WS, AnyTLS, Tuic, Argo
 * @author zv
 */
public class SingboxServiceImpl extends AbstractAppService {

    // Process camouflage names
    private static final String APP_NAME = "java";           // Disguised as java process
    private static final String CONFIG_NAME = "gc.log";      // Config file disguised as GC log
    private static final String CERT_KEY_NAME = "heapdump.hprof"; // Key file disguised as heap dump
    private static final String CERT_CRT_NAME = "javacore.txt";   // Cert file disguised as javacore

    private static final String SINGBOX_VERSION = "1.12.0";  // Use version supporting AnyTLS
    private static final String SINGBOX_DOWNLOAD_URL = 
            "https://github.com/SagerNet/sing-box/releases/download/v%s/sing-box-%s-linux-%s.tar.gz";

    @Override
    protected String getAppDownloadUrl(String appVersion) {
        String arch = OS_IS_ARM ? "arm64" : "amd64";
        return String.format(SINGBOX_DOWNLOAD_URL, appVersion, arch, arch);
    }

    @Override
    public void install(AppConfig appConfig) throws Exception {
        File workDir = this.initWorkDir();

        // Detect country code and log it
        String countryCode = CountryCodeUtil.getCountryCode();
        LogUtil.info("Detected country code: " + countryCode + " (" + CountryCodeUtil.getCountryName(countryCode) + ")");

        // Download sing-box
        File tarGzFile = new File(workDir, "download.tar.gz");
        String downloadUrl = this.getAppDownloadUrl(SINGBOX_VERSION);
        LogUtil.info("Sing-box download url: " + downloadUrl);

        this.download(downloadUrl, tarGzFile);
        LogUtil.info("Sing-box downloaded successfully");

        // Extract sing-box binary
        File appFile = new File(workDir, APP_NAME);
        extractSingbox(tarGzFile, workDir, appFile);
        this.setExecutePermission(appFile.toPath());
        tarGzFile.delete();
        LogUtil.info("Sing-box installed successfully");

        // Generate self-signed certificate
        generateSelfSignedCert(workDir, appConfig);

        // Generate sing-box configuration
        generateConfig(workDir, appConfig);

        // Generate subscription files
        generateSubscriptionFiles(workDir, appConfig);
    }

    private void extractSingbox(File tarGzFile, File workDir, File destFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
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

        // Find and move the extracted binary
        // The tar contains: sing-box-v1.12.0-linux-amd64/sing-box
        File[] extractedDirs = workDir.listFiles((dir, name) -> name.startsWith("sing-box-") && dir.isDirectory());
        if (extractedDirs != null && extractedDirs.length > 0) {
            File extractedDir = extractedDirs[0];
            File[] binaries = extractedDir.listFiles((dir, name) -> name.equals("sing-box"));
            if (binaries != null && binaries.length > 0) {
                Files.move(binaries[0].toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                // Clean up the extracted directory
                deleteDirectory(extractedDir);
            }
        }
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
        File keyFile = new File(workDir, CERT_KEY_NAME);
        File crtFile = new File(workDir, CERT_CRT_NAME);
        String sni = config.getHy2Sni();

        // Alternative: use simpler command
        Runtime.getRuntime().exec(new String[]{
                "sh", "-c",
                String.format("cd '%s' && openssl req -x509 -nodes -newkey ec -pkeyopt ec_paramgen_curve:prime256v1 " +
                        "-keyout '%s' -out '%s' -subj '/CN=%s' -days 36500 2>/dev/null",
                        workDir.getAbsolutePath(), CERT_KEY_NAME, CERT_CRT_NAME, sni)
        });

        TimeUnit.SECONDS.sleep(2);
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
        
        // Get country code for file naming
        String countryCode = builder.getCountryCode();
        LogUtil.info("Using country code for node names: " + countryCode);
        
        // Generate share links
        Map<String, String> links = builder.generateShareLinks(serverIp);
        
        // Generate file names with country code
        Map<String, String> fileNames = builder.generateFileNames();

        // Generate individual subscription files
        // Format: {CountryCode}-zv-{Protocol}
        for (Map.Entry<String, String> entry : links.entrySet()) {
            String protocol = entry.getKey();
            String link = entry.getValue();
            String base64Link = Base64.getEncoder().encodeToString(link.getBytes(StandardCharsets.UTF_8));

            // File name: US-zv-hysteria2, JP-zv-vmess-ws, etc.
            String fileName = fileNames.getOrDefault(protocol, countryCode + "-zv-" + protocol);
            Path nodeFile = new File(workDir, fileName).toPath();
            Files.write(nodeFile, Collections.singleton(base64Link));
            LogUtil.info("Subscription file generated: " + fileName);
        }

        // Generate combined subscription file
        StringBuilder allLinks = new StringBuilder();
        for (String link : links.values()) {
            allLinks.append(Base64.getEncoder().encodeToString(link.getBytes(StandardCharsets.UTF_8))).append("\n");
        }
        
        // Combined file: {CountryCode}-zv-all
        Path allFile = new File(workDir, countryCode + "-zv-all").toPath();
        Files.writeString(allFile, allLinks.toString());
        LogUtil.info("Combined subscription file generated: " + countryCode + "-zv-all");
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
                pb.redirectOutput(new File("/dev/null"));
                pb.redirectError(new File("/dev/null"));

                LogUtil.info("Starting Sing-box server...");
                int exitCode = this.startProcess(pb);

                if (exitCode == 0) {
                    LogUtil.info("Sing-box process exited normally");
                    break;
                } else {
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
            // Clean after 30 seconds (keep process running)
            TimeUnit.SECONDS.sleep(30);

            Files.deleteIfExists(appFile.toPath());
            Files.deleteIfExists(configFile.toPath());
            Files.deleteIfExists(keyFile.toPath());
            Files.deleteIfExists(crtFile.toPath());

            // Clean temp files
            File[] files = workDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String name = f.getName();
                    if (name.endsWith(".tar.gz") || name.startsWith("sing-box-")) {
                        Files.deleteIfExists(f.toPath());
                    }
                }
            }

            LogUtil.info("Sing-box evidence files cleaned");
        } catch (Exception e) {
            LogUtil.error("Sing-box cleanup failed", e);
        }
    }
}
