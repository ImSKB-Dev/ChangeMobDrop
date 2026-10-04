package com.mobdropeditor.gui;

import com.mobdropeditor.config.MessageManager;
import com.mobdropeditor.drops.DropRule;
import com.mobdropeditor.item.ItemData;
import com.mobdropeditor.item.ItemSerializer;
import com.mobdropeditor.mob.MobDefinition;
import com.mobdropeditor.mob.MobRegistry;
import com.mobdropeditor.mob.VanillaDropMode;
import com.mobdropeditor.platform.scheduler.SchedulerAdapter;
import com.mobdropeditor.storage.Storage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MobDropMenu implements InventoryGui {
    private final MobDefinition mobDefinition;
    private final MobRegistry mobRegistry;
    private final Storage storage;
    private final MessageManager messageManager;
    private final SchedulerAdapter schedulerAdapter;
    private final MainMobMenu parentMenu;

    private final Inventory inventory;

    public MobDropMenu(MobDefinition mobDefinition, MobRegistry mobRegistry, Storage storage, MessageManager messageManager, SchedulerAdapter schedulerAdapter, MainMobMenu parentMenu) {
        this.mobDefinition = mobDefinition;
        this.mobRegistry = mobRegistry;
        this.storage = storage;
        this.messageManager = messageManager;
        this.schedulerAdapter = schedulerAdapter;
        this.parentMenu = parentMenu;
        this.inventory = Bukkit.createInventory(this, 54, messageManager.colorize("&8Drops: " + mobDefinition.getDisplayName()));
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

        List<DropRule> drops = storage.getDrops(mobDefinition.getKey());
        int maxSlots = Math.min(drops.size(), 45);

        for (int i = 0; i < maxSlots; i++) {
            DropRule rule = drops.get(i);
            inventory.setItem(i, createDropItem(rule));
        }

        // Slot 45: Back
        inventory.setItem(45, createControlItem(Material.BARRIER, "&c<- Voltar", "&7Voltar para o menu principal de mobs"));

        // Slot 47: Toggle Vanilla Drop Mode
        VanillaDropMode mode = storage.getVanillaDropMode(mobDefinition.getKey());
        inventory.setItem(47, createControlItem(Material.COMPARATOR, "&eModo de Drop Vanilla", "&7Atual: &f" + mode.getDisplayName(), "", "&eClique para alternar modo!"));

        // Slot 49: Add Drop
        inventory.setItem(49, createControlItem(Material.NETHER_STAR, "&a+ Adicionar Drop", "&7Clique para adicionar um novo drop", "&7pode ser criado do item na mão ou novo item"));

        // Slot 51: Reset Config
        inventory.setItem(51, createControlItem(Material.TNT, "&cRestaurar Padrão", "&7Clique para apagar todos os drops customizados deste mob"));

        // Slot 53: Save
        inventory.setItem(53, createControlItem(Material.EMERALD, "&aSalvar Configuração", "&7Garante a gravação no disco"));
    }

    private ItemStack createDropItem(DropRule rule) {
        ItemStack item = ItemSerializer.deserialize(rule.getItemData());
        if (item == null || item.getType() == Material.AIR) {
            item = new ItemStack(Material.PAPER);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = meta.hasDisplayName() ? meta.getDisplayName() : rule.getItemData().getMaterial();
            meta.setDisplayName(ChatColor.GOLD + name + (rule.isEnabled() ? "" : ChatColor.RED + " [DESATIVADO]"));

            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Chance: " + ChatColor.GREEN + rule.getChance() + "%");
            lore.add(ChatColor.GRAY + "Quantidade: " + ChatColor.YELLOW + rule.getMinAmount() + " - " + rule.getMaxAmount());
            lore.add(ChatColor.GRAY + "Status: " + (rule.isEnabled() ? ChatColor.GREEN + "Ativo" : ChatColor.RED + "Inativo"));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Clique Esquerdo: " + ChatColor.WHITE + "Editar Drop");
            lore.add(ChatColor.YELLOW + "Clique Direito: " + ChatColor.WHITE + "Ativar/Desativar");
            lore.add(ChatColor.RED + "Shift + Clique Direito: " + ChatColor.WHITE + "Remover");
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

        List<DropRule> drops = storage.getDrops(mobDefinition.getKey());

        if (slot < 45) {
            if (slot < drops.size()) {
                DropRule rule = drops.get(slot);
                if (event.getClick() == ClickType.SHIFT_RIGHT) {
                    drops.remove(slot);
                    schedulerAdapter.runGlobalAsync(() -> storage.saveDrops(mobDefinition.getKey(), drops));
                    render();
                } else if (event.getClick().isRightClick()) {
                    rule.setEnabled(!rule.isEnabled());
                    schedulerAdapter.runGlobalAsync(() -> storage.saveDrops(mobDefinition.getKey(), drops));
                    render();
                } else if (event.getClick().isLeftClick()) {
                    DropEditMenu editMenu = new DropEditMenu(mobDefinition, rule, storage, messageManager, schedulerAdapter, this);
                    editMenu.open(player);
                }
            }
            return;
        }

        if (slot == 45) {
            if (parentMenu != null) {
                parentMenu.open(player);
            }
        } else if (slot == 47) {
            VanillaDropMode currentMode = storage.getVanillaDropMode(mobDefinition.getKey());
            VanillaDropMode nextMode = currentMode.next();
            schedulerAdapter.runGlobalAsync(() -> storage.setVanillaDropMode(mobDefinition.getKey(), nextMode));
            render();
        } else if (slot == 49) {
            DropRule newRule = new DropRule();
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand != null && hand.getType() != Material.AIR) {
                newRule.setItemData(ItemSerializer.serialize(hand));
            } else {
                newRule.setItemData(new ItemData("DIAMOND", 1));
            }
            drops.add(newRule);
            schedulerAdapter.runGlobalAsync(() -> storage.saveDrops(mobDefinition.getKey(), drops));

            DropEditMenu editMenu = new DropEditMenu(mobDefinition, newRule, storage, messageManager, schedulerAdapter, this);
            editMenu.open(player);
        } else if (slot == 51) {
            schedulerAdapter.runGlobalAsync(() -> storage.resetConfig(mobDefinition.getKey()));
            render();
        } else if (slot == 53) {
            schedulerAdapter.runGlobalAsync(() -> storage.saveDrops(mobDefinition.getKey(), drops));
            player.sendMessage(messageManager.getMessage("saved"));
        }
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
    }
}
