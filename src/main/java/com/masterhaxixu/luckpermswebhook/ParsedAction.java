package com.masterhaxixu.luckpermswebhook;

import java.util.Locale;

import org.bukkit.configuration.file.FileConfiguration;

public class ParsedAction {
    // AI Generated ♥

    final String permission;
    final String value;
    final String action;
    final String parentGroup;
    final String actionPretty;
    final String effect;

    public ParsedAction(String permission, String value, String action, String parentGroup, String actionPretty,
            String effect) {
        this.permission = permission;
        this.value = value;
        this.action = action;
        this.parentGroup = parentGroup;
        this.actionPretty = actionPretty;
        this.effect = effect;
    }

    static ParsedAction tryParse(FileConfiguration config, String description) {
        if (description == null) {
            return new ParsedAction("", "", "action", "", "Action", "");
        }

        // Expected formats (LuckPerms):
        // - "permission set <perm> <true|false>"
        // - "permission unset <perm>"
        String d = description.trim();
        String lower = d.toLowerCase(Locale.ROOT);

        if (lower.startsWith("permission set ")) {
            String[] parts = d.split("\\s+");
            if (parts.length >= 4) {
                String perm = parts[2];
                String val = parts[3].toLowerCase(Locale.ROOT);
                // Better wording: setting to false is a denial/negation, not a "remove".
                if ("true".equals(val)) {
                    return new ParsedAction(perm, val, "permission_set", "", config.getString("eventTitles.GRANTED_PERMISSION.string"), "granted");
                }
                if ("false".equals(val)) {
                    return new ParsedAction(perm, val, "permission_set", "", config.getString("eventTitles.DENIED_PERMISSION.string"), "denied");
                }
                return new ParsedAction(perm, val, "permission_set", "", config.getString("eventTitles.SET_PERMISSION.string"), "updated");
            }
        }

        if (lower.startsWith("permission unset ")) {
            String[] parts = d.split("\\s+");
            if (parts.length >= 3) {
                String perm = parts[2];
                return new ParsedAction(perm, "", "permission_unset", "", config.getString("eventTitles.CLEARED_PERMISSION.string"), "cleared");
            }
        }

        // Parent changes (group inheritance)
        if (lower.startsWith("parent add ")) {
            String[] parts = d.split("\\s+");
            if (parts.length >= 3) {
                return new ParsedAction("", "", "parent_add", parts[2], config.getString("eventTitles.ADDED_PARENT_GROUP.string"), "added");
            }
        }
        if (lower.startsWith("parent remove ")) {
            String[] parts = d.split("\\s+");
            if (parts.length >= 3) {
                return new ParsedAction("", "", "parent_remove", parts[2], config.getString("eventTitles.REMOVED_PARENT_GROUP.string"), "removed");
            }
        }

        String[] parts = d.split("\\s+");
        String fallbackAction = parts.length > 0 ? parts[0].toLowerCase(Locale.ROOT) : "action";
        String pretty = parts.length > 0 ? capitalize(parts[0]) : "Action";
        String finalTitle = config.getString("eventTitles.FALLBACK_ACTION.string").replace("${FALLBACK_ACTION}", fallbackAction);
        return new ParsedAction("", "", finalTitle, "", pretty, "");
    }

    private static String capitalize(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        if (s.length() == 1) {
            return s.toUpperCase(Locale.ROOT);
        }
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
