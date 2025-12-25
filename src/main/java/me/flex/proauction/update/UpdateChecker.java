package me.flex.proauction.update;

import me.flex.proauction.ProAuctionPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class UpdateChecker {

    private UpdateChecker() {}

    public static void checkAsync(ProAuctionPlugin plugin) {
        boolean enabled = plugin.getSettingsConfig().getBoolean("updates.enabled", true);
        if (!enabled) return;

        String owner = plugin.getSettingsConfig().getString("updates.github-owner", "").trim();
        String repo = plugin.getSettingsConfig().getString("updates.github-repo", "").trim();

        if (owner.isEmpty() || repo.isEmpty()) {
            plugin.getLogger().warning("Update checker is enabled but updates.github-owner/repo are not set in settings.yml");
            return;
        }

        String current = plugin.getDescription().getVersion();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String api = "https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest";

                HttpURLConnection con = (HttpURLConnection) new URL(api).openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);
                con.setRequestProperty("Accept", "application/vnd.github+json");
                con.setRequestProperty("User-Agent", "ProAuction/" + current);

                int code = con.getResponseCode();
                if (code != 200) {
                    plugin.getLogger().warning("Update check failed (HTTP " + code + ").");
                    return;
                }

                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }

                String json = sb.toString();

                String tag = extractJsonString(json, "tag_name");
                if (tag == null || tag.isBlank()) return;

                // common pattern: v1.0.0 -> 1.0.0
                String latest = tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;

                if (!equalsVersion(current, latest)) {
                    plugin.getLogger().info("Update available: " + latest + " (current: " + current + ")");
                    plugin.getLogger().info("Download: https://github.com/" + owner + "/" + repo + "/releases/latest");
                } else {
                    plugin.getLogger().info("You are running the latest version (" + current + ").");
                }

            } catch (Exception ex) {
                plugin.getLogger().warning("Update check error: " + ex.getMessage());
            }
        });
    }

    private static boolean equalsVersion(String a, String b) {
        return normalize(a).equalsIgnoreCase(normalize(b));
    }

    private static String normalize(String v) {
        if (v == null) return "";
        v = v.trim();
        if (v.startsWith("v") || v.startsWith("V")) v = v.substring(1);
        return v;
    }

    // tiny JSON key string extractor (no external libs)
    private static String extractJsonString(String json, String key) {
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return null;

        int start = json.indexOf('"', i + needle.length());
        if (start < 0) return null;

        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;

        return json.substring(start + 1, end);
    }
}
