package com.mobdropeditor.platform;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public interface ItemAdapter {
    ItemStack getItemInMainHand(Player player);
    void setItemInMainHand(Player player, ItemStack item);
    String getEntityKey(Entity entity);
}
