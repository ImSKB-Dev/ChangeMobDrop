package com.mobdropeditor;

import com.mobdropeditor.command.MobDropsCommand;
import com.mobdropeditor.command.MobDropsTabCompleter;
import com.mobdropeditor.config.ConfigManager;
import com.mobdropeditor.drops.DropEngine;
import com.mobdropeditor.listener.EntityDeathListener;
import com.mobdropeditor.listener.GuiListener;
import com.mobdropeditor.mob.MobRegistry;
import com.mobdropeditor.platform.DefaultItemAdapter;
import com.mobdropeditor.platform.ItemAdapter;
import com.mobdropeditor.platform.scheduler.BukkitSchedulerAdapter;
import com.mobdropeditor.platform.scheduler.FoliaSchedulerAdapter;
import com.mobdropeditor.platform.scheduler.SchedulerAdapter;
import org.bukkit.plugin.java.JavaPlugin;

public class MobDropEditorPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private MobRegistry mobRegistry;
    private DropEngine dropEngine;
    private SchedulerAdapter schedulerAdapter;
    private ItemAdapter itemAdapter;

    @Override
    public void onEnable() {
        // Platform adapters
        this.schedulerAdapter = new FoliaSchedulerAdapter(this);
        this.itemAdapter = new DefaultItemAdapter();

        // Config & Storage
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        // Mobs & Drop Engine
        this.mobRegistry = new MobRegistry();
        this.dropEngine = new DropEngine(configManager.getStorage());

        // Event Listeners
        getServer().getPluginManager().registerEvents(new EntityDeathListener(dropEngine, itemAdapter), this);
        getServer().getPluginManager().registerEvents(new GuiListener(), this);

        // Commands
        MobDropsCommand commandExecutor = new MobDropsCommand(
                mobRegistry,
                configManager.getStorage(),
                configManager.getMessageManager(),
                schedulerAdapter,
                itemAdapter,
                this::reloadPlugin
        );

        if (getCommand("mobdrops") != null) {
            getCommand("mobdrops").setExecutor(commandExecutor);
            getCommand("mobdrops").setTabCompleter(new MobDropsTabCompleter(mobRegistry));
        }

        getLogger().info("MobDropEditor ativado com sucesso! Folia detectado: " + schedulerAdapter.isFolia());
    }

    @Override
    public void onDisable() {
        if (configManager != null && configManager.getStorage() != null) {
            configManager.getStorage().close();
        }
        getLogger().info("MobDropEditor desativado.");
    }

    public void reloadPlugin() {
        if (configManager != null) {
            configManager.load();
        }
        if (mobRegistry != null) {
            mobRegistry.registerVanillaMobs();
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MobRegistry getMobRegistry() {
        return mobRegistry;
    }

    public DropEngine getDropEngine() {
        return dropEngine;
    }

    public SchedulerAdapter getSchedulerAdapter() {
        return schedulerAdapter;
    }

    public ItemAdapter getItemAdapter() {
        return itemAdapter;
    }
}
