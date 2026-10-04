package com.mobdropeditor.config;

import com.mobdropeditor.storage.SQLiteStorage;
import com.mobdropeditor.storage.Storage;
import com.mobdropeditor.storage.YamlStorage;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class ConfigManager {
    private final Plugin plugin;
    private final MessageManager messageManager;
    private Storage storage;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.messageManager = new MessageManager();
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        messageManager.load(plugin.getConfig());

        String storageType = plugin.getConfig().getString("storage.type", "YAML").toUpperCase();
        if (storage != null) {
            storage.close();
        }

        if (storageType.equalsIgnoreCase("SQLITE")) {
            storage = new SQLiteStorage(plugin.getDataFolder());
        } else {
            storage = new YamlStorage(plugin.getDataFolder());
        }
        storage.init();
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public Storage getStorage() {
        return storage;
    }
}
