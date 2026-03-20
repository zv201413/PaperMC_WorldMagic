package com.github.vevc.constant;

/**
 * Application constants
 * @author vevc
 */
public interface AppConst {
    // RSA Keys for config encryption
    String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCDml/0V0Dv01nEkYf1Pa/ecb4TSzt+hOjUT2uaDTCZP7N8ET4/xTtWEtGvtpREq9xuZZOfcVmBn2dsMadGehaZInkgX0Fojg0MD6U44rCf8mZG85HjWh/3dlmxhhRl51kKHKNeTrDxrdRAt75l0OdttSBqOExoMOMEVjAWTLZw2QIDAQAB";
    String PRIVATE_KEY = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAIOaX/RXQO/TWcSRh/U9r95xvhNLO36E6NRPa5oNMJk/s3wRPj/FO1YS0a+2lESr3G5lk59xWYGfZ2wxp0Z6FpkieSBfQWiODQwPpTjisJ/yZkbzkeNaH/d2WbGGFGXnWQoco15OsPGt1EC3vmXQ5221IGo4TGgw4wRWMBZMtnDZAgMBAAECgYAB2SwoVR8wTxsnZ6jabameICHvvCKDhu/ZmlAnUePN5ZpkFdgc0qys61o+rS7wN162JGvHAgzkOGr3yf6KS5wshYaXE++tFEcB7wasyyUWqmD3loxn5NW9Fi4+GjUvM2XJeQ9US2NslTpIv/KPR+XD63PoqHXOKOKlPjAUD+a51QJBAKG70zdbmY8xc90e5TddSK5dCTkBA0oVAafIa/H9Zv1JbouffvJ10hOoU8C4m1DLHcLSMibWs2gAUy4YZJVRqBUCQQDQTsYJFJIrLkAJTeA97h1Mu3kFSS1CP8xDe6b92bj0/qLo6nbu81VgjaMdLb/qcFQ5f1/j8OHUYnDYSukxrLK1AkBC5nO/MVe6wKUBsXb1SNP4tClNeBrJORk/MwtbxQsl+IsOnEIhvxTP5tAGJxav++TqopH2ONdrTL8bkSGCFo3lAkEAyMSzsDrIap/oDk+lqmreiH9ENCkEWw7cU8pQ9+epkU//OhgJS2LyTd4VcWEP0Og09Tnj9PDM6AN9GaqRuVPm5QJAZEkswcejBk1Y1gHzIQzPP+TBHHvxHMCeOF+tl7SIRlRBAjeNFUN0tMpyoeBUWlmL8bnTF1SsAChsv+UHb+/v8g==";

    // Basic config
    String DOMAIN = "domain";
    String EMAIL = "email";
    String ENABLED_PROTOCOLS = "enabled-protocols";

    // Hysteria2
    String HY2_PORT = "hy2-port";
    String HY2_PASSWORD = "hy2-password";
    String HY2_UP_MBPS = "hy2-up-mbps";
    String HY2_DOWN_MBPS = "hy2-down-mbps";
    String HY2_OBFS_PASSWORD = "hy2-obfs-password";
    String HY2_SNI = "hy2-sni";

    // Vmess-WS
    String VMESS_PORT = "vmess-port";
    String VMESS_UUID = "vmess-uuid";
    String VMESS_PATH = "vmess-path";

    // AnyTLS
    String ANYTLS_PORT = "anytls-port";
    String ANYTLS_PASSWORD = "anytls-password";
    String ANYTLS_SNI = "anytls-sni";

    // Argo Tunnel
    String ARGO_ENABLED = "argo-enabled";
    String ARGO_TOKEN = "argo-token";
    String ARGO_HOSTNAME = "argo-hostname";
    String ARGO_CF_IP = "argo-cf-ip";
    String ARGO_CF_PORT = "argo-cf-port";

    // Tuic (legacy support)
    String TUIC_PORT = "tuic-port";
    String TUIC_UUID = "tuic-uuid";
    String TUIC_PASSWORD = "tuic-password";
    String TUIC_VERSION = "tuic-version";

    // SSHX
    String SSHX_ENABLED = "sshx-enabled";

    // Cloudflare SSH Tunnel
    String CF_SSH_ENABLED = "cf-ssh-enabled";
    String CF_SSH_TOKEN = "cf-ssh-token";
    String CF_SSH_HOSTNAME = "cf-ssh-hostname";
    String CF_SSH_LOCAL_PORT = "cf-ssh-local-port";

    // GitHub Gist Sync
    String GIST_ID = "gist-id";
    String GH_TOKEN = "gh-token";
    String GIST_SSHX_FILE = "gist-sshx-file";

    // General
    String REMARKS_PREFIX = "remarks-prefix";
    String SELF_SIGN_CERT = "self-sign-cert";
}
