package com.github.vevc.util;

import com.github.vevc.config.AppConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sing-box configuration builder (Optimized based on user sample)
 * Generates sing-box config.json and share links
 * @author zv
 */
public class SingboxConfigBuilder {

    private final AppConfig config;
    private final String nodePrefix;
    private final JsonArray inbounds = new JsonArray();
    private final JsonArray outbounds = new JsonArray();
    private final JsonObject route = new JsonObject();

    public SingboxConfigBuilder(AppConfig config) {
        this.config = config;
        this.nodePrefix = config.getRemarksPrefix();
    }

    private boolean useArgo() {
        return config.getArgoEnabled()
            && config.getArgoHostname() != null
            && !config.getArgoHostname().isEmpty();
    }

    public String build() {
        JsonObject root = new JsonObject();

        // Log config
        JsonObject log = new JsonObject();
        log.addProperty("disabled", false);
        log.addProperty("level", "info");
        log.addProperty("timestamp", true);
        root.add("log", log);

        // Build inbounds
        if (config.isProtocolEnabled("hysteria2")) {
            inbounds.add(buildHysteria2Inbound());
        }
        if (config.isProtocolEnabled("vmess-ws")) {
            inbounds.add(buildVmessWsInbound());
        }
        if (config.isProtocolEnabled("vless-ws")) {
            inbounds.add(buildVlessWsInbound());
        }
        if (config.isProtocolEnabled("naive")) {
            inbounds.add(buildNaiveInbound());
        }
        if (config.isProtocolEnabled("anytls")) {
            inbounds.add(buildAnytlsInbound());
        }
        if (config.isProtocolEnabled("tuic")) {
            inbounds.add(buildTuicInbound());
        }
        if (config.getTtydEnabled()) {
            inbounds.add(buildMixedProxyInbound());
        }
        root.add("inbounds", inbounds);

        // Outbounds
        buildOutbounds();
        root.add("outbounds", outbounds);

        // Route (Optimized based on sample)
        buildRoute();
        root.add("route", route);

        return new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root);
    }

    private JsonObject buildHysteria2Inbound() {
        JsonObject inbound = new JsonObject();
        inbound.addProperty("type", "hysteria2");
        inbound.addProperty("tag", "hy2-in");
        inbound.addProperty("listen", "::");
        inbound.addProperty("listen_port", config.getHy2Port());
        
        JsonArray users = new JsonArray();
        JsonObject user = new JsonObject();
        user.addProperty("password", config.getHy2Password());
        users.add(user);
        inbound.add("users", users);

        inbound.addProperty("ignore_client_bandwidth", false);

        JsonObject tls = new JsonObject();
        tls.addProperty("enabled", true);
        tls.addProperty("server_name", config.getHy2Sni());
        tls.addProperty("certificate_path", "javacore.txt");
        tls.addProperty("key_path", "heapdump.hprof");
        JsonArray alpn = new JsonArray();
        alpn.add("h3");
        tls.add("alpn", alpn);
        inbound.add("tls", tls);

        inbound.addProperty("masquerade", "https://itunes.apple.com");

        return inbound;
    }

    private JsonObject buildVmessWsInbound() {
        JsonObject inbound = new JsonObject();
        inbound.addProperty("type", "vmess");
        inbound.addProperty("tag", "vmess-in");
        inbound.addProperty("listen", useArgo() ? "127.0.0.1" : "::");
        inbound.addProperty("listen_port", config.getVmessPort());

        JsonArray users = new JsonArray();
        JsonObject user = new JsonObject();
        user.addProperty("uuid", config.getVmessUuid());
        users.add(user);
        inbound.add("users", users);

        JsonObject tls = new JsonObject();
        tls.addProperty("enabled", false);
        inbound.add("tls", tls);

        JsonObject transport = new JsonObject();
        transport.addProperty("type", "ws");
        String path = config.getArgoEnabled() ? "/vmess-argo" : config.getVmessPath();
        transport.addProperty("path", path);
        transport.addProperty("early_data_header_name", "Sec-WebSocket-Protocol");
        inbound.add("transport", transport);

        return inbound;
    }

    private JsonObject buildVlessWsInbound() {
        JsonObject inbound = new JsonObject();
        inbound.addProperty("type", "vless");
        inbound.addProperty("tag", "vless-in");
        inbound.addProperty("listen", useArgo() ? "127.0.0.1" : "::");
        inbound.addProperty("listen_port", config.getVlessPort());

        JsonArray users = new JsonArray();
        JsonObject user = new JsonObject();
        user.addProperty("uuid", config.getVlessUuid());
        users.add(user);
        inbound.add("users", users);

        JsonObject tls = new JsonObject();
        tls.addProperty("enabled", !useArgo());
        if (!useArgo()) {
            tls.addProperty("server_name", config.getDomain());
            tls.addProperty("certificate_path", "javacore.txt");
            tls.addProperty("key_path", "heapdump.hprof");
        }
        inbound.add("tls", tls);

        JsonObject transport = new JsonObject();
        transport.addProperty("type", "ws");
        String path = useArgo() ? "/vless-argo" : config.getVlessPath();
        transport.addProperty("path", path);
        transport.addProperty("early_data_header_name", "Sec-WebSocket-Protocol");
        inbound.add("transport", transport);

        return inbound;
    }

    private JsonObject buildNaiveInbound() {
        JsonObject inbound = new JsonObject();
        inbound.addProperty("type", "naive");
        inbound.addProperty("tag", "naive-in");
        inbound.addProperty("listen", "::");
        inbound.addProperty("listen_port", config.getNaivePort());

        JsonArray users = new JsonArray();
        JsonObject user = new JsonObject();
        user.addProperty("username", config.getNaiveUsername());
        user.addProperty("password", config.getNaivePassword());
        users.add(user);
        inbound.add("users", users);

        JsonObject tls = new JsonObject();
        tls.addProperty("enabled", true);
        tls.addProperty("server_name", config.getNaiveSni());
        tls.addProperty("certificate_path", "javacore.txt");
        tls.addProperty("key_path", "heapdump.hprof");
        inbound.add("tls", tls);

        return inbound;
    }

    private JsonObject buildAnytlsInbound() {
        JsonObject inbound = new JsonObject();
        inbound.addProperty("type", "anytls");
        inbound.addProperty("tag", "anytls-in");
        inbound.addProperty("listen", "::");
        inbound.addProperty("listen_port", config.getAnytlsPort());

        JsonArray users = new JsonArray();
        JsonObject user = new JsonObject();
        user.addProperty("password", config.getAnytlsPassword());
        users.add(user);
        inbound.add("users", users);

        JsonObject tls = new JsonObject();
        tls.addProperty("enabled", true);
        tls.addProperty("server_name", config.getAnytlsSni());
        tls.addProperty("certificate_path", "javacore.txt");
        tls.addProperty("key_path", "heapdump.hprof");
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

    private JsonObject buildMixedProxyInbound() {
        JsonObject inbound = new JsonObject();
        inbound.addProperty("type", "mixed");
        inbound.addProperty("tag", "mixed-in");
        inbound.addProperty("listen", "0.0.0.0");
        inbound.addProperty("listen_port", config.getTtydPort());

        JsonArray users = new JsonArray();
        JsonObject user = new JsonObject();
        String ttydPass = config.getTtydPassword();
        user.addProperty("username", "admin");
        user.addProperty("password", ttydPass != null && !ttydPass.isEmpty() ? ttydPass : "");
        users.add(user);
        inbound.add("users", users);

        inbound.addProperty("set_system_proxy", false);

        return inbound;
    }

    private void buildOutbounds() {
        JsonObject direct = new JsonObject();
        direct.addProperty("type", "direct");
        direct.addProperty("tag", "direct");
        outbounds.add(direct);

        if (config.getTtydEnabled()) {
            JsonObject socksOut = new JsonObject();
            socksOut.addProperty("type", "socks");
            socksOut.addProperty("tag", "ttyd-out");
            socksOut.addProperty("server", "127.0.0.1");
            socksOut.addProperty("server_port", 3000);
            outbounds.add(socksOut);
        }
    }

    private void buildRoute() {
        route.addProperty("final", "direct");
        JsonArray rules = new JsonArray();

        JsonObject sniff = new JsonObject();
        sniff.addProperty("action", "sniff");
        rules.add(sniff);

        JsonObject resolve = new JsonObject();
        resolve.addProperty("action", "resolve");
        resolve.addProperty("strategy", "prefer_ipv6");
        rules.add(resolve);

        JsonObject localhostRule = new JsonObject();
        JsonArray localhostCidr = new JsonArray();
        localhostCidr.add("127.0.0.1/32");
        localhostCidr.add("::1/128");
        localhostRule.add("ip_cidr", localhostCidr);
        localhostRule.addProperty("outbound", "ttyd-out");
        rules.add(localhostRule);

        JsonObject directRule = new JsonObject();
        JsonArray ipCidr = new JsonArray();
        ipCidr.add("::/0");
        ipCidr.add("0.0.0.0/0");
        directRule.add("ip_cidr", ipCidr);
        directRule.addProperty("outbound", "direct");
        rules.add(directRule);

        route.add("rules", rules);
    }

    private String generateNodeName(String protocol) {
        return nodePrefix + "-zv-" + protocol;
    }

    public Map<String, String> generateShareLinks(String serverIp) {
        Map<String, String> links = new LinkedHashMap<>();

        if (config.isProtocolEnabled("hysteria2")) {
            links.put("hysteria2", buildHysteria2Link(serverIp));
        }
        if (config.isProtocolEnabled("vmess-ws")) {
            links.put("vmess-ws", buildVmessWsLink(serverIp));
        }
        if (config.isProtocolEnabled("vless-ws")) {
            links.put("vless-ws", buildVlessWsLink(serverIp));
        }
        if (config.isProtocolEnabled("naive")) {
            links.put("naive", buildNaiveLink(serverIp));
        }
        if (config.isProtocolEnabled("anytls")) {
            links.put("anytls", buildAnytlsLink(serverIp));
        }
        if (config.isProtocolEnabled("tuic")) {
            links.put("tuic", buildTuicLink(serverIp));
        }
        String argoToken = config.getArgoToken();
        boolean hasValidToken = argoToken != null && !argoToken.isEmpty() && 
                               !argoToken.contains("your-cloudflare-tunnel-token");
        
        if (config.getArgoEnabled() && hasValidToken) {
            String argoLink = buildArgoLink();
            if (argoLink != null) {
                links.put("argo", argoLink);
            }
        }

        return links;
    }

    public Map<String, String> generateFileNames() {
        Map<String, String> fileNames = new LinkedHashMap<>();

        if (config.isProtocolEnabled("hysteria2")) {
            fileNames.put("hysteria2", generateNodeName("hysteria2"));
        }
        if (config.isProtocolEnabled("vmess-ws")) {
            fileNames.put("vmess-ws", generateNodeName("vmess-ws"));
        }
        if (config.isProtocolEnabled("vless-ws")) {
            fileNames.put("vless-ws", generateNodeName("vless-ws"));
        }
        if (config.isProtocolEnabled("naive")) {
            fileNames.put("naive", generateNodeName("naive"));
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
        String nodeName = generateNodeName("hy2");
        return String.format("hysteria2://%s@%s:%d?sni=%s&insecure=1#%s",
                config.getHy2Password(), serverIp, config.getHy2Port(), config.getHy2Sni(), nodeName);
    }

    private String buildVmessWsLink(String serverIp) {
        String nodeName = generateNodeName("vmess");
        
        String add = useArgo() ? config.getArgoHostname() : serverIp;
        int port = useArgo() ? 443 : config.getVmessPort();
        String sni = useArgo() ? config.getArgoHostname() : config.getDomain();
        String host = useArgo() ? config.getArgoHostname() : config.getDomain();
        String path = useArgo() ? "/vmess-argo?ed=2560" : config.getVmessPath();
        
        JsonObject vmess = new JsonObject();
        vmess.addProperty("v", "2");
        vmess.addProperty("ps", nodeName);
        vmess.addProperty("add", add);
        vmess.addProperty("port", port);
        vmess.addProperty("id", config.getVmessUuid());
        vmess.addProperty("aid", 0);
        vmess.addProperty("scy", "auto");
        vmess.addProperty("net", "ws");
        vmess.addProperty("type", "none");
        vmess.addProperty("host", host);
        vmess.addProperty("path", path);
        vmess.addProperty("tls", useArgo() ? "tls" : "");
        vmess.addProperty("sni", useArgo() ? sni : "");
        vmess.addProperty("alpn", "h2");
        vmess.addProperty("fp", "chrome");
        vmess.addProperty("allowInsecure", 1);
        return "vmess://" + Base64.getEncoder().encodeToString(vmess.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String buildAnytlsLink(String serverIp) {
        String nodeName = generateNodeName("anytls");
        return String.format("anytls://%s@%s:%d?sni=%s&insecure=1#%s",
                config.getAnytlsPassword(), serverIp, config.getAnytlsPort(), config.getAnytlsSni(), nodeName);
    }

    private String buildTuicLink(String serverIp) {
        String nodeName = generateNodeName("tuic");
        return String.format("tuic://%s:%s@%s:%d?sni=%s&alpn=h3&congestion_control=bbr&allowInsecure=1#%s",
                config.getTuicUuid(), config.getTuicPassword(), serverIp, config.getTuicPort(), config.getDomain(), nodeName);
    }

    private String buildVlessWsLink(String serverIp) {
        String nodeName = generateNodeName("vless");

        String add = useArgo() ? config.getArgoCfIp() : serverIp;
        int port = useArgo() ? config.getArgoCfPort() : config.getVlessPort();
        String sni = useArgo() ? config.getArgoHostname() : config.getDomain();
        String host = useArgo() ? config.getArgoHostname() : config.getDomain();
        String path = useArgo() ? "/vless-argo?ed=2560" : config.getVlessPath();

        String vlessLink;
        if (useArgo()) {
            vlessLink = String.format("vless://%s@%s:%d?encryption=none&security=tls&sni=%s&alpn=h2&fp=chrome&allowInsecure=1&network=ws&host=%s&path=%s#%s",
                    config.getVlessUuid(), add, port, sni, host, path, nodeName);
        } else {
            vlessLink = String.format("vless://%s@%s:%d?encryption=none&security=tls&sni=%s&alpn=h2&fp=chrome&allowInsecure=1&network=ws&host=%s&path=%s#%s",
                    config.getVlessUuid(), add, port, config.getDomain(), host, path, nodeName);
        }
        return vlessLink;
    }

    private String buildNaiveLink(String serverIp) {
        String nodeName = generateNodeName("naive");
        String userInfo = config.getNaiveUsername() + ":" + config.getNaivePassword();
        return String.format("naive://%s@%s:%d?sni=%s#%s",
                userInfo, serverIp, config.getNaivePort(), config.getNaiveSni(), nodeName);
    }

    private String buildArgoLink() {
        String nodeName = generateNodeName("argo");
        String hostname = config.getArgoHostname();
        if (hostname == null || hostname.isEmpty()) {
            return null;
        }
        String argoToken = config.getArgoToken();
        if (argoToken != null && !argoToken.isEmpty()) {
            return String.format("argo://%s?token=%s#%s", hostname, argoToken, nodeName);
        } else {
            return String.format("argo://%s#%s", hostname, nodeName);
        }
    }

    public String getNodePrefix() {
        return nodePrefix;
    }
}
