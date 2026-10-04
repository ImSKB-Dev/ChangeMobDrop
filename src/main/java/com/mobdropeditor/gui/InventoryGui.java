package com.mobdropeditor.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public interface InventoryGui extends InventoryHolder {
    void open(Player player);
    void handleClick(InventoryClickEvent event);
    void handleClose(InventoryCloseEvent event);
}
