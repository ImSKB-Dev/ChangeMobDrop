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
import com.mobdropeditor.platform.scheduler.FoliaSchedulerAdapter;
import com.mobdropeditor.platform.scheduler.SchedulerAdapter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

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

        // Register Command
        MobDropsCommand commandExecutor = new MobDropsCommand(
                mobRegistry,
                configManager.getStorage(),
                configManager.getMessageManager(),
                schedulerAdapter,
                itemAdapter,
                this::reloadPlugin
        );
        MobDropsTabCompleter tabCompleter = new MobDropsTabCompleter(mobRegistry);

        registerCommandSafely(commandExecutor, tabCompleter);

        getLogger().info("MobDropEditor ativado com sucesso! Folia detectado: " + schedulerAdapter.isFolia());
    }

    private void registerCommandSafely(MobDropsCommand executor, MobDropsTabCompleter tabCompleter) {
        // First try registering dynamically via CommandMap (required on Paper/Folia paper-plugins)
        boolean registeredViaCommandMap = false;
        try {
            Field commandMapField = getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(getServer());
            if (commandMap != null) {
                BukkitCommand customCommand = new BukkitCommand("mobdrops", "MobDropEditor main command", "/mobdrops", Arrays.asList("mobdrop", "mde")) {
                    @Override
                    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                        return executor.onCommand(sender, this, commandLabel, args);
                    }

                    @Override
                    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
                        List<String> list = tabCompleter.onTabComplete(sender, this, alias, args);
                        return list != null ? list : super.tabComplete(sender, alias, args);
                    }
                };
                customCommand.setPermission("mobdrops.admin");
                commandMap.register("mobdropeditor", customCommand);
                registeredViaCommandMap = true;
            }
        } catch (Throwable ignored) {}

        // Fallback to getCommand for traditional Bukkit/Spigot servers if CommandMap failed
        if (!registeredViaCommandMap) {
            try {
                if (getCommand("mobdrops") != null) {
                    getCommand("mobdrops").setExecutor(executor);
                    getCommand("mobdrops").setTabCompleter(tabCompleter);
                }
            } catch (Throwable ignored) {}
        }
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
