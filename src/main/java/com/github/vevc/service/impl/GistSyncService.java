package com.github.vevc.service.impl;

import com.github.vevc.config.AppConfig;
import com.github.vevc.util.LogUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class GistSyncService {

    private final String gistId;
    private final String ghToken;

    public GistSyncService(AppConfig config) {
        this.gistId = config.getGistId();
        this.ghToken = config.getGhToken();
    }

    public boolean isEnabled() {
        return gistId != null && !gistId.isEmpty() && ghToken != null && !ghToken.isEmpty();
    }

    public void sync(String filename, String content) {
        LogUtil.info("[Gist] Syncing " + filename + "...");
        if (!isEnabled()) {
            LogUtil.info("[Gist] Gist sync disabled, skipping");
            return;
        }

        try {
            String timestamp = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                    .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
            
            String fullContent = "最后更新时间: " + timestamp + "\n----------------------------\n" + content;

            String jsonBody = buildPatchJson(filename, fullContent);
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/gists/" + gistId))
                    .header("Authorization", "token " + ghToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                LogUtil.info("[Gist] Sync success: " + filename);
            } else {
                LogUtil.info("[Gist] Sync failed: HTTP " + response.statusCode() + " - " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            LogUtil.info("[Gist] Sync error: " + e.getMessage());
        }
    }

    private String buildPatchJson(String filename, String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"description\": \"WorldMagic SSH 链接同步\",");
        sb.append("\"files\": {");
        sb.append("\"").append(escapeJson(filename)).append("\": {");
        sb.append("\"content\": \"").append(escapeJson(content)).append("\"");
        sb.append("}");
        sb.append("}");
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
