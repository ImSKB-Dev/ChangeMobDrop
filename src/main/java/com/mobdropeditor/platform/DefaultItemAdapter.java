package com.mobdropeditor.platform;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DefaultItemAdapter implements ItemAdapter {

    @Override
    public ItemStack getItemInMainHand(Player player) {
        if (player == null) return null;
        try {
            return player.getInventory().getItemInMainHand();
        } catch (NoSuchMethodError e) {
            // Pre 1.9 Bukkit fallback
            return player.getInventory().getItemInHand();
        }
    }

    @Override
    public void setItemInMainHand(Player player, ItemStack item) {
        if (player == null) return;
        try {
            player.getInventory().setItemInMainHand(item);
        } catch (NoSuchMethodError e) {
            player.getInventory().setItemInHand(item);
        }
    }

    @Override
    public String getEntityKey(Entity entity) {
        if (entity == null) return "minecraft:unknown";
        try {
            return entity.getType().getKey().toString().toLowerCase();
        } catch (Throwable t) {
            return "minecraft:" + entity.getType().name().toLowerCase();
        }
    }
}
