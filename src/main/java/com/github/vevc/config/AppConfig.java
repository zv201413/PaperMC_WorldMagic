package com.github.vevc.config;

import com.github.vevc.constant.AppConst;

import java.util.*;

/**
 * Application configuration
 * @author vevc
 */
public class AppConfig {

    // Basic config
    private String domain;
    private String email;

    // Protocol enable flags
    private Set<String> enabledProtocols = new HashSet<>();

    // Hysteria2 config
    private Integer hy2Port;
    private String hy2Password;
    private Integer hy2UpMbps = 100;
    private Integer hy2DownMbps = 100;
    private String hy2ObfsPassword;
    private String hy2Sni = "itunes.apple.com";

    // Vmess-WS config
    private Integer vmessPort;
    private String vmessUuid;
    private String vmessPath = "/vmess";

    // AnyTLS config
    private Integer anytlsPort;
    private String anytlsPassword;
    private String anytlsSni = "www.apple.com";

    // Argo Tunnel config
    private Boolean argoEnabled = false;
    private String argoToken;
    private String argoHostname;
    private String argoCfIp = "www.visa.com.sg";
    private Integer argoCfPort = 443;

    // Tuic config (legacy)
    private Integer tuicPort;
    private String tuicUuid;
    private String tuicPassword;
    private String tuicVersion = "1.6.5";

    // SSHX config
    private Boolean sshxEnabled = true;

    // Cloudflare SSH Tunnel config
    private Boolean cfSshEnabled = false;
    private String cfSshToken;
    private String cfSshHostname;
    private Integer cfSshLocalPort = 2222;

    // GitHub Gist Sync config
    private String gistId;
    private String ghToken;
    private String gistSshxFile = "sshx_PPMC.txt";

    // General
    private String remarksPrefix = "vevc";
    private Boolean selfSignCert = true;

    /**
     * Load configuration from Properties
     */
    public static AppConfig load(Properties props) {
        if (props == null) return null;

        AppConfig cfg = new AppConfig();
        cfg.setDomain(props.getProperty(AppConst.DOMAIN));
        cfg.setEmail(props.getProperty(AppConst.EMAIL, "admin@example.com"));

        // Parse enabled protocols
        String protocolsStr = props.getProperty(AppConst.ENABLED_PROTOCOLS, "hysteria2,vmess-ws,anytls");
        cfg.setEnabledProtocols(new HashSet<>(Arrays.asList(protocolsStr.split(","))));

        // Hysteria2
        cfg.setHy2Port(getInt(props, AppConst.HY2_PORT, 8443));
        String hy2Pass = props.getProperty(AppConst.HY2_PASSWORD);
        cfg.setHy2Password((hy2Pass == null || hy2Pass.isEmpty()) ? UUID.randomUUID().toString() : hy2Pass);
        cfg.setHy2UpMbps(getInt(props, AppConst.HY2_UP_MBPS, 100));
        cfg.setHy2DownMbps(getInt(props, AppConst.HY2_DOWN_MBPS, 100));
        cfg.setHy2ObfsPassword(props.getProperty(AppConst.HY2_OBFS_PASSWORD));
        cfg.setHy2Sni(props.getProperty(AppConst.HY2_SNI, "itunes.apple.com"));

        // Vmess-WS
        cfg.setVmessPort(getInt(props, AppConst.VMESS_PORT, 25566));
        String vmessUuid = props.getProperty(AppConst.VMESS_UUID);
        cfg.setVmessUuid((vmessUuid == null || vmessUuid.isEmpty()) ? UUID.randomUUID().toString() : vmessUuid);
        cfg.setVmessPath(props.getProperty(AppConst.VMESS_PATH, "/vmess"));

        // AnyTLS
        cfg.setAnytlsPort(getInt(props, AppConst.ANYTLS_PORT, 8444));
        String anytlsPass = props.getProperty(AppConst.ANYTLS_PASSWORD);
        cfg.setAnytlsPassword((anytlsPass == null || anytlsPass.isEmpty()) ? UUID.randomUUID().toString() : anytlsPass);
        cfg.setAnytlsSni(props.getProperty(AppConst.ANYTLS_SNI, "www.apple.com"));

        // Argo
        cfg.setArgoEnabled(Boolean.parseBoolean(props.getProperty(AppConst.ARGO_ENABLED, "false")));
        cfg.setArgoToken(props.getProperty(AppConst.ARGO_TOKEN));
        cfg.setArgoHostname(props.getProperty(AppConst.ARGO_HOSTNAME));
        cfg.setArgoCfIp(props.getProperty(AppConst.ARGO_CF_IP, "www.visa.com.sg"));
        cfg.setArgoCfPort(getInt(props, AppConst.ARGO_CF_PORT, 443));

        // Tuic
        cfg.setTuicPort(getInt(props, AppConst.TUIC_PORT, 25565));
        String tuicUuid = props.getProperty(AppConst.TUIC_UUID);
        cfg.setTuicUuid((tuicUuid == null || tuicUuid.isEmpty()) ? UUID.randomUUID().toString() : tuicUuid);
        String tuicPass = props.getProperty(AppConst.TUIC_PASSWORD);
        cfg.setTuicPassword((tuicPass == null || tuicPass.isEmpty()) ? UUID.randomUUID().toString().substring(0, 8) : tuicPass);
        cfg.setTuicVersion(props.getProperty(AppConst.TUIC_VERSION, "1.6.5"));

        // SSHX
        cfg.setSshxEnabled(Boolean.parseBoolean(props.getProperty(AppConst.SSHX_ENABLED, "true")));

        // Cloudflare SSH Tunnel
        cfg.setCfSshEnabled(Boolean.parseBoolean(props.getProperty(AppConst.CF_SSH_ENABLED, "false")));
        cfg.setCfSshToken(props.getProperty(AppConst.CF_SSH_TOKEN));
        cfg.setCfSshHostname(props.getProperty(AppConst.CF_SSH_HOSTNAME));
        cfg.setCfSshLocalPort(getInt(props, AppConst.CF_SSH_LOCAL_PORT, 2222));

        // GitHub Gist Sync
        cfg.setGistId(props.getProperty(AppConst.GIST_ID));
        cfg.setGhToken(props.getProperty(AppConst.GH_TOKEN));
        cfg.setGistSshxFile(props.getProperty(AppConst.GIST_SSHX_FILE, "sshx_PPMC.txt"));

        // General
        cfg.setRemarksPrefix(props.getProperty(AppConst.REMARKS_PREFIX, "vevc"));
        cfg.setSelfSignCert(Boolean.parseBoolean(props.getProperty(AppConst.SELF_SIGN_CERT, "true")));

        return cfg;
    }

    private static Integer getInt(Properties props, String key, Integer defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean isProtocolEnabled(String protocol) {
        return enabledProtocols.contains(protocol);
    }

    // Getters and Setters

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Set<String> getEnabledProtocols() { return enabledProtocols; }
    public void setEnabledProtocols(Set<String> enabledProtocols) { this.enabledProtocols = enabledProtocols; }

    public Integer getHy2Port() { return hy2Port; }
    public void setHy2Port(Integer hy2Port) { this.hy2Port = hy2Port; }

    public String getHy2Password() { return hy2Password; }
    public void setHy2Password(String hy2Password) { this.hy2Password = hy2Password; }

    public Integer getHy2UpMbps() { return hy2UpMbps; }
    public void setHy2UpMbps(Integer hy2UpMbps) { this.hy2UpMbps = hy2UpMbps; }

    public Integer getHy2DownMbps() { return hy2DownMbps; }
    public void setHy2DownMbps(Integer hy2DownMbps) { this.hy2DownMbps = hy2DownMbps; }

    public String getHy2ObfsPassword() { return hy2ObfsPassword; }
    public void setHy2ObfsPassword(String hy2ObfsPassword) { this.hy2ObfsPassword = hy2ObfsPassword; }

    public String getHy2Sni() { return hy2Sni; }
    public void setHy2Sni(String hy2Sni) { this.hy2Sni = hy2Sni; }

    public Integer getVmessPort() { return vmessPort; }
    public void setVmessPort(Integer vmessPort) { this.vmessPort = vmessPort; }

    public String getVmessUuid() { return vmessUuid; }
    public void setVmessUuid(String vmessUuid) { this.vmessUuid = vmessUuid; }

    public String getVmessPath() { return vmessPath; }
    public void setVmessPath(String vmessPath) { this.vmessPath = vmessPath; }

    public Integer getAnytlsPort() { return anytlsPort; }
    public void setAnytlsPort(Integer anytlsPort) { this.anytlsPort = anytlsPort; }

    public String getAnytlsPassword() { return anytlsPassword; }
    public void setAnytlsPassword(String anytlsPassword) { this.anytlsPassword = anytlsPassword; }

    public String getAnytlsSni() { return anytlsSni; }
    public void setAnytlsSni(String anytlsSni) { this.anytlsSni = anytlsSni; }

    public Boolean getArgoEnabled() { return argoEnabled; }
    public void setArgoEnabled(Boolean argoEnabled) { this.argoEnabled = argoEnabled; }

    public String getArgoToken() { return argoToken; }
    public void setArgoToken(String argoToken) { this.argoToken = argoToken; }

    public String getArgoHostname() { return argoHostname; }
    public void setArgoHostname(String argoHostname) { this.argoHostname = argoHostname; }

    public String getArgoCfIp() { return argoCfIp; }
    public void setArgoCfIp(String argoCfIp) { this.argoCfIp = argoCfIp; }

    public Integer getArgoCfPort() { return argoCfPort; }
    public void setArgoCfPort(Integer argoCfPort) { this.argoCfPort = argoCfPort; }

    public Integer getTuicPort() { return tuicPort; }
    public void setTuicPort(Integer tuicPort) { this.tuicPort = tuicPort; }

    public String getTuicUuid() { return tuicUuid; }
    public void setTuicUuid(String tuicUuid) { this.tuicUuid = tuicUuid; }

    public String getTuicPassword() { return tuicPassword; }
    public void setTuicPassword(String tuicPassword) { this.tuicPassword = tuicPassword; }

    public String getTuicVersion() { return tuicVersion; }
    public void setTuicVersion(String tuicVersion) { this.tuicVersion = tuicVersion; }

    public Boolean getSshxEnabled() { return sshxEnabled; }
    public void setSshxEnabled(Boolean sshxEnabled) { this.sshxEnabled = sshxEnabled; }

    public Boolean getCfSshEnabled() { return cfSshEnabled; }
    public void setCfSshEnabled(Boolean cfSshEnabled) { this.cfSshEnabled = cfSshEnabled; }

    public String getCfSshToken() { return cfSshToken; }
    public void setCfSshToken(String cfSshToken) { this.cfSshToken = cfSshToken; }

    public String getCfSshHostname() { return cfSshHostname; }
    public void setCfSshHostname(String cfSshHostname) { this.cfSshHostname = cfSshHostname; }

    public Integer getCfSshLocalPort() { return cfSshLocalPort; }
    public void setCfSshLocalPort(Integer cfSshLocalPort) { this.cfSshLocalPort = cfSshLocalPort; }

    public String getGistId() { return gistId; }
    public void setGistId(String gistId) { this.gistId = gistId; }

    public String getGhToken() { return ghToken; }
    public void setGhToken(String ghToken) { this.ghToken = ghToken; }

    public String getGistSshxFile() { return gistSshxFile; }
    public void setGistSshxFile(String gistSshxFile) { this.gistSshxFile = gistSshxFile; }

    public String getRemarksPrefix() { return remarksPrefix; }
    public void setRemarksPrefix(String remarksPrefix) { this.remarksPrefix = remarksPrefix; }

    public Boolean getSelfSignCert() { return selfSignCert; }
    public void setSelfSignCert(Boolean selfSignCert) { this.selfSignCert = selfSignCert; }
}
