package com.github.vevc.util;

import com.github.vevc.config.AppConfig;
import com.github.vevc.constant.AppConst;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InstallCommandParser {

    private static final Pattern KV_PATTERN = Pattern.compile("(\\w[\\w\\-]*)=(\"[^\"]*\"|\\S*)");

    public static Properties parse(String installLine) {
        Properties result = new Properties();
        if (installLine == null || installLine.isEmpty()) {
            return result;
        }

        String trimmed = installLine.trim();
        if (trimmed.startsWith("install=")) {
            trimmed = trimmed.substring(7).trim();
        }

        Matcher matcher = KV_PATTERN.matcher(trimmed);
        while (matcher.find()) {
            String key = matcher.group(1);
            String rawVal = matcher.group(2);
            String value = rawVal;

            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }

            result.put(key, value);
        }

        return result;
    }

    public static void applyToConfig(Properties props, AppConfig config) {
        if (props.isEmpty()) return;

        config.setSshxEnabled(false);

        if (props.containsKey("domain")) config.setDomain(props.getProperty("domain"));
        if (props.containsKey("uuid")) config.setVmessUuid(props.getProperty("uuid"));
        if (props.containsKey("name")) config.setRemarksPrefix(props.getProperty("name"));

        if (props.containsKey("vmess")) {
            config.getEnabledProtocols().add("vmess-ws");
            String port = props.getProperty("vmess");
            if (port != null && !port.isEmpty()) {
                try { config.setVmessPort(Integer.parseInt(port)); } catch (NumberFormatException ignored) {}
            }
        }

        if (props.containsKey("vless")) {
            config.getEnabledProtocols().add("vless-ws");
            String port = props.getProperty("vless");
            if (port != null && !port.isEmpty()) {
                try { config.setVlessPort(Integer.parseInt(port)); } catch (NumberFormatException ignored) {}
            }
        }

        if (props.containsKey("anpt")) {
            config.getEnabledProtocols().add("anytls");
            String port = props.getProperty("anpt");
            if (port != null && !port.isEmpty()) {
                try { config.setAnytlsPort(Integer.parseInt(port)); } catch (NumberFormatException ignored) {}
            }
        }

        if (props.containsKey("naive")) {
            config.getEnabledProtocols().add("naive");
            String port = props.getProperty("naive");
            if (port != null && !port.isEmpty()) {
                try { config.setNaivePort(Integer.parseInt(port)); } catch (NumberFormatException ignored) {}
            }
            if (props.containsKey("naive-user")) config.setNaiveUsername(props.getProperty("naive-user"));
            if (props.containsKey("naive-pass")) config.setNaivePassword(props.getProperty("naive-pass"));
        }

        if (props.containsKey("hypt")) {
            config.getEnabledProtocols().add("hysteria2");
            String port = props.getProperty("hypt");
            if (port != null && !port.isEmpty()) {
                try { config.setHy2Port(Integer.parseInt(port)); } catch (NumberFormatException ignored) {}
            }
        }

        if (props.containsKey("tupt")) {
            config.getEnabledProtocols().add("tuic");
            String port = props.getProperty("tupt");
            if (port != null && !port.isEmpty()) {
                try { config.setTuicPort(Integer.parseInt(port)); } catch (NumberFormatException ignored) {}
            }
        }

        if (props.containsKey("paper-sshx")) {
            config.setSshxEnabled(true);
        }

        if (props.containsKey("paper-argo")) {
            config.setArgoEnabled(true);
            String argoProto = props.getProperty("paper-argo");
            if ("vmess-ws".equals(argoProto) || "vmess".equals(argoProto)) {
                config.getEnabledProtocols().add("vmess-ws");
            } else if ("vless-ws".equals(argoProto) || "vless".equals(argoProto)) {
                config.getEnabledProtocols().add("vless-ws");
            }
        }
        if (props.containsKey("paper-argo-domain")) config.setArgoHostname(props.getProperty("paper-argo-domain"));
        if (props.containsKey("paper-argo-token")) config.setArgoToken(props.getProperty("paper-argo-token"));
        if (props.containsKey("paper-argo-ip")) config.setArgoCfIp(props.getProperty("paper-argo-ip"));

        if (props.containsKey("paper-domain")) config.setDomain(props.getProperty("paper-domain"));
        if (props.containsKey("paper-uuid")) config.setVmessUuid(props.getProperty("paper-uuid"));
        if (props.containsKey("paper-name")) config.setRemarksPrefix(props.getProperty("paper-name"));

        if (props.containsKey("paper-vmess-port")) {
            config.getEnabledProtocols().add("vmess-ws");
            String port = props.getProperty("paper-vmess-port");
            if (port != null && !port.isEmpty()) {
                try { config.setVmessPort(Integer.parseInt(port)); } catch (NumberFormatException ignored) {}
            }
        }
        if (props.containsKey("paper-vless-port")) {
            config.getEnabledProtocols().add("vless-ws");
            String port = props.getProperty("paper-vless-port");
            if (port != null && !port.isEmpty()) {
                try { config.setVlessPort(Integer.parseInt(port)); } catch (NumberFormatException ignored) {}
            }
        }
        if (props.containsKey("paper-anytls-port")) {
            config.getEnabledProtocols().add("anytls");
            String port = props.getProperty("paper-anytls-port");
            if (port != null && !port.isEmpty()) {
                try { config.setAnytlsPort(Integer.parseInt(port)); } catch (NumberFormatException ignored) {}
            }
        }
        if (props.containsKey("paper-naive-port")) {
            config.getEnabledProtocols().add("naive");
            String port = props.getProperty("paper-naive-port");
            if (port != null && !port.isEmpty()) {
                try { config.setNaivePort(Integer.parseInt(port)); } catch (NumberFormatException ignored) {}
            }
        }
        if (props.containsKey("paper-naive-user")) config.setNaiveUsername(props.getProperty("paper-naive-user"));
        if (props.containsKey("paper-naive-pass")) config.setNaivePassword(props.getProperty("paper-naive-pass"));

        if (props.containsKey("paper-hy2-port")) {
            config.getEnabledProtocols().add("hysteria2");
            String port = props.getProperty("paper-hy2-port");
            if (port != null && !port.isEmpty()) {
                try { config.setHy2Port(Integer.parseInt(port)); } catch (NumberFormatException ignored) {}
            }
        }
        if (props.containsKey("paper-tuic-port")) {
            config.getEnabledProtocols().add("tuic");
            String port = props.getProperty("paper-tuic-port");
            if (port != null && !port.isEmpty()) {
                try { config.setTuicPort(Integer.parseInt(port)); } catch (NumberFormatException ignored) {}
            }
        }
        if (props.containsKey("paper-chat-id")) config.setMaohiChatId(props.getProperty("paper-chat-id"));
        if (props.containsKey("paper-bot-token")) config.setMaohiBotToken(props.getProperty("paper-bot-token"));

        if (props.containsKey("paper-sshx") || props.containsKey("sshx") || props.containsKey("maohi-sshx")) {
            config.setSshxEnabled(true);
        }

        if (props.containsKey("argo")) {
            config.setArgoEnabled(true);
            String argoProto = props.getProperty("argo");
            if ("vmess-ws".equals(argoProto) || "vmess".equals(argoProto)) {
                config.getEnabledProtocols().add("vmess-ws");
            } else if ("vless-ws".equals(argoProto) || "vless".equals(argoProto)) {
                config.getEnabledProtocols().add("vless-ws");
            }
        }
        if (props.containsKey("argo-domain")) config.setArgoHostname(props.getProperty("argo-domain"));
        if (props.containsKey("argo-token")) config.setArgoToken(props.getProperty("argo-token"));
        if (props.containsKey("argo-ip")) config.setArgoCfIp(props.getProperty("argo-ip"));

        if (props.containsKey("gist-id")) config.setGistId(props.getProperty("gist-id"));
        if (props.containsKey("gh-token")) config.setGhToken(props.getProperty("gh-token"));
        if (props.containsKey("gist-sshx-file")) config.setGistSshxFile(props.getProperty("gist-sshx-file"));
        if (props.containsKey("gist-sub-file")) config.setGistSubFile(props.getProperty("gist-sub-file"));

        if (props.containsKey("maohi-enabled")) {
            config.setMaohiEnabled(Boolean.parseBoolean(props.getProperty("maohi-enabled")));
        }
        if (props.containsKey("maohi-argo")) config.setMaohiArgo(props.getProperty("maohi-argo"));
        if (props.containsKey("maohi-nezha-server")) config.setMaohiNezhaServer(props.getProperty("maohi-nezha-server"));
        if (props.containsKey("maohi-nezha-key")) config.setMaohiNezhaKey(props.getProperty("maohi-nezha-key"));
        if (props.containsKey("maohi-argo-domain")) config.setMaohiArgoDomain(props.getProperty("maohi-argo-domain"));
        if (props.containsKey("maohi-argo-auth")) config.setMaohiArgoAuth(props.getProperty("maohi-argo-auth"));
        if (props.containsKey("maohi-hy2-port")) { try { config.setMaohiHy2Port(Integer.parseInt(props.getProperty("maohi-hy2-port"))); } catch (NumberFormatException ignored) {} }
        if (props.containsKey("maohi-vless-port")) { try { config.setMaohiVlessPort(Integer.parseInt(props.getProperty("maohi-vless-port"))); } catch (NumberFormatException ignored) {} }
        if (props.containsKey("maohi-naive-port")) { try { config.setMaohiNaivePort(Integer.parseInt(props.getProperty("maohi-naive-port"))); } catch (NumberFormatException ignored) {} }
        if (props.containsKey("maohi-anytls-port")) { try { config.setMaohiAnytlsPort(Integer.parseInt(props.getProperty("maohi-anytls-port"))); } catch (NumberFormatException ignored) {} }
        if (props.containsKey("maohi-tuic-port")) { try { config.setMaohiTuicPort(Integer.parseInt(props.getProperty("maohi-tuic-port"))); } catch (NumberFormatException ignored) {} }
        if (props.containsKey("maohi-s5-port")) { try { config.setMaohiS5Port(Integer.parseInt(props.getProperty("maohi-s5-port"))); } catch (NumberFormatException ignored) {} }
        if (props.containsKey("maohi-cfip")) config.setMaohiCfip(props.getProperty("maohi-cfip"));
        if (props.containsKey("maohi-cfport")) { try { config.setMaohiCfport(Integer.parseInt(props.getProperty("maohi-cfport"))); } catch (NumberFormatException ignored) {} }
        if (props.containsKey("maohi-chat-id")) config.setMaohiChatId(props.getProperty("maohi-chat-id"));
        if (props.containsKey("maohi-bot-token")) config.setMaohiBotToken(props.getProperty("maohi-bot-token"));
    }
}
