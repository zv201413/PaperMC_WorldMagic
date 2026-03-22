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

        if (props.containsKey("sshx")) {
            config.setSshxEnabled(true);
        }

        if (props.containsKey("ttyd")) {
            config.setTtydEnabled(true);
            String port = props.getProperty("ttyd");
            if (port != null && !port.isEmpty()) {
                try { config.setTtydPort(Integer.parseInt(port)); } catch (NumberFormatException ignored) {}
            }
            if (props.containsKey("ttyd-pass")) config.setTtydPassword(props.getProperty("ttyd-pass"));
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
        if (props.containsKey("gist-ttyd-file")) config.setGistTtydFile(props.getProperty("gist-ttyd-file"));
    }
}
