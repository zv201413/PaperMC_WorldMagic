package com.github.vevc.service.impl;

import com.github.vevc.config.AppConfig;
import com.github.vevc.util.LogUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MaohiService {

    private static final Path WORK_DIR = Paths.get("./.cache_maohi");
    private static final String APP_NAME = "java";
    private static final String CONFIG_NAME = "gc.log";
    private static final String CERT_KEY_NAME = "heapdump.hprof";
    private static final String CERT_CRT_NAME = "javacore.txt";
    private static final String SINGBOX_VERSION = "1.12.0";
    private static final String SINGBOX_DOWNLOAD_URL =
        "https://github.com/SagerNet/sing-box/releases/download/v%s/sing-box-%s-linux-%s.tar.gz";

    private final AppConfig config;
    private volatile boolean stopping = false;

    private static final Map<String, String[]> COUNTRY_MAP = new HashMap<>();
    static {
        COUNTRY_MAP.put("JP", new String[]{"日本", "🇯🇵"});
        COUNTRY_MAP.put("US", new String[]{"美国", "🇺🇸"});
        COUNTRY_MAP.put("HK", new String[]{"香港", "🇭🇰"});
        COUNTRY_MAP.put("SG", new String[]{"新加坡", "🇸🇬"});
        COUNTRY_MAP.put("KR", new String[]{"韩国", "🇰🇷"});
        COUNTRY_MAP.put("CN", new String[]{"中国", "🇨🇳"});
        COUNTRY_MAP.put("TW", new String[]{"台湾", "🇹🇼"});
        COUNTRY_MAP.put("GB", new String[]{"英国", "🇬🇧"});
        COUNTRY_MAP.put("DE", new String[]{"德国", "🇩🇪"});
        COUNTRY_MAP.put("FR", new String[]{"法国", "🇫🇷"});
    }

    public MaohiService(AppConfig config) {
        this.config = config;
    }

    public void start() {
        if (!config.getMaohiEnabled()) {
            LogUtil.info("[Maohi] Disabled, skipping");
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                runMaohi();
            } catch (Throwable e) {
                LogUtil.error("[Maohi] Error", e);
            }
        }, "Maohi-Main");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        stopping = true;
    }

    private void runMaohi() throws Exception {
        if (!Files.exists(WORK_DIR)) Files.createDirectories(WORK_DIR);
        downloadSingbox();
        chmodBinary();
        generateCert();
        String serverIP = getServerIP();
        
        runSingbox();
        
        String argoProto = config.getMaohiArgo();
        if (argoProto != null && !argoProto.isEmpty()) {
            LogUtil.info("[Maohi] Starting Argo tunnel...");
            ArgoServiceImpl argoService = new ArgoServiceImpl();
            argoService.install(config);
            int targetPort = argoProto.contains("vless") ? config.getMaohiVlessPort() : config.getMaohiVmessPort();
            
            String auth = config.getMaohiArgoAuth();
            String fixedDomain = config.getMaohiArgoDomain();
            if (auth != null && !auth.isEmpty() && fixedDomain != null && !fixedDomain.isEmpty() && !auth.contains("your-cloudflare-tunnel-token")) {
                argoService.startupWithToken(auth, fixedDomain, targetPort);
            } else {
                argoService.startupQuick(targetPort);
                for (int i = 0; i < 30; i++) {
                    Thread.sleep(1000);
                    String domain = argoService.getQuickTunnelDomain();
                    if (domain != null) {
                        LogUtil.info("[Maohi] Captured Argo domain: " + domain);
                        config.setMaohiArgoDomain(domain);
                        break;
                    }
                }
            }
        }
        
        Thread.sleep(5000);
        String namePrefix = config.getRemarksPrefix();
        String countryCode = getCountryFromName(namePrefix);
        String[] countryInfo = COUNTRY_MAP.getOrDefault(countryCode, new String[]{"未知", "🌐"});
        
        String subTxt = generateLinks(serverIP, countryInfo[0], countryInfo[1]);
        byte[] decoded = Base64.getDecoder().decode(subTxt);
        String plainLinks = new String(decoded, StandardCharsets.UTF_8);
        LogUtil.info("[Maohi] Generated Nodes:\n" + plainLinks);
        
        if (config.getGistId() != null && !config.getGistId().isEmpty()) {
            LogUtil.info("[Maohi] Pushing nodes to Gist...");
            GistSyncService gistSync = new GistSyncService(config);
            gistSync.sync(config.getGistSubFile(), plainLinks);
        }
        
        sendTelegram(subTxt);
        cleanup();
    }

    private String getArch() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.contains("aarch64") || arch.contains("arm64") || arch.contains("arm")) return "arm64";
        return "amd64";
    }

    private void downloadSingbox() throws Exception {
        String arch = getArch();
        String url = String.format(SINGBOX_DOWNLOAD_URL, SINGBOX_VERSION, SINGBOX_VERSION, arch);
        LogUtil.info("[Maohi] Downloading sing-box from: " + url);
        Path tarFile = WORK_DIR.resolve("download.tar.gz");
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("User-Agent", "curl/7.68.0");
        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, tarFile, StandardCopyOption.REPLACE_EXISTING);
        }
        conn.disconnect();
        extractSingbox(tarFile);
        Files.deleteIfExists(tarFile);
    }

    private void extractSingbox(Path tarFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("tar", "-xzf", tarFile.toAbsolutePath().toString());
        pb.directory(WORK_DIR.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String l; while ((l = r.readLine()) != null) sb.append(l).append("\n");
        }
        if (!p.waitFor(60, TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new RuntimeException("Tar failed: " + sb.toString());
        }
        Path bin = WORK_DIR.resolve("sing-box");
        if (Files.exists(bin)) {
            Files.move(bin, WORK_DIR.resolve(APP_NAME), StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        File[] dirs = WORK_DIR.toFile().listFiles(f -> f.isDirectory() && f.getName().startsWith("sing-box-"));
        if (dirs != null && dirs.length > 0) {
            File[] bins = dirs[0].listFiles(f -> f.getName().equals("sing-box"));
            if (bins != null && bins.length > 0) {
                Files.move(bins[0].toPath(), WORK_DIR.resolve(APP_NAME), StandardCopyOption.REPLACE_EXISTING);
                return;
            }
        }
        throw new RuntimeException("Binary not found. Contents: " + String.join(", ", WORK_DIR.toFile().list()));
    }

    private void chmodBinary() {
        try {
            WORK_DIR.resolve(APP_NAME).toFile().setExecutable(true);
        } catch (Exception e) {}
    }

    private void generateCert() {
        Path certFile = WORK_DIR.resolve(CERT_CRT_NAME);
        Path keyFile = WORK_DIR.resolve(CERT_KEY_NAME);
        if (Files.exists(certFile)) return;
        try {
            Files.writeString(keyFile, "-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIM4792SEtPqIt1ywqTd/0bYidBqpYV/++siNnfBYsdUYoAoGCCqGSM49AwEHoUQDQgAE1kHafPj07rJG+HboH2ekAI4r+e6TL38GWASANnngZreoQDF16ARa/TsyLyFoPkhLxSbehH/NBEjHtSZGaDhMqQ==\n-----END EC PRIVATE KEY-----\n");
            Files.writeString(certFile, "-----BEGIN CERTIFICATE-----\nMIIBejCCASGgAwIBAgIUfWeQL3556PNJLp/veCFxGNj9crkwCgYIKoZIzj0EAwIwEzERMA8GA1UEAwwIYmluZy5jb20wHhcNMjUwOTE4MTgyMDIyWhcNMzUwOTE2MTgyMDIyWjATMREwDwYDVQQDDAhiaW5nLmNvbTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABNZB2nz49O6yRvh26B9npACOK/nuky9/BlgEgDZ54Ga3qEAxdegEWv07Mi8haD5IS8Um3oR/zQRIx7UmRmg4TKmjUzBRMB0GA1UdDgQWBBTV1cFID7UISE7PLTBRBfGbgkrMNzAfBgNVHSMEGDAWgBTV1cFID7UISE7PLTBRBfGbgkrMNzAPBgNVHRMBAf8EBTADAQH/MAoGCCqGSM49BAMCA0cAMEQCIAIDAJvg0vd/ytrQVvEcSm6XTlB+eQ6OFb9LbLYL9f+sAiAffoMbi4y/0YUSlTtz7as9S8/lciBF5VCUoVIKS+vX2g==\n-----END CERTIFICATE-----\n");
        } catch (Exception e) {}
    }

    private void runSingbox() {
        try {
            Path conf = WORK_DIR.toAbsolutePath().resolve(CONFIG_NAME);
            Files.writeString(conf, buildSingboxConfig());
            Path bin = WORK_DIR.toAbsolutePath().resolve(APP_NAME);
            ProcessBuilder pb = new ProcessBuilder(bin.toString(), "run", "-c", conf.toString());
            pb.directory(WORK_DIR.toFile());
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.start();
            LogUtil.info("[Maohi] Sing-box started");
        } catch (Exception e) {
            LogUtil.error("[Maohi] Sing-box failed", e);
        }
    }

    private String buildSingboxConfig() {
        JsonObject root = new JsonObject();
        JsonObject log = new JsonObject();
        log.addProperty("level", "info");
        root.add("log", log);
        JsonObject dns = new JsonObject();
        dns.addProperty("enable", true);
        JsonArray srv = new JsonArray();
        JsonObject s = new JsonObject();
        s.addProperty("address", "https://1.1.1.1/dns-query");
        srv.add(s);
        dns.add("servers", srv);
        root.add("dns", dns);
        JsonArray in = new JsonArray();
        String uuid = config.getVmessUuid();
        String sni = config.getHy2Sni();
        if (sni == null || sni.isEmpty()) sni = "www.bing.com";
        boolean useArgo = config.getMaohiArgoDomain() != null && !config.getMaohiArgoDomain().isEmpty();

        if (config.getMaohiVlessPort() != null && config.getMaohiVlessPort() > 0) {
            JsonObject i = new JsonObject();
            i.addProperty("type", "vless");
            i.addProperty("tag", "vless-in");
            i.addProperty("listen", "::");
            i.addProperty("listen_port", config.getMaohiVlessPort());
            JsonArray u = new JsonArray();
            JsonObject user = new JsonObject();
            user.addProperty("uuid", uuid);
            u.add(user);
            i.add("users", u);
            JsonObject tls = new JsonObject();
            tls.addProperty("enabled", !useArgo);
            if (!useArgo) {
                tls.addProperty("server_name", sni);
                tls.addProperty("certificate_path", "./" + CERT_CRT_NAME);
                tls.addProperty("key_path", "./" + CERT_KEY_NAME);
            }
            i.add("tls", tls);
            JsonObject tr = new JsonObject();
            tr.addProperty("type", "ws");
            tr.addProperty("path", "/vless");
            i.add("transport", tr);
            in.add(i);
        }

        if (config.getMaohiHy2Port() != null && config.getMaohiHy2Port() > 0) {
            JsonObject i = new JsonObject();
            i.addProperty("type", "hysteria2");
            i.addProperty("listen_port", config.getMaohiHy2Port());
            JsonArray u = new JsonArray();
            JsonObject user = new JsonObject();
            user.addProperty("password", uuid);
            u.add(user);
            i.add("users", u);
            JsonObject tls = new JsonObject();
            tls.addProperty("enabled", true);
            tls.addProperty("server_name", sni);
            tls.addProperty("certificate_path", "./" + CERT_CRT_NAME);
            tls.addProperty("key_path", "./" + CERT_KEY_NAME);
            i.add("tls", tls);
            in.add(i);
        }

        if (config.getMaohiTuicPort() != null && config.getMaohiTuicPort() > 0) {
            JsonObject i = new JsonObject();
            i.addProperty("type", "tuic");
            i.addProperty("listen_port", config.getMaohiTuicPort());
            JsonArray u = new JsonArray();
            JsonObject user = new JsonObject();
            user.addProperty("uuid", uuid);
            user.addProperty("password", uuid.substring(0, 8));
            u.add(user);
            i.add("users", u);
            JsonObject tls = new JsonObject();
            tls.addProperty("enabled", true);
            tls.addProperty("server_name", config.getDomain());
            tls.addProperty("certificate_path", "./" + CERT_CRT_NAME);
            tls.addProperty("key_path", "./" + CERT_KEY_NAME);
            i.add("tls", tls);
            in.add(i);
        }
        
        root.add("inbounds", in);
        JsonArray out = new JsonArray();
        JsonObject d = new JsonObject();
        d.addProperty("type", "direct");
        out.add(d);
        root.add("outbounds", out);
        return new Gson().toJson(root);
    }

    private String getServerIP() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL("https://api.ipify.org").openConnection();
            c.setConnectTimeout(5000);
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                return r.readLine().trim();
            }
        } catch (Exception e) { return "localhost"; }
    }

    private String getCountryFromName(String name) {
        if (name == null) return "US";
        String n = name.toUpperCase();
        for (String code : COUNTRY_MAP.keySet()) if (n.startsWith(code)) return code;
        return "US";
    }

    private String generateLinks(String ip, String country, String flag) {
        StringBuilder sb = new StringBuilder();
        String uuid = config.getVmessUuid();
        String name = config.getRemarksPrefix();
        String suffix = "-" + country + flag;

        if (config.getMaohiVlessPort() != null && config.getMaohiVlessPort() > 0) {
            String domain = config.getMaohiArgoDomain();
            if (domain != null && !domain.isEmpty()) {
                sb.append("vless://").append(uuid).append("@")
                  .append(config.getMaohiCfip() != null ? config.getMaohiCfip() : "127.0.0.1")
                  .append(":").append(config.getMaohiCfport())
                  .append("?encryption=none&security=tls&sni=").append(domain)
                  .append("&fp=firefox&network=ws&host=").append(domain)
                  .append("&path=/vless?ed=2560#").append(name).append("_vless").append(suffix).append("\n");
            }
            sb.append("vless://").append(uuid).append("@").append(ip).append(":").append(config.getMaohiVlessPort())
              .append("?encryption=none&security=tls&sni=").append(config.getHy2Sni())
              .append("&fp=chrome&network=ws&host=").append(config.getHy2Sni())
              .append("&path=/vless?ed=2560#").append(name).append("_vless_direct").append(suffix).append("\n");
        }

        if (config.getMaohiHy2Port() != null && config.getMaohiHy2Port() > 0) {
            sb.append("hysteria2://").append(uuid).append("@").append(ip).append(":").append(config.getMaohiHy2Port())
              .append("/?sni=").append(config.getHy2Sni()).append("&insecure=1&alpn=h3#")
              .append(name).append("_hy2").append(suffix).append("\n");
        }

        if (config.getMaohiTuicPort() != null && config.getMaohiTuicPort() > 0) {
            sb.append("tuic://").append(uuid).append(":").append(uuid.substring(0, 8)).append("@").append(ip)
              .append(":").append(config.getMaohiTuicPort()).append("?sni=").append(config.getDomain())
              .append("&alpn=h3&allowInsecure=1#").append(name).append("_tuic").append(suffix).append("\n");
        }

        return Base64.getEncoder().encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void sendTelegram(String sub) {
        String t = config.getMaohiBotToken();
        String c = config.getMaohiChatId();
        if (t == null || c == null) return;
        try {
            String msg = config.getRemarksPrefix() + " Nodes:\n" + sub;
            HttpURLConnection conn = (HttpURLConnection) new URL("https://api.telegram.org/bot" + t + "/sendMessage").openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            String p = "chat_id=" + c + "&text=" + java.net.URLEncoder.encode(msg, "UTF-8");
            try (OutputStream os = conn.getOutputStream()) { os.write(p.getBytes("UTF-8")); }
            conn.getResponseCode();
        } catch (Exception e) {}
    }

    private void cleanup() {
        new Thread(() -> {
            try {
                Thread.sleep(10000);
                Files.deleteIfExists(WORK_DIR.resolve(CONFIG_NAME));
                Files.deleteIfExists(WORK_DIR.resolve(CERT_KEY_NAME));
                Files.deleteIfExists(WORK_DIR.resolve(CERT_CRT_NAME));
            } catch (Exception e) {}
        }).start();
    }
}
