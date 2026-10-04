package com.mobdropeditor.gui;

import com.mobdropeditor.config.MessageManager;
import com.mobdropeditor.mob.MobDefinition;
import com.mobdropeditor.mob.MobRegistry;
import com.mobdropeditor.platform.scheduler.SchedulerAdapter;
import com.mobdropeditor.storage.Storage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class MainMobMenu implements InventoryGui {
    private final MobRegistry mobRegistry;
    private final Storage storage;
    private final MessageManager messageManager;
    private final SchedulerAdapter schedulerAdapter;

    private final Inventory inventory;
    private int currentPage = 0;
    private String searchQuery = "";

    private static final int PAGE_SIZE = 45;

    public MainMobMenu(MobRegistry mobRegistry, Storage storage, MessageManager messageManager, SchedulerAdapter schedulerAdapter) {
        this.mobRegistry = mobRegistry;
        this.storage = storage;
        this.messageManager = messageManager;
        this.schedulerAdapter = schedulerAdapter;
        this.inventory = Bukkit.createInventory(this, 54, messageManager.colorize("&8Mob Drop Editor - Mobs"));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void open(Player player) {
        render();
        player.openInventory(inventory);
    }

    public void render() {
        inventory.clear();

        List<MobDefinition> mobs = mobRegistry.getAllMobs().stream()
                .filter(m -> searchQuery.isEmpty() ||
                        m.getKey().toLowerCase().contains(searchQuery.toLowerCase()) ||
                        m.getDisplayName().toLowerCase().contains(searchQuery.toLowerCase()))
                .sorted(Comparator.comparing(MobDefinition::getDisplayName))
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) mobs.size() / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        if (currentPage >= totalPages) currentPage = totalPages - 1;

        int startIndex = currentPage * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, mobs.size());

        for (int i = startIndex; i < endIndex; i++) {
            MobDefinition mob = mobs.get(i);
            int slot = i - startIndex;
            inventory.setItem(slot, createMobItem(mob));
        }

        // Bottom row controls
        // Slot 45: Prev Page
        if (currentPage > 0) {
            inventory.setItem(45, createControlItem(Material.ARROW, "&a<- Página Anterior", "&7Página " + currentPage + " de " + totalPages));
        }

        // Slot 48: Reload Config
        inventory.setItem(48, createControlItem(Material.EMERALD, "&aRecarregar Config", "&7Clique para recarregar as configurações"));

        // Slot 49: Info / Filter Info
        inventory.setItem(49, createControlItem(Material.BOOK, "&eInfo / Filtro", "&7Mobs: &f" + mobs.size(), "&7Página: &f" + (currentPage + 1) + "/" + totalPages, searchQuery.isEmpty() ? "&7Filtro: &aNenhum" : "&7Filtro: &e" + searchQuery));

        // Slot 50: Save
        inventory.setItem(50, createControlItem(Material.NETHER_STAR, "&aSalvar Tudo", "&7Garante que todas as alterações foram salvas"));

        // Slot 53: Next Page
        if (currentPage < totalPages - 1) {
            inventory.setItem(53, createControlItem(Material.ARROW, "&aPróxima Página ->", "&7Página " + (currentPage + 2) + " de " + totalPages));
        }
    }

    private ItemStack createMobItem(MobDefinition mob) {
        Material mat;
        try {
            mat = Material.valueOf(mob.getKey().replace("minecraft:", "").toUpperCase() + "_SPAWN_EGG");
        } catch (Throwable t) {
            mat = Material.ROTTEN_FLESH;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + mob.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "ID: " + ChatColor.YELLOW + mob.getKey());
            int dropCount = storage.getDrops(mob.getKey()).size();
            lore.add(ChatColor.GRAY + "Drops configurados: " + ChatColor.GREEN + dropCount);
            lore.add(ChatColor.GRAY + "Modo: " + ChatColor.AQUA + storage.getVanillaDropMode(mob.getKey()).getDisplayName());
            lore.add("");
            lore.add(ChatColor.YELLOW + "Clique para editar os drops!");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createControlItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(messageManager.colorize(name));
            List<String> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(messageManager.colorize(line));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot < 0 || slot >= 54) return;

        if (slot < PAGE_SIZE) {
            List<MobDefinition> mobs = mobRegistry.getAllMobs().stream()
                    .filter(m -> searchQuery.isEmpty() ||
                            m.getKey().toLowerCase().contains(searchQuery.toLowerCase()) ||
                            m.getDisplayName().toLowerCase().contains(searchQuery.toLowerCase()))
                    .sorted(Comparator.comparing(MobDefinition::getDisplayName))
                    .collect(Collectors.toList());

            int index = currentPage * PAGE_SIZE + slot;
            if (index < mobs.size()) {
                MobDefinition selectedMob = mobs.get(index);
                MobDropMenu dropMenu = new MobDropMenu(selectedMob, mobRegistry, storage, messageManager, schedulerAdapter, this);
                dropMenu.open(player);
            }
            return;
        }

        if (slot == 45 && currentPage > 0) {
            currentPage--;
            render();
        } else if (slot == 53) {
            currentPage++;
            render();
        } else if (slot == 48) {
            storage.init();
            player.sendMessage(messageManager.getMessage("reloaded"));
            render();
        } else if (slot == 50) {
            player.sendMessage(messageManager.getMessage("saved"));
        }
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
    }
}
