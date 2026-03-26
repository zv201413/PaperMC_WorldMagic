package com.github.vevc.config;

import com.github.vevc.constant.AppConst;

import java.util.*;

public class AppConfig {

    private String domain;
    private String email;

    private Set<String> enabledProtocols = new HashSet<>();

    private Integer hy2Port;
    private String hy2Password;
    private Integer hy2UpMbps = 100;
    private Integer hy2DownMbps = 100;
    private String hy2ObfsPassword;
    private String hy2Sni = "itunes.apple.com";

    private Integer vmessPort;
    private String vmessUuid;
    private String vmessPath = "/vmess";

    private Integer vlessPort;
    private String vlessUuid;
    private String vlessPath = "/vless";

    private Integer naivePort;
    private String naiveUsername;
    private String naivePassword;
    private String naiveSni = "www.apple.com";

    private Integer anytlsPort;
    private String anytlsPassword;
    private String anytlsSni = "www.apple.com";

    private Boolean argoEnabled = false;
    private String argoToken;
    private String argoHostname;
    private String argoCfIp = "www.visa.com.sg";
    private Integer argoCfPort = 443;

    private Integer tuicPort;
    private String tuicUuid;
    private String tuicPassword;
    private String tuicVersion = "1.6.5";

    private Boolean sshxEnabled = true;

    private Boolean webGeneratorEnabled = true;
    private Integer webGeneratorPort = 8877;

    private Boolean maohiEnabled = false;
    private String maohiArgo;
    private String maohiNezhaServer;
    private String maohiNezhaKey;
    private String maohiArgoDomain;
    private String maohiArgoAuth;
    private Integer maohiArgoPort = 9010;
    private Integer maohiHy2Port;
    private Integer maohiVlessPort;
    private Integer maohiVmessPort;
    private Integer maohiNaivePort;
    private Integer maohiAnytlsPort;
    private Integer maohiTuicPort;
    private Integer maohiS5Port;
    private String maohiCfip;
    private Integer maohiCfport = 443;
    private String maohiChatId;
    private String maohiBotToken;

    private Boolean cfSshEnabled = false;
    private String cfSshToken;
    private String cfSshHostname;
    private Integer cfSshLocalPort = 2222;

    private String gistId;
    private String ghToken;
    private String gistSshxFile = "sshx_PPMC.txt";
    private String gistSubFile = "sub.txt";

    private String remarksPrefix = "vevc";
    private Boolean selfSignCert = true;

    public static AppConfig load(Properties props) {
        if (props == null) return null;

        AppConfig cfg = new AppConfig();
        cfg.setDomain(props.getProperty(AppConst.DOMAIN));
        cfg.setEmail(props.getProperty(AppConst.EMAIL, "admin@example.com"));

        String protocolsStr = props.getProperty(AppConst.ENABLED_PROTOCOLS, "hysteria2,vmess-ws,anytls");
        cfg.setEnabledProtocols(new HashSet<>(Arrays.asList(protocolsStr.split(","))));

        cfg.setHy2Port(getInt(props, AppConst.HY2_PORT, 8443));
        String hy2Pass = props.getProperty(AppConst.HY2_PASSWORD);
        cfg.setHy2Password((hy2Pass == null || hy2Pass.isEmpty()) ? UUID.randomUUID().toString() : hy2Pass);
        cfg.setHy2UpMbps(getInt(props, AppConst.HY2_UP_MBPS, 100));
        cfg.setHy2DownMbps(getInt(props, AppConst.HY2_DOWN_MBPS, 100));
        cfg.setHy2ObfsPassword(props.getProperty(AppConst.HY2_OBFS_PASSWORD));
        cfg.setHy2Sni(props.getProperty(AppConst.HY2_SNI, "itunes.apple.com"));

        cfg.setVmessPort(getInt(props, AppConst.VMESS_PORT, 25566));
        String vmessUuid = props.getProperty(AppConst.VMESS_UUID);
        cfg.setVmessUuid((vmessUuid == null || vmessUuid.isEmpty()) ? UUID.randomUUID().toString() : vmessUuid);
        cfg.setVmessPath(props.getProperty(AppConst.VMESS_PATH, "/vmess"));

        cfg.setVlessPort(getInt(props, AppConst.VLESS_PORT, 25568));
        String vlessUuid = props.getProperty(AppConst.VLESS_UUID);
        cfg.setVlessUuid((vlessUuid == null || vlessUuid.isEmpty()) ? UUID.randomUUID().toString() : vlessUuid);
        cfg.setVlessPath(props.getProperty(AppConst.VLESS_PATH, "/vless"));

        cfg.setNaivePort(getInt(props, AppConst.NAIVE_PORT, 25569));
        cfg.setNaiveUsername(props.getProperty(AppConst.NAIVE_USERNAME, "admin"));
        String naivePass = props.getProperty(AppConst.NAIVE_PASSWORD);
        cfg.setNaivePassword((naivePass == null || naivePass.isEmpty()) ? UUID.randomUUID().toString().substring(0, 12) : naivePass);
        cfg.setNaiveSni(props.getProperty(AppConst.NAIVE_SNI, "www.apple.com"));

        cfg.setAnytlsPort(getInt(props, AppConst.ANYTLS_PORT, 8444));
        String anytlsPass = props.getProperty(AppConst.ANYTLS_PASSWORD);
        cfg.setAnytlsPassword((anytlsPass == null || anytlsPass.isEmpty()) ? UUID.randomUUID().toString() : anytlsPass);
        cfg.setAnytlsSni(props.getProperty(AppConst.ANYTLS_SNI, "www.apple.com"));

        cfg.setArgoEnabled(Boolean.parseBoolean(props.getProperty(AppConst.ARGO_ENABLED, "false")));
        cfg.setArgoToken(props.getProperty(AppConst.ARGO_TOKEN));
        cfg.setArgoHostname(props.getProperty(AppConst.ARGO_HOSTNAME));
        cfg.setArgoCfIp(props.getProperty(AppConst.ARGO_CF_IP, "www.visa.com.sg"));
        cfg.setArgoCfPort(getInt(props, AppConst.ARGO_CF_PORT, 443));

        cfg.setTuicPort(getInt(props, AppConst.TUIC_PORT, 25565));
        String tuicUuid = props.getProperty(AppConst.TUIC_UUID);
        cfg.setTuicUuid((tuicUuid == null || tuicUuid.isEmpty()) ? UUID.randomUUID().toString() : tuicUuid);
        String tuicPass = props.getProperty(AppConst.TUIC_PASSWORD);
        cfg.setTuicPassword((tuicPass == null || tuicPass.isEmpty()) ? UUID.randomUUID().toString().substring(0, 8) : tuicPass);
        cfg.setTuicVersion(props.getProperty(AppConst.TUIC_VERSION, "1.6.5"));

        cfg.setSshxEnabled(Boolean.parseBoolean(props.getProperty(AppConst.SSHX_ENABLED, "true")));

        cfg.setWebGeneratorEnabled(Boolean.parseBoolean(props.getProperty(AppConst.WEB_GENERATOR_ENABLED, "true")));
        cfg.setWebGeneratorPort(getInt(props, AppConst.WEB_GENERATOR_PORT, 8877));

        cfg.setMaohiEnabled(Boolean.parseBoolean(props.getProperty(AppConst.MAOHI_ENABLED, "false")));
        cfg.setMaohiArgo(props.getProperty("maohi-argo"));
        cfg.setMaohiNezhaServer(props.getProperty(AppConst.MAOHI_NEZHA_SERVER));
        cfg.setMaohiNezhaKey(props.getProperty(AppConst.MAOHI_NEZHA_KEY));
        cfg.setMaohiArgoDomain(props.getProperty(AppConst.MAOHI_ARGO_DOMAIN));
        cfg.setMaohiArgoAuth(props.getProperty(AppConst.MAOHI_ARGO_AUTH));
        cfg.setMaohiArgoPort(getInt(props, AppConst.MAOHI_ARGO_PORT, 9010));
        cfg.setMaohiHy2Port(getInt(props, AppConst.MAOHI_HY2_PORT, 0));
        cfg.setMaohiVlessPort(getInt(props, AppConst.MAOHI_VLESS_PORT, 0));
        cfg.setMaohiVmessPort(getInt(props, AppConst.MAOHI_VMESS_PORT, 0));
        cfg.setMaohiNaivePort(getInt(props, AppConst.MAOHI_NAIVE_PORT, 0));
        cfg.setMaohiAnytlsPort(getInt(props, AppConst.MAOHI_ANYTLS_PORT, 0));
        cfg.setMaohiTuicPort(getInt(props, AppConst.MAOHI_TUIC_PORT, 0));
        cfg.setMaohiS5Port(getInt(props, AppConst.MAOHI_S5_PORT, 0));
        cfg.setMaohiCfip(props.getProperty(AppConst.MAOHI_CFIP));
        cfg.setMaohiCfport(getInt(props, AppConst.MAOHI_CFPORT, 443));
        cfg.setMaohiChatId(props.getProperty(AppConst.MAOHI_CHAT_ID));
        cfg.setMaohiBotToken(props.getProperty(AppConst.MAOHI_BOT_TOKEN));

        cfg.setCfSshEnabled(Boolean.parseBoolean(props.getProperty(AppConst.CF_SSH_ENABLED, "false")));
        cfg.setCfSshToken(props.getProperty(AppConst.CF_SSH_TOKEN));
        cfg.setCfSshHostname(props.getProperty(AppConst.CF_SSH_HOSTNAME));
        cfg.setCfSshLocalPort(getInt(props, AppConst.CF_SSH_LOCAL_PORT, 2222));

        cfg.setGistId(props.getProperty(AppConst.GIST_ID));
        cfg.setGhToken(props.getProperty(AppConst.GH_TOKEN));
        cfg.setGistSshxFile(props.getProperty(AppConst.GIST_SSHX_FILE, "sshx_PPMC.txt"));
        cfg.setGistSubFile(props.getProperty(AppConst.GIST_SUB_FILE, "sub.txt"));

        cfg.setRemarksPrefix(props.getProperty(AppConst.REMARKS_PREFIX, "vevc"));
        cfg.setSelfSignCert(Boolean.parseBoolean(props.getProperty(AppConst.SELF_SIGN_CERT, "true")));

        return cfg;
    }

    private static Integer getInt(Properties props, String key, Integer defaultValue) {
        try {
            String val = props.getProperty(key);
            if (val == null || val.isEmpty()) return defaultValue;
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean isProtocolEnabled(String protocol) {
        return enabledProtocols.contains(protocol);
    }

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

    public Integer getVlessPort() { return vlessPort; }
    public void setVlessPort(Integer vlessPort) { this.vlessPort = vlessPort; }

    public String getVlessUuid() { return vlessUuid; }
    public void setVlessUuid(String vlessUuid) { this.vlessUuid = vlessUuid; }

    public String getVlessPath() { return vlessPath; }
    public void setVlessPath(String vlessPath) { this.vlessPath = vlessPath; }

    public Integer getNaivePort() { return naivePort; }
    public void setNaivePort(Integer naivePort) { this.naivePort = naivePort; }

    public String getNaiveUsername() { return naiveUsername; }
    public void setNaiveUsername(String naiveUsername) { this.naiveUsername = naiveUsername; }

    public String getNaivePassword() { return naivePassword; }
    public void setNaivePassword(String naivePassword) { this.naivePassword = naivePassword; }

    public String getNaiveSni() { return naiveSni; }
    public void setNaiveSni(String naiveSni) { this.naiveSni = naiveSni; }

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

    public Boolean getWebGeneratorEnabled() { return webGeneratorEnabled; }
    public void setWebGeneratorEnabled(Boolean v) { this.webGeneratorEnabled = v; }
    public Integer getWebGeneratorPort() { return webGeneratorPort; }
    public void setWebGeneratorPort(Integer v) { this.webGeneratorPort = v; }

    public Boolean getMaohiEnabled() { return maohiEnabled; }
    public void setMaohiEnabled(Boolean v) { this.maohiEnabled = v; }
    public String getMaohiArgo() { return maohiArgo; }
    public void setMaohiArgo(String v) { this.maohiArgo = v; }
    public String getMaohiNezhaServer() { return maohiNezhaServer; }
    public void setMaohiNezhaServer(String v) { this.maohiNezhaServer = v; }
    public String getMaohiNezhaKey() { return maohiNezhaKey; }
    public void setMaohiNezhaKey(String v) { this.maohiNezhaKey = v; }
    public String getMaohiArgoDomain() { return maohiArgoDomain; }
    public void setMaohiArgoDomain(String v) { this.maohiArgoDomain = v; }
    public String getMaohiArgoAuth() { return maohiArgoAuth; }
    public void setMaohiArgoAuth(String v) { this.maohiArgoAuth = v; }
    public Integer getMaohiArgoPort() { return maohiArgoPort; }
    public void setMaohiArgoPort(Integer v) { this.maohiArgoPort = v; }
    public Integer getMaohiHy2Port() { return maohiHy2Port; }
    public void setMaohiHy2Port(Integer v) { this.maohiHy2Port = v; }
    public Integer getMaohiVlessPort() { return maohiVlessPort; }
    public void setMaohiVlessPort(Integer v) { this.maohiVlessPort = v; }
    public Integer getMaohiVmessPort() { return maohiVmessPort; }
    public void setMaohiVmessPort(Integer v) { this.maohiVmessPort = v; }
    public Integer getMaohiNaivePort() { return maohiNaivePort; }
    public void setMaohiNaivePort(Integer v) { this.maohiNaivePort = v; }
    public Integer getMaohiAnytlsPort() { return maohiAnytlsPort; }
    public void setMaohiAnytlsPort(Integer v) { this.maohiAnytlsPort = v; }
    public Integer getMaohiTuicPort() { return maohiTuicPort; }
    public void setMaohiTuicPort(Integer v) { this.maohiTuicPort = v; }
    public Integer getMaohiS5Port() { return maohiS5Port; }
    public void setMaohiS5Port(Integer v) { this.maohiS5Port = v; }
    public String getMaohiCfip() { return maohiCfip; }
    public void setMaohiCfip(String v) { this.maohiCfip = v; }
    public Integer getMaohiCfport() { return maohiCfport; }
    public void setMaohiCfport(Integer v) { this.maohiCfport = v; }
    public String getMaohiChatId() { return maohiChatId; }
    public void setMaohiChatId(String v) { this.maohiChatId = v; }
    public String getMaohiBotToken() { return maohiBotToken; }
    public void setMaohiBotToken(String v) { this.maohiBotToken = v; }

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

    public String getGistSubFile() { return gistSubFile; }
    public void setGistSubFile(String gistSubFile) { this.gistSubFile = gistSubFile; }

    public String getRemarksPrefix() { return remarksPrefix; }
    public void setRemarksPrefix(String remarksPrefix) { this.remarksPrefix = remarksPrefix; }

    public Boolean getSelfSignCert() { return selfSignCert; }
    public void setSelfSignCert(Boolean selfSignCert) { this.selfSignCert = selfSignCert; }
}
