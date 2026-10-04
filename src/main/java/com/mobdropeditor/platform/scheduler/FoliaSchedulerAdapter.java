package com.mobdropeditor.platform.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class FoliaSchedulerAdapter implements SchedulerAdapter {
    private final Plugin plugin;
    private final boolean isFoliaServer;

    public FoliaSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        boolean foliaCheck = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            foliaCheck = true;
        } catch (ClassNotFoundException e) {
            foliaCheck = false;
        }
        this.isFoliaServer = foliaCheck;
    }

    @Override
    public boolean isFolia() {
        return isFoliaServer;
    }

    @Override
    public void runGlobal(Runnable runnable) {
        if (!isFoliaServer) {
            Bukkit.getScheduler().runTask(plugin, runnable);
            return;
        }
        try {
            Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
            Method runMethod = globalScheduler.getClass().getMethod("run", Plugin.class, java.util.function.Consumer.class);
            runMethod.invoke(globalScheduler, plugin, (java.util.function.Consumer<Object>) o -> runnable.run());
        } catch (Exception e) {
            runnable.run();
        }
    }

    @Override
    public void runGlobalAsync(Runnable runnable) {
        if (!isFoliaServer) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            return;
        }
        try {
            Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
            Method runNowMethod = asyncScheduler.getClass().getMethod("runNow", Plugin.class, java.util.function.Consumer.class);
            runNowMethod.invoke(asyncScheduler, plugin, (java.util.function.Consumer<Object>) o -> runnable.run());
        } catch (Exception e) {
            new Thread(runnable).start();
        }
    }

    @Override
    public void runGlobalLater(Runnable runnable, long delayTicks) {
        if (!isFoliaServer) {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
            return;
        }
        try {
            Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
            Method runDelayedMethod = globalScheduler.getClass().getMethod("runDelayed", Plugin.class, java.util.function.Consumer.class, long.class);
            runDelayedMethod.invoke(globalScheduler, plugin, (java.util.function.Consumer<Object>) o -> runnable.run(), Math.max(1L, delayTicks));
        } catch (Exception e) {
            runnable.run();
        }
    }

    @Override
    public void runEntity(Entity entity, Runnable runnable) {
        if (!isFoliaServer || entity == null) {
            Bukkit.getScheduler().runTask(plugin, runnable);
            return;
        }
        try {
            Method getSchedulerMethod = entity.getClass().getMethod("getScheduler");
            Object entityScheduler = getSchedulerMethod.invoke(entity);
            Method runMethod = entityScheduler.getClass().getMethod("run", Plugin.class, java.util.function.Consumer.class, Runnable.class);
            runMethod.invoke(entityScheduler, plugin, (java.util.function.Consumer<Object>) o -> runnable.run(), null);
        } catch (Exception e) {
            runnable.run();
        }
    }

    @Override
    public void runLocation(Location location, Runnable runnable) {
        if (!isFoliaServer || location == null || location.getWorld() == null) {
            Bukkit.getScheduler().runTask(plugin, runnable);
            return;
        }
        try {
            Object regionScheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
            Method runMethod = regionScheduler.getClass().getMethod("run", Plugin.class, Location.class, java.util.function.Consumer.class);
            runMethod.invoke(regionScheduler, plugin, location, (java.util.function.Consumer<Object>) o -> runnable.run());
        } catch (Exception e) {
            runnable.run();
        }
    }
}
