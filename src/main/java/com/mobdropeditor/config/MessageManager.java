package com.mobdropeditor.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManager {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private String prefix = "&8[&6MobDrops&8] ";
    private final Map<String, String> messages = new HashMap<>();

    public void load(FileConfiguration config) {
        messages.clear();
        if (config.isConfigurationSection("messages")) {
            for (String key : config.getConfigurationSection("messages").getKeys(false)) {
                String val = config.getString("messages." + key);
                if (key.equalsIgnoreCase("prefix")) {
                    prefix = val;
                } else {
                    messages.put(key, val);
                }
            }
        }
    }

    public String getMessage(String key) {
        String msg = messages.getOrDefault(key, key);
        return colorize(prefix + msg);
    }

    public String getRawMessage(String key) {
        return colorize(messages.getOrDefault(key, key));
    }

    public String colorize(String text) {
        if (text == null) return "";

        // Parse Hex Colors: &#RRGGBB
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hexCode.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);

        // Parse standard legacy & color codes
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}
