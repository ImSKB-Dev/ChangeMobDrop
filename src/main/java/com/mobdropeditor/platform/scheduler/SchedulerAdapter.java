package com.mobdropeditor.platform.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public interface SchedulerAdapter {
    void runGlobal(Runnable runnable);
    void runGlobalAsync(Runnable runnable);
    void runGlobalLater(Runnable runnable, long delayTicks);
    void runEntity(Entity entity, Runnable runnable);
    void runLocation(Location location, Runnable runnable);
    boolean isFolia();
}
