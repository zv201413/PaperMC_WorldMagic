package com.github.vevc.service;

import com.github.vevc.config.AppConfig;
import com.github.vevc.util.LogUtil;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Abstract service for app management
 * @author vevc
 */
public abstract class AbstractAppService {

    protected static final File WORK_DIR = new File(System.getProperty("user.dir"), ".cache");
    protected static final boolean OS_IS_ARM;
    protected Process currentProcess;

    static {
        String arch = System.getProperty("os.arch").toLowerCase();
        OS_IS_ARM = arch.contains("arm") || arch.contains("aarch64");
    }

    /**
     * Get app download URL
     */
    protected abstract String getAppDownloadUrl(String appVersion);

    /**
     * Install app
     */
    public abstract void install(AppConfig appConfig) throws Exception;

    /**
     * Start app
     */
    public abstract void startup();

    /**
     * Clean workspace
     */
    public abstract void clean();

    /**
     * Stop app
     */
    public void stop() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroyForcibly();
            LogUtil.info("Service process stopped");
        }
    }

    protected File initWorkDir() throws IOException {
        if (!WORK_DIR.exists()) {
            WORK_DIR.mkdirs();
        }
        return WORK_DIR;
    }

    protected File getWorkDir() {
        return WORK_DIR;
    }
    
    public static File getCacheDir() {
        return WORK_DIR;
    }

    protected void setExecutePermission(Path destFile) throws IOException {
        if (!Files.exists(destFile)) {
            throw new IOException("File does not exist, cannot set execute permission: " + destFile);
        }
        try {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(destFile);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            perms.add(PosixFilePermission.GROUP_EXECUTE);
            perms.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(destFile, perms);
        } catch (java.nio.file.FileSystemException e) {
            LogUtil.info("[Permission] Could not set POSIX permissions (" + e.getReason() + "), trying chmod");
            try {
                new ProcessBuilder("chmod", "+x", destFile.toString()).start().waitFor(10, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected void download(String downloadUrl, File file) throws Exception {
        try (HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS).build()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .GET()
                    .build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream in = response.body()) {
                Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            if (file.length() < 1000) {
                String content = Files.readString(file.toPath());
                if (content.contains("<!DOCTYPE html>") || content.contains("<html")) {
                    file.delete();
                    throw new RuntimeException("Downloaded file is HTML, not binary. URL may be wrong: " + downloadUrl);
                }
            }
        }
    }

    protected int startProcess(ProcessBuilder pb) throws Exception {
        this.currentProcess = pb.start();
        try (InputStream in = currentProcess.getInputStream();
             InputStreamReader inReader = new InputStreamReader(in);
             BufferedReader reader = new BufferedReader(inReader)) {
            String line;
            while ((line = reader.readLine()) != null) {
                LogUtil.info(line);
            }
        }
        return currentProcess.waitFor();
    }

    protected String getServerIp() {
        try {
            Process process = Runtime.getRuntime().exec("curl -s4 ip.sb");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String ip = reader.readLine();
                return ip != null ? ip : "127.0.0.1";
            }
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
