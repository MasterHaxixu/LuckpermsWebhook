package com.masterhaxixu.luckpermswebhook;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.bukkit.Bukkit;

import net.luckperms.api.actionlog.Action;
import net.luckperms.api.event.log.LogBroadcastEvent;

public class WebhookHandler {

    public static String parseString(String message, Map<String, String> variables) {
        String parsed = message;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            parsed = parsed.replace(placeholder, escapeJson(entry.getValue()));
        }
        return parsed;
    }

    public static void sendWebhook(Main plugin, LogBroadcastEvent event) {
        Action entry = event.getEntry();
        if (entry == null) {
            return;
        }

        Map<String, String> vars = buildVariables(entry);
        vars.put("eventType", "LogBroadcastEvent");
        vars.put("origin", event.getOrigin().name());

        String embedKey = selectLogBroadcastEmbed(vars);
        String template = plugin.getEmbeds().get(embedKey);
        if (template == null) {
            plugin.getLogger().log(Level.WARNING, "No embed template loaded for: {0}", embedKey);
            return;
        }

        String payload = parseString(template, vars);
        sendPayloadAsync(plugin, payload);
    }

    private static String selectLogBroadcastEmbed(Map<String, String> vars) {
        String permission = vars.getOrDefault("permission", "");
        String parentGroup = vars.getOrDefault("parentGroup", "");

        if (permission != null && !permission.isBlank()) {
            return "LogBroadcastPermissionEvent";
        }
        if (parentGroup != null && !parentGroup.isBlank()) {
            return "LogBroadcastParentEvent";
        }
        return "LogBroadcastEvent";
    }

    private static void sendPayloadAsync(Main plugin, String payload) {
        HttpClient httpClient = HttpClients.createDefault();

        HttpPost request = new HttpPost(plugin.getConfig().getString("webhooks.luckperms.url"));
        request.setHeader("Content-type", "application/json");
        request.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                httpClient.execute(request);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to send notification. Is the webhook valid?");
            }
        });
    }

    private static Map<String, String> buildVariables(Action entry) {
        Map<String, String> vars = new HashMap<>();

        vars.put("timestamp", entry.getTimestamp() == null ? "" : entry.getTimestamp().toString());
        vars.put("description", safe(entry.getDescription()));

        // Source (who executed the command)
        vars.put("sourceName", safe(entry.getSource().getName()));
        vars.put("sourceUuid", entry.getSource().getUniqueId().toString());

        // Target (who/what was modified)
        vars.put("targetName", safe(entry.getTarget().getName()));
        vars.put("targetType", entry.getTarget().getType().name());
        vars.put("targetUuid", entry.getTarget().getUniqueId().map(java.util.UUID::toString).orElse(""));

        // Parse commonly-used action details from description.
        ParsedAction parsed = ParsedAction.tryParse(entry.getDescription());
        vars.put("permission", parsed.permission);
        vars.put("value", parsed.value);
        vars.put("action", parsed.action);
        vars.put("parentGroup", parsed.parentGroup);
        vars.put("actionPretty", parsed.actionPretty);
        vars.put("effect", parsed.effect);

        vars.put("permissionDisplay", parsed.permission.isBlank() ? "(none)" : parsed.permission);
        vars.put("valueDisplay", parsed.value.isBlank() ? "(none)" : parsed.value);
        vars.put("effectDisplay", parsed.effect.isBlank() ? "" : " (" + parsed.effect + ")");
        vars.put("parentGroupDisplay", parsed.parentGroup.isBlank() ? "(none)" : parsed.parentGroup);

        // Backwards-compat placeholders used by existing templates.
        vars.put("actorName", vars.get("sourceName"));
        vars.put("actorUuid", vars.get("sourceUuid"));
        vars.put("actionDescription", vars.get("description"));
        vars.put("nodeKey", parsed.permission);
        vars.put("nodeValue", parsed.value);

        // Convenience pre-formatted strings for embeds
        vars.put("targetDisplay", vars.get("targetType") + " " + vars.get("targetName"));

        return vars;
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    out.append("\\\\");
                    break;
                case '"':
                    out.append("\\\"");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
