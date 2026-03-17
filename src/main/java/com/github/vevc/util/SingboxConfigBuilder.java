package com.github.vevc.util;

import com.github.vevc.config.AppConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sing-box configuration builder
 * Generates sing-box config.json and share links
 * @author zv
 */
public class SingboxConfigBuilder {

    private final AppConfig config;
    private final String nodePrefix;
    private final JsonArray inbounds = new JsonArray();
    private final JsonArray outbounds = new JsonArray();
    private final JsonObject dns = new JsonObject();
    private final JsonObject route = new JsonObject();

    public SingboxConfigBuilder(AppConfig config) {
        this.config = config;
        this.nodePrefix = config.getRemarksPrefix();
    }

    /**
     * Build complete sing-box configuration JSON
     */
    public String build() {
        JsonObject root = new JsonObject();

        // Log config (silent mode)
        JsonObject log = new JsonObject();
        log.addProperty("level", "off");
        root.add("log", log);

        // DNS config
        buildDnsConfig();
        root.add("dns", dns);

        // Build inbounds for each enabled protocol
        if (config.isProtocolEnabled("hysteria2")) {
            inbounds.add(buildHysteria2Inbound());
        }
        if (config.isProtocolEnabled("vmess-ws")) {
            inbounds.add(buildVmessWsInbound());
        }
        if (config.isProtocolEnabled("anytls")) {
            inbounds.add(buildAnytlsInbound());
        }
        if (config.isProtocolEnabled("tuic")) {
            inbounds.add(buildTuicInbound());
        }

        root.add("inbounds", inbounds);

        // Outbounds
        buildOutbounds();
        root.add("outbounds", outbounds);

        // Route
        buildRoute();
        root.add("route", route);

        return new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root);
    }

    private void buildDnsConfig() {
        dns.addProperty("enable", true);

        JsonArray servers = new JsonArray();
        JsonObject dnsServer = new JsonObject();
        dnsServer.addProperty("address", "https://1.1.1.1/dns-query");
        dnsServer.addProperty("detour", "direct");
        servers.add(dnsServer);
        dns.add("servers", servers);
        
        // New DNS rule format
        JsonArray rules = new JsonArray();
        dns.add("rules", rules);
    }

    private void buildOutbounds() {
        // Direct
        JsonObject direct = new JsonObject();
        direct.addProperty("type", "direct");
        direct.addProperty("tag", "direct");
        outbounds.add(direct);

        // Block
        JsonObject block = new JsonObject();
        block.addProperty("type", "block");
        block.addProperty("tag", "block");
        outbounds.add(block);
    }

    private void buildRoute() {
        route.addProperty("final", "direct");

        JsonArray rules = new JsonArray();

        // New route action format for DNS
        JsonObject dnsRule = new JsonObject();
        dnsRule.addProperty("protocol", "dns");
        dnsRule.addProperty("action", "hijack-dns");
        rules.add(dnsRule);

        route.add("rules", rules);
    }

    private JsonObject buildHysteria2Inbound() {
        JsonObject inbound = new JsonObject();
        inbound.addProperty("type", "hysteria2");
        inbound.addProperty("tag", "hy2-in");
        inbound.addProperty("listen", "::");
        inbound.addProperty("listen_port", config.getHy2Port());
        inbound.addProperty("up_mbps", config.getHy2UpMbps());
        inbound.addProperty("down_mbps", config.getHy2DownMbps());

        // User authentication
        JsonArray users = new JsonArray();
        JsonObject user = new JsonObject();
        user.addProperty("password", config.getHy2Password());
        users.add(user);
        inbound.add("users", users);

        // Obfuscation (optional)
        if (config.getHy2ObfsPassword() != null && !config.getHy2ObfsPassword().isEmpty()) {
            JsonObject obfs = new JsonObject();
            obfs.addProperty("type", "salamander");
            obfs.addProperty("password", config.getHy2ObfsPassword());
            inbound.add("obfs", obfs);
        }

        // TLS config (self-signed)
        JsonObject tls = buildSelfSignTls(config.getHy2Sni());
        inbound.add("tls", tls);

        // Masquerade (Must be a string URL in sing-box Hysteria2)
        inbound.addProperty("masquerade", "https://itunes.apple.com");

        return inbound;
    }

    private JsonObject buildVmessWsInbound() {
        JsonObject inbound = new JsonObject();
        inbound.addProperty("type", "vmess");
        inbound.addProperty("tag", "vmess-in");
        inbound.addProperty("listen", "::");
        inbound.addProperty("listen_port", config.getVmessPort());

        // User authentication
        JsonArray users = new JsonArray();
        JsonObject user = new JsonObject();
        user.addProperty("uuid", config.getVmessUuid());
        users.add(user);
        inbound.add("users", users);

        // TLS
        JsonObject tls = buildSelfSignTls(config.getDomain());
        inbound.add("tls", tls);

        // WebSocket transport
        JsonObject transport = new JsonObject();
        transport.addProperty("type", "ws");
        transport.addProperty("path", config.getVmessPath());
        transport.addProperty("early_data_header_name", "Sec-WebSocket-Protocol");
        inbound.add("transport", transport);

        return inbound;
    }

    private JsonObject buildAnytlsInbound() {
        JsonObject inbound = new JsonObject();
        inbound.addProperty("type", "anytls");
        inbound.addProperty("tag", "anytls-in");
        inbound.addProperty("listen", "::");
        inbound.addProperty("listen_port", config.getAnytlsPort());

        // User authentication
        JsonArray users = new JsonArray();
        JsonObject user = new JsonObject();
        user.addProperty("password", config.getAnytlsPassword());
        users.add(user);
        inbound.add("users", users);

        // TLS
        JsonObject tls = buildSelfSignTls(config.getAnytlsSni());
        inbound.add("tls", tls);

        return inbound;
    }

    private JsonObject buildTuicInbound() {
        JsonObject inbound = new JsonObject();
        inbound.addProperty("type", "tuic");
        inbound.addProperty("tag", "tuic-in");
        inbound.addProperty("listen", "::");
        inbound.addProperty("listen_port", config.getTuicPort());
        inbound.addProperty("congestion_control", "bbr");

        JsonArray users = new JsonArray();
        JsonObject user = new JsonObject();
        user.addProperty("name", "user1");
        user.addProperty("uuid", config.getTuicUuid());
        user.addProperty("password", config.getTuicPassword());
        users.add(user);
        inbound.add("users", users);

        JsonObject tls = new JsonObject();
        tls.addProperty("enabled", true);
        tls.addProperty("server_name", config.getDomain());
        tls.addProperty("certificate_path", "javacore.txt");
        tls.addProperty("key_path", "heapdump.hprof");

        JsonArray alpn = new JsonArray();
        alpn.add("h3");
        tls.add("alpn", alpn);

        inbound.add("tls", tls);

        return inbound;
    }

    private JsonObject buildSelfSignTls(String sni) {
        JsonObject tls = new JsonObject();
        tls.addProperty("enabled", true);
        tls.addProperty("server_name", sni);

        // Use actual certificate files (generated by openssl)
        // Files: heapdump.hprof (key), javacore.txt (cert)
        tls.addProperty("certificate_path", "javacore.txt");
        tls.addProperty("key_path", "heapdump.hprof");

        // ALPN
        JsonArray alpn = new JsonArray();
        alpn.add("h3");
        alpn.add("h2");
        alpn.add("http/1.1");
        tls.add("alpn", alpn);

        return tls;
    }

    private void buildOutbounds() {
        // Direct
        JsonObject direct = new JsonObject();
        direct.addProperty("type", "direct");
        direct.addProperty("tag", "direct");
        outbounds.add(direct);

        // Block
        JsonObject block = new JsonObject();
        block.addProperty("type", "block");
        block.addProperty("tag", "block");
        outbounds.add(block);

        // DNS
        JsonObject dnsOut = new JsonObject();
        dnsOut.addProperty("type", "dns");
        dnsOut.addProperty("tag", "dns-out");
        outbounds.add(dnsOut);
    }

    private void buildRoute() {
        route.addProperty("final", "direct");

        JsonArray rules = new JsonArray();

        JsonObject dnsRule = new JsonObject();
        dnsRule.addProperty("protocol", "dns");
        dnsRule.addProperty("outbound", "dns-out");
        rules.add(dnsRule);

        route.add("rules", rules);
    }

    /**
     * Generate node name with prefix
     * Format: {Prefix}-zv-{Protocol}
     * Example: JP-zv-hysteria2, US-zv-vmess-ws
     */
    private String generateNodeName(String protocol) {
        return nodePrefix + "-zv-" + protocol;
    }

    /**
     * Generate share links for all enabled protocols
     */
    public Map<String, String> generateShareLinks(String serverIp) {
        Map<String, String> links = new LinkedHashMap<>();

        if (config.isProtocolEnabled("hysteria2")) {
            links.put("hysteria2", buildHysteria2Link(serverIp));
        }
        if (config.isProtocolEnabled("vmess-ws")) {
            links.put("vmess-ws", buildVmessWsLink(serverIp));
        }
        if (config.isProtocolEnabled("anytls")) {
            links.put("anytls", buildAnytlsLink(serverIp));
        }
        if (config.isProtocolEnabled("tuic")) {
            links.put("tuic", buildTuicLink(serverIp));
        }
        // Argo tunnel support
        if (config.getArgoEnabled() && config.getArgoHostname() != null) {
            links.put("argo", buildArgoLink());
        }

        return links;
    }

    /**
     * Generate subscription file names with prefix
     * Format: {Prefix}-zv-{Protocol}
     */
    public Map<String, String> generateFileNames() {
        Map<String, String> fileNames = new LinkedHashMap<>();

        if (config.isProtocolEnabled("hysteria2")) {
            fileNames.put("hysteria2", generateNodeName("hysteria2"));
        }
        if (config.isProtocolEnabled("vmess-ws")) {
            fileNames.put("vmess-ws", generateNodeName("vmess-ws"));
        }
        if (config.isProtocolEnabled("anytls")) {
            fileNames.put("anytls", generateNodeName("anytls"));
        }
        if (config.isProtocolEnabled("tuic")) {
            fileNames.put("tuic", generateNodeName("tuic"));
        }
        if (config.getArgoEnabled() && config.getArgoHostname() != null) {
            fileNames.put("argo", generateNodeName("argo"));
        }

        return fileNames;
    }

    private String buildHysteria2Link(String serverIp) {
        // hysteria2://password@server:port?sni=xxx&insecure=1#name
        String nodeName = generateNodeName("hy2");
        return String.format("hysteria2://%s@%s:%d?sni=%s&insecure=1#%s",
                config.getHy2Password(),
                serverIp,
                config.getHy2Port(),
                config.getHy2Sni(),
                nodeName
        );
    }

    private String buildVmessWsLink(String serverIp) {
        // vmess://base64(JSON)
        String nodeName = generateNodeName("vmess");
        JsonObject vmess = new JsonObject();
        vmess.addProperty("v", "2");
        vmess.addProperty("ps", nodeName);
        vmess.addProperty("add", serverIp);
        vmess.addProperty("port", config.getVmessPort());
        vmess.addProperty("id", config.getVmessUuid());
        vmess.addProperty("aid", 0);
        vmess.addProperty("net", "ws");
        vmess.addProperty("type", "none");
        vmess.addProperty("host", config.getDomain());
        vmess.addProperty("path", config.getVmessPath());
        vmess.addProperty("tls", "tls");
        vmess.addProperty("sni", config.getDomain());
        vmess.addProperty("allowInsecure", 1);

        String json = vmess.toString();
        return "vmess://" + Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private String buildAnytlsLink(String serverIp) {
        // anytls://password@server:port?sni=xxx&insecure=1#name
        String nodeName = generateNodeName("anytls");
        return String.format("anytls://%s@%s:%d?sni=%s&insecure=1#%s",
                config.getAnytlsPassword(),
                serverIp,
                config.getAnytlsPort(),
                config.getAnytlsSni(),
                nodeName
        );
    }

    private String buildTuicLink(String serverIp) {
        // tuic://uuid:password@server:port?sni=xxx&alpn=h3&congestion_control=bbr#name
        String nodeName = generateNodeName("tuic");
        return String.format("tuic://%s:%s@%s:%d?sni=%s&alpn=h3&congestion_control=bbr&allowInsecure=1#%s",
                config.getTuicUuid(),
                config.getTuicPassword(),
                serverIp,
                config.getTuicPort(),
                config.getDomain(),
                nodeName
        );
    }

    /**
     * Build Argo tunnel subscription link
     * Argo uses Cloudflare tunnel, so hostname is provided by Cloudflare
     */
    private String buildArgoLink() {
        // For Argo tunnel, we output configuration info
        // The actual connection is via Cloudflare network
        String nodeName = generateNodeName("argo");
        String hostname = config.getArgoHostname();
        
        // Return a config line that can be used with cloudflared or sing-box
        // Format: argo://hostname?token=xxx#name
        return String.format("argo://%s?token=%s#%s",
                hostname,
                config.getArgoToken() != null ? config.getArgoToken() : "",
                nodeName
        );
    }

    public String getNodePrefix() {
        return nodePrefix;
    }
}
