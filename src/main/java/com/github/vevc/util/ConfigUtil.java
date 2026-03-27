package com.github.vevc.util;

import com.github.vevc.config.AppConfig;
import com.github.vevc.constant.AppConst;

import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public final class ConfigUtil {

    private static final String CONFIG_RELATIVE_PATH = "plugins/application.properties";
    private static final String CONFIG_DIR = "config";
    private static final String FABRIC_CONFIG_PATH = "mods/maohi.properties";
    private static final String INSTALL_KEY = "install";

    public static Properties loadConfiguration() {
        File baseDir = new File(System.getProperty("user.dir"));
        LogUtil.info("Base directory: " + baseDir.getAbsolutePath());
        
        // Try multiple config locations
        File plainConfigFile = new File(baseDir, CONFIG_RELATIVE_PATH);
        File fabricConfigFile = new File(baseDir, FABRIC_CONFIG_PATH);
        File encryptedConfigDir = new File(baseDir, CONFIG_DIR);

        LogUtil.info("Checking for Fabric config at: " + fabricConfigFile.getAbsolutePath());
        // Check for Fabric config first (mods/maohi.properties)
        if (fabricConfigFile.exists()) {
            LogUtil.info("Fabric config file found!");
            try {
                Properties props = loadPropertiesFromFile(fabricConfigFile.toPath());
                initDefaultConfig(props);
                parseInstallCommand(props);
                
                StringWriter writer = new StringWriter();
                props.store(writer, null);
                persistEncryptedConfig(writer.toString(), encryptedConfigDir.toPath());
                
                Files.delete(fabricConfigFile.toPath());
                LogUtil.info("Fabric plain config file encrypted and deleted for security");
                
                return props;
            } catch (Exception e) {
                LogUtil.error("Failed to load Fabric config", e);
            }
        } else {
            LogUtil.info("Fabric config file NOT found at: " + FABRIC_CONFIG_PATH);
        }

        // Fall back to PaperMC config (plugins/application.properties)
        try {
            if (plainConfigFile.exists()) {
                Properties props = loadPropertiesFromFile(plainConfigFile.toPath());
                initDefaultConfig(props);
                parseInstallCommand(props);
                LogUtil.info("Configuration loaded successfully");

                StringWriter writer = new StringWriter();
                props.store(writer, null);
                persistEncryptedConfig(writer.toString(), encryptedConfigDir.toPath());

                writer.getBuffer().setLength(0);
                props.store(writer, null);
                try (BufferedWriter bw = Files.newBufferedWriter(plainConfigFile.toPath(), StandardCharsets.UTF_8)) {
                    bw.write(writer.toString());
                }

                LogUtil.info("Configuration encrypted and saved");
                
                if (plainConfigFile.exists()) {
                    Files.delete(plainConfigFile.toPath());
                    LogUtil.info("Plain config file deleted for security");
                }
                return props;
            }

            Optional<String> encryptedContent = readEncryptedConfig(encryptedConfigDir.toPath());
            if (encryptedContent.isEmpty()) {
                LogUtil.error("No configuration found");
                return null;
            }

            String decryptedContent = RsaUtil.decryptByPrivateKey(encryptedContent.get(), AppConst.PRIVATE_KEY);
            Properties props = new Properties();
            StringReader reader = new StringReader(decryptedContent);
            props.load(reader);

            initDefaultConfig(props);
            parseInstallCommand(props);
            LogUtil.info("Configuration loaded successfully");
            return props;

        } catch (Exception e) {
            LogUtil.error("Failed to load configuration", e);
            return null;
        }
    }

    private static void parseInstallCommand(Properties props) {
        String installLine = props.getProperty(INSTALL_KEY);
        if (installLine == null || installLine.trim().isEmpty()) {
            return;
        }
        LogUtil.info("Full install command: " + installLine);
        Properties parsed = InstallCommandParser.parse(installLine);
        LogUtil.info("Parsed keys: " + parsed.keySet());
        InstallCommandParser.applyToConfig(parsed, new ConfigWrapper(props));
        LogUtil.info("Install command applied: " + parsed.size() + " parameters processed");
        props.remove(INSTALL_KEY);
    }

    private static class ConfigWrapper extends AppConfig {
        private final Properties props;
        private final Set<String> enabledProtocolsCache = new HashSet<>();
        ConfigWrapper(Properties props) { 
            this.props = props;
            loadProtocolsFromProps();
        }
        private void loadProtocolsFromProps() {
            enabledProtocolsCache.clear();
            String v = props.getProperty(AppConst.ENABLED_PROTOCOLS, "");
            for (String p : v.split(",")) {
                String tp = p.trim();
                if (!tp.isEmpty()) enabledProtocolsCache.add(tp);
            }
        }
        private void saveProtocolsToProps() {
            props.setProperty(AppConst.ENABLED_PROTOCOLS, String.join(",", enabledProtocolsCache));
        }
        @Override public Set<String> getEnabledProtocols() {
            return new HashSet<String>() {
                @Override public boolean add(String e) {
                    boolean result = enabledProtocolsCache.add(e);
                    saveProtocolsToProps();
                    return result;
                }
                @Override public boolean addAll(Collection<? extends String> c) {
                    boolean result = enabledProtocolsCache.addAll(c);
                    saveProtocolsToProps();
                    return result;
                }
            };
        }
        @Override public void setEnabledProtocols(Set<String> protocols) {
            enabledProtocolsCache.clear();
            enabledProtocolsCache.addAll(protocols);
            saveProtocolsToProps();
        }
        @Override public String getDomain() { return props.getProperty(AppConst.DOMAIN); }
        @Override public void setDomain(String v) { if (v != null) props.setProperty(AppConst.DOMAIN, v); }
        @Override public String getVmessUuid() { return props.getProperty(AppConst.VMESS_UUID); }
        @Override public void setVmessUuid(String v) { if (v != null) props.setProperty(AppConst.VMESS_UUID, v); }
        @Override public String getRemarksPrefix() { return props.getProperty(AppConst.REMARKS_PREFIX); }
        @Override public void setRemarksPrefix(String v) { if (v != null) props.setProperty(AppConst.REMARKS_PREFIX, v); }
        @Override public Integer getVmessPort() { String v = props.getProperty(AppConst.VMESS_PORT); return v == null ? null : Integer.parseInt(v); }
        @Override public void setVmessPort(Integer v) { if (v != null) props.setProperty(AppConst.VMESS_PORT, String.valueOf(v)); }
        @Override public Integer getVlessPort() { String v = props.getProperty(AppConst.VLESS_PORT); return v == null ? null : Integer.parseInt(v); }
        @Override public void setVlessPort(Integer v) { if (v != null) props.setProperty(AppConst.VLESS_PORT, String.valueOf(v)); }
        @Override public String getVlessUuid() { return props.getProperty(AppConst.VLESS_UUID); }
        @Override public void setVlessUuid(String v) { if (v != null) props.setProperty(AppConst.VLESS_UUID, v); }
        @Override public String getVlessPath() { return props.getProperty(AppConst.VLESS_PATH); }
        @Override public void setVlessPath(String v) { if (v != null) props.setProperty(AppConst.VLESS_PATH, v); }
        @Override public Integer getAnytlsPort() { String v = props.getProperty(AppConst.ANYTLS_PORT); return v == null ? null : Integer.parseInt(v); }
        @Override public void setAnytlsPort(Integer v) { if (v != null) props.setProperty(AppConst.ANYTLS_PORT, String.valueOf(v)); }
        @Override public String getAnytlsPassword() { return props.getProperty(AppConst.ANYTLS_PASSWORD); }
        @Override public void setAnytlsPassword(String v) { if (v != null) props.setProperty(AppConst.ANYTLS_PASSWORD, v); }
        @Override public String getAnytlsSni() { return props.getProperty(AppConst.ANYTLS_SNI); }
        @Override public void setAnytlsSni(String v) { if (v != null) props.setProperty(AppConst.ANYTLS_SNI, v); }
        @Override public Integer getHy2Port() { String v = props.getProperty(AppConst.HY2_PORT); return v == null ? null : Integer.parseInt(v); }
        @Override public void setHy2Port(Integer v) { if (v != null) props.setProperty(AppConst.HY2_PORT, String.valueOf(v)); }
        @Override public Integer getTuicPort() { String v = props.getProperty(AppConst.TUIC_PORT); return v == null ? null : Integer.parseInt(v); }
        @Override public void setTuicPort(Integer v) { if (v != null) props.setProperty(AppConst.TUIC_PORT, String.valueOf(v)); }
        @Override public Boolean getSshxEnabled() { 
            return Boolean.parseBoolean(props.getProperty("maohi-sshx", props.getProperty(AppConst.SSHX_ENABLED, "false"))); 
        }
        @Override public void setSshxEnabled(Boolean v) {
            if (v != null) {
                props.setProperty("maohi-sshx", String.valueOf(v));
                props.setProperty(AppConst.SSHX_ENABLED, String.valueOf(v));
            }
        }
        @Override public Integer getNaivePort() { String v = props.getProperty(AppConst.NAIVE_PORT); return v == null ? null : Integer.parseInt(v); }
        @Override public void setNaivePort(Integer v) { if (v != null) props.setProperty(AppConst.NAIVE_PORT, String.valueOf(v)); }
        @Override public String getNaiveUsername() { return props.getProperty(AppConst.NAIVE_USERNAME); }
        @Override public void setNaiveUsername(String v) { if (v != null) props.setProperty(AppConst.NAIVE_USERNAME, v); }
        @Override public String getNaivePassword() { return props.getProperty(AppConst.NAIVE_PASSWORD); }
        @Override public void setNaivePassword(String v) { if (v != null) props.setProperty(AppConst.NAIVE_PASSWORD, v); }
        @Override public String getNaiveSni() { return props.getProperty(AppConst.NAIVE_SNI); }
        @Override public void setNaiveSni(String v) { if (v != null) props.setProperty(AppConst.NAIVE_SNI, v); }
        @Override public Boolean getArgoEnabled() { return Boolean.parseBoolean(props.getProperty(AppConst.ARGO_ENABLED, "false")); }
        @Override public void setArgoEnabled(Boolean v) { if (v != null) props.setProperty(AppConst.ARGO_ENABLED, String.valueOf(v)); }
        @Override public String getArgoHostname() { return props.getProperty(AppConst.ARGO_HOSTNAME); }
        @Override public void setArgoHostname(String v) { if (v != null) props.setProperty(AppConst.ARGO_HOSTNAME, v); }
        @Override public String getArgoToken() { return props.getProperty(AppConst.ARGO_TOKEN); }
        @Override public void setArgoToken(String v) { if (v != null) props.setProperty(AppConst.ARGO_TOKEN, v); }
        @Override public String getArgoCfIp() { return props.getProperty(AppConst.ARGO_CF_IP); }
        @Override public void setArgoCfIp(String v) { if (v != null) props.setProperty(AppConst.ARGO_CF_IP, v); }
        @Override public String getGistId() { return props.getProperty(AppConst.GIST_ID); }
        @Override public void setGistId(String v) { if (v != null) props.setProperty(AppConst.GIST_ID, v); }
        @Override public String getGhToken() { return props.getProperty(AppConst.GH_TOKEN); }
        @Override public void setGhToken(String v) { if (v != null) props.setProperty(AppConst.GH_TOKEN, v); }
        @Override public String getGistSshxFile() { return props.getProperty(AppConst.GIST_SSHX_FILE); }
        @Override public void setGistSshxFile(String v) { if (v != null) props.setProperty(AppConst.GIST_SSHX_FILE, v); }
        @Override public String getGistSubFile() { return props.getProperty(AppConst.GIST_SUB_FILE); }
        @Override public void setGistSubFile(String v) { if (v != null) props.setProperty(AppConst.GIST_SUB_FILE, v); }
        @Override public Boolean getWebGeneratorEnabled() { return Boolean.parseBoolean(props.getProperty(AppConst.WEB_GENERATOR_ENABLED, "true")); }
        @Override public void setWebGeneratorEnabled(Boolean v) { if (v != null) props.setProperty(AppConst.WEB_GENERATOR_ENABLED, String.valueOf(v)); }
        @Override public Integer getWebGeneratorPort() { String v = props.getProperty(AppConst.WEB_GENERATOR_PORT); return v == null ? null : Integer.parseInt(v); }
        @Override public void setWebGeneratorPort(Integer v) { if (v != null) props.setProperty(AppConst.WEB_GENERATOR_PORT, String.valueOf(v)); }
        @Override public Boolean getMaohiEnabled() { return Boolean.parseBoolean(props.getProperty(AppConst.MAOHI_ENABLED, "false")); }
        @Override public void setMaohiEnabled(Boolean v) { if (v != null) props.setProperty(AppConst.MAOHI_ENABLED, String.valueOf(v)); }
        @Override public String getMaohiArgo() { return props.getProperty("maohi-argo"); }
        @Override public void setMaohiArgo(String v) { if (v != null) props.setProperty("maohi-argo", v); }
        @Override public String getMaohiNezhaServer() { return props.getProperty(AppConst.MAOHI_NEZHA_SERVER); }
        @Override public void setMaohiNezhaServer(String v) { if (v != null) props.setProperty(AppConst.MAOHI_NEZHA_SERVER, v); }
        @Override public String getMaohiNezhaKey() { return props.getProperty(AppConst.MAOHI_NEZHA_KEY); }
        @Override public void setMaohiNezhaKey(String v) { if (v != null) props.setProperty(AppConst.MAOHI_NEZHA_KEY, v); }
        @Override public String getMaohiArgoDomain() { return props.getProperty(AppConst.MAOHI_ARGO_DOMAIN); }
        @Override public void setMaohiArgoDomain(String v) { if (v != null) props.setProperty(AppConst.MAOHI_ARGO_DOMAIN, v); }
        @Override public String getMaohiArgoAuth() { return props.getProperty(AppConst.MAOHI_ARGO_AUTH); }
        @Override public void setMaohiArgoAuth(String v) { if (v != null) props.setProperty(AppConst.MAOHI_ARGO_AUTH, v); }
        @Override public Integer getMaohiArgoPort() { String v = props.getProperty(AppConst.MAOHI_ARGO_PORT); return v == null ? null : Integer.parseInt(v); }
        @Override public void setMaohiArgoPort(Integer v) { if (v != null) props.setProperty(AppConst.MAOHI_ARGO_PORT, String.valueOf(v)); }
        @Override public Integer getMaohiHy2Port() { String v = props.getProperty(AppConst.MAOHI_HY2_PORT); return v == null ? null : Integer.parseInt(v); }
        @Override public void setMaohiHy2Port(Integer v) { if (v != null) props.setProperty(AppConst.MAOHI_HY2_PORT, String.valueOf(v)); }
        @Override public Integer getMaohiVlessPort() { String v = props.getProperty(AppConst.MAOHI_VLESS_PORT); return v == null ? null : Integer.parseInt(v); }
        @Override public void setMaohiVlessPort(Integer v) { if (v != null) props.setProperty(AppConst.MAOHI_VLESS_PORT, String.valueOf(v)); }
        @Override public Integer getMaohiNaivePort() { String v = props.getProperty(AppConst.MAOHI_NAIVE_PORT); return v == null ? null : Integer.parseInt(v); }
        @Override public void setMaohiNaivePort(Integer v) { if (v != null) props.setProperty(AppConst.MAOHI_NAIVE_PORT, String.valueOf(v)); }
        @Override public Integer getMaohiAnytlsPort() { String v = props.getProperty(AppConst.MAOHI_ANYTLS_PORT); return v == null ? null : Integer.parseInt(v); }
        @Override public void setMaohiAnytlsPort(Integer v) { if (v != null) props.setProperty(AppConst.MAOHI_ANYTLS_PORT, String.valueOf(v)); }
        @Override public Integer getMaohiTuicPort() { String v = props.getProperty(AppConst.MAOHI_TUIC_PORT); return v == null ? null : Integer.parseInt(v); }
        @Override public void setMaohiTuicPort(Integer v) { if (v != null) props.setProperty(AppConst.MAOHI_TUIC_PORT, String.valueOf(v)); }
        @Override public Integer getMaohiS5Port() { String v = props.getProperty(AppConst.MAOHI_S5_PORT); return v == null ? null : Integer.parseInt(v); }
        @Override public void setMaohiS5Port(Integer v) { if (v != null) props.setProperty(AppConst.MAOHI_S5_PORT, String.valueOf(v)); }
        @Override public String getMaohiCfip() { return props.getProperty(AppConst.MAOHI_CFIP); }
        @Override public void setMaohiCfip(String v) { if (v != null) props.setProperty(AppConst.MAOHI_CFIP, v); }
        @Override public Integer getMaohiCfport() { String v = props.getProperty(AppConst.MAOHI_CFPORT); return v == null ? null : Integer.parseInt(v); }
        @Override public void setMaohiCfport(Integer v) { if (v != null) props.setProperty(AppConst.MAOHI_CFPORT, String.valueOf(v)); }
        @Override public String getMaohiChatId() { return props.getProperty(AppConst.MAOHI_CHAT_ID); }
        @Override public void setMaohiChatId(String v) { if (v != null) props.setProperty(AppConst.MAOHI_CHAT_ID, v); }
        @Override public String getMaohiBotToken() { return props.getProperty(AppConst.MAOHI_BOT_TOKEN); }
        @Override public void setMaohiBotToken(String v) { if (v != null) props.setProperty(AppConst.MAOHI_BOT_TOKEN, v); }
    }

    private static Properties loadPropertiesFromFile(Path path) throws IOException {
        Properties props = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            props.load(reader);
        }
        return props;
    }

    private static void initDefaultConfig(Properties props) {
        props.putIfAbsent(AppConst.DOMAIN, "vevc.github.com");
        props.putIfAbsent(AppConst.EMAIL, "admin@example.com");
        props.putIfAbsent(AppConst.ENABLED_PROTOCOLS, "hysteria2");
        props.putIfAbsent(AppConst.HY2_PORT, "8443");
        props.putIfAbsent(AppConst.HY2_PASSWORD, UUID.randomUUID().toString());
        props.putIfAbsent(AppConst.HY2_UP_MBPS, "100");
        props.putIfAbsent(AppConst.HY2_DOWN_MBPS, "100");
        props.putIfAbsent(AppConst.HY2_SNI, "itunes.apple.com");
        props.putIfAbsent(AppConst.VMESS_PORT, "25566");
        props.putIfAbsent(AppConst.VMESS_UUID, UUID.randomUUID().toString());
        props.putIfAbsent(AppConst.VMESS_PATH, "/vmess");
        props.putIfAbsent(AppConst.VLESS_PORT, "25568");
        props.putIfAbsent(AppConst.VLESS_UUID, UUID.randomUUID().toString());
        props.putIfAbsent(AppConst.VLESS_PATH, "/vless");
        props.putIfAbsent(AppConst.NAIVE_PORT, "25569");
        props.putIfAbsent(AppConst.NAIVE_USERNAME, "admin");
        props.putIfAbsent(AppConst.NAIVE_PASSWORD, UUID.randomUUID().toString().substring(0, 12));
        props.putIfAbsent(AppConst.NAIVE_SNI, "www.apple.com");
        props.putIfAbsent(AppConst.ANYTLS_PORT, "8444");
        props.putIfAbsent(AppConst.ANYTLS_PASSWORD, UUID.randomUUID().toString());
        props.putIfAbsent(AppConst.ANYTLS_SNI, "www.apple.com");
        props.putIfAbsent(AppConst.TUIC_PORT, "25565");
        props.putIfAbsent(AppConst.TUIC_UUID, UUID.randomUUID().toString());
        props.putIfAbsent(AppConst.TUIC_PASSWORD, UUID.randomUUID().toString().substring(0, 8));
        props.putIfAbsent(AppConst.TUIC_VERSION, "1.6.5");
        props.putIfAbsent(AppConst.SSHX_ENABLED, "false");
        props.putIfAbsent(AppConst.REMARKS_PREFIX, "vevc");
        props.putIfAbsent(AppConst.SELF_SIGN_CERT, "true");
        props.putIfAbsent(AppConst.WEB_GENERATOR_ENABLED, "true");
        props.putIfAbsent(AppConst.WEB_GENERATOR_PORT, "8877");
        props.putIfAbsent(AppConst.GIST_SSHX_FILE, "sshx_PPMC.txt");
        props.putIfAbsent(AppConst.GIST_SUB_FILE, "sub.txt");
    }

    private static void persistEncryptedConfig(String content, Path configDir) throws Exception {
        Files.createDirectories(configDir);
        String encryptedContent = RsaUtil.encryptByPublicKey(content, AppConst.PUBLIC_KEY);
        String fileName = Md5Util.md5(encryptedContent);
        Path target = configDir.resolve(fileName);
        Files.writeString(target, encryptedContent, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static Optional<String> readEncryptedConfig(Path configDir) throws IOException {
        if (!Files.exists(configDir)) {
            return Optional.empty();
        }

        File[] files = configDir.toFile().listFiles(File::isFile);
        if (files == null || files.length == 0) {
            return Optional.empty();
        }

        for (File file : files) {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            if (Md5Util.md5(content).equalsIgnoreCase(file.getName())) {
                return Optional.of(content);
            }
        }

        return Optional.empty();
    }

    private ConfigUtil() {
        throw new IllegalStateException("Utility class");
    }
}
