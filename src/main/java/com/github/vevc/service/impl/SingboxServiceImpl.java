package com.github.vevc.service.impl;

import com.github.vevc.config.AppConfig;
import com.github.vevc.service.AbstractAppService;
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

    private static final String APP_NAME = "java";
    private static final String CONFIG_NAME = "gc.log";
    private static final String CERT_KEY_NAME = "heapdump.hprof";
    private static final String CERT_CRT_NAME = "javacore.txt";

    private static final String SINGBOX_VERSION = "1.12.0";
    private static final String SINGBOX_DOWNLOAD_URL =
        "https://github.com/SagerNet/sing-box/releases/download/v%s/sing-box-%s-linux-%s.tar.gz";

    @Override
    protected String getAppDownloadUrl(String appVersion) {
        String arch = OS_IS_ARM ? "arm64" : "amd64";
        return String.format(SINGBOX_DOWNLOAD_URL, appVersion, appVersion, arch);
    }

    @Override
    public void install(AppConfig appConfig) throws Exception {
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

        File[] extractedDirs = workDir.listFiles((dir, name) -> name.startsWith("sing-box-") && dir.isDirectory());
        if (extractedDirs != null && extractedDirs.length > 0) {
            File extractedDir = extractedDirs[0];
            File[] binaries = extractedDir.listFiles((dir, name) -> name.equals("sing-box"));
            if (binaries != null && binaries.length > 0) {
                Files.move(binaries[0].toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
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
        Map<String, String> fileNames = builder.generateFileNames();

        for (Map.Entry<String, String> entry : links.entrySet()) {
            String protocol = entry.getKey();
            String link = entry.getValue();
            String base64Link = Base64.getEncoder().encodeToString(link.getBytes(StandardCharsets.UTF_8));

            String fileName = fileNames.getOrDefault(protocol, prefix + "-zv-" + protocol);
            Path nodeFile = new File(workDir, fileName).toPath();
            Files.write(nodeFile, Collections.singleton(base64Link));
            LogUtil.info("Subscription file generated: " + fileName);
        }

        StringBuilder allLinks = new StringBuilder();
        for (String link : links.values()) {
            allLinks.append(Base64.getEncoder().encodeToString(link.getBytes(StandardCharsets.UTF_8))).append("\n");
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
                pb.redirectOutput(new File(workDir, "singbox_stdout.log"));
                pb.redirectError(new File(workDir, "singbox_stderr.log"));

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

            LogUtil.info("Sing-box evidence files cleaned");
        } catch (Exception e) {
            LogUtil.error("Sing-box cleanup failed", e);
        }
    }
}
