package com.mobdropeditor.gui;

import com.mobdropeditor.config.MessageManager;
import com.mobdropeditor.drops.DropRule;
import com.mobdropeditor.item.ItemData;
import com.mobdropeditor.item.ItemSerializer;
import com.mobdropeditor.mob.MobDefinition;
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

import java.util.ArrayList;
import java.util.List;

public class DropEditMenu implements InventoryGui {
    private final MobDefinition mobDefinition;
    private final DropRule dropRule;
    private final Storage storage;
    private final MessageManager messageManager;
    private final SchedulerAdapter schedulerAdapter;
    private final MobDropMenu parentMenu;

    private final Inventory inventory;

    public DropEditMenu(MobDefinition mobDefinition, DropRule dropRule, Storage storage, MessageManager messageManager, SchedulerAdapter schedulerAdapter, MobDropMenu parentMenu) {
        this.mobDefinition = mobDefinition;
        this.dropRule = dropRule;
        this.storage = storage;
        this.messageManager = messageManager;
        this.schedulerAdapter = schedulerAdapter;
        this.parentMenu = parentMenu;
        this.inventory = Bukkit.createInventory(this, 27, messageManager.colorize("&8Editar Drop: " + dropRule.getId()));
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

        // Slot 4: Item Display Preview
        ItemStack previewItem = ItemSerializer.deserialize(dropRule.getItemData());
        if (previewItem == null || previewItem.getType() == Material.AIR) {
            previewItem = new ItemStack(Material.PAPER);
        }
        inventory.setItem(4, previewItem);

        // Slot 10: Chance Adjustment
        inventory.setItem(10, createControlItem(Material.GOLD_NUGGET,
                "&eChance: &f" + dropRule.getChance() + "%",
                "&7Clique Esquerdo: &a+5%",
                "&7Clique Esquerdo + Shift: &a+0.5%",
                "&7Clique Direito: &c-5%",
                "&7Clique Direito + Shift: &c-0.5%"));

        // Slot 12: Min Amount
        inventory.setItem(12, createControlItem(Material.CHEST,
                "&eQtd Mínima: &f" + dropRule.getMinAmount(),
                "&7Clique Esquerdo: &a+1",
                "&7Clique Direito: &c-1"));

        // Slot 14: Max Amount
        inventory.setItem(14, createControlItem(Material.TRAPPED_CHEST,
                "&eQtd Máxima: &f" + dropRule.getMaxAmount(),
                "&7Clique Esquerdo: &a+1",
                "&7Clique Direito: &c-1"));

        // Slot 16: Replace item with Item in Hand
        inventory.setItem(16, createControlItem(Material.ANVIL,
                "&aUsar Item da Mão",
                "&7Substitui o item deste drop pelo",
                "&7item que você está segurando"));

        // Slot 18: Back
        inventory.setItem(18, createControlItem(Material.BARRIER, "&c<- Voltar", "&7Voltar aos drops do mob"));

        // Slot 22: Enable / Disable Toggle
        inventory.setItem(22, createControlItem(dropRule.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                dropRule.isEnabled() ? "&aStatus: Ativado" : "&cStatus: Desativado",
                "&7Clique para alternar status"));

        // Slot 26: Save & Close
        inventory.setItem(26, createControlItem(Material.EMERALD, "&aSalvar", "&7Salva as alterações no banco/arquivo"));
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

        if (slot < 0 || slot >= 27) return;

        if (slot == 10) { // Chance
            double change = event.isShiftClick() ? 0.5 : 5.0;
            if (event.getClick().isLeftClick()) {
                dropRule.setChance(Math.min(100.0, Math.round((dropRule.getChance() + change) * 100.0) / 100.0));
            } else if (event.getClick().isRightClick()) {
                dropRule.setChance(Math.max(0.0001, Math.round((dropRule.getChance() - change) * 100.0) / 100.0));
            }
            render();
        } else if (slot == 12) { // Min Amount
            if (event.getClick().isLeftClick()) {
                dropRule.setMinAmount(dropRule.getMinAmount() + 1);
            } else if (event.getClick().isRightClick()) {
                dropRule.setMinAmount(Math.max(1, dropRule.getMinAmount() - 1));
            }
            if (dropRule.getMaxAmount() < dropRule.getMinAmount()) {
                dropRule.setMaxAmount(dropRule.getMinAmount());
            }
            render();
        } else if (slot == 14) { // Max Amount
            if (event.getClick().isLeftClick()) {
                dropRule.setMaxAmount(dropRule.getMaxAmount() + 1);
            } else if (event.getClick().isRightClick()) {
                dropRule.setMaxAmount(Math.max(dropRule.getMinAmount(), dropRule.getMaxAmount() - 1));
            }
            render();
        } else if (slot == 16) { // Copy item from hand
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand != null && hand.getType() != Material.AIR) {
                dropRule.setItemData(ItemSerializer.serialize(hand));
                player.sendMessage(messageManager.colorize("&aItem do drop atualizado com o item da sua mão!"));
                render();
            } else {
                player.sendMessage(messageManager.colorize("&cVocê precisa estar segurando um item na mão principal!"));
            }
        } else if (slot == 18) { // Back
            save();
            if (parentMenu != null) {
                parentMenu.open(player);
            }
        } else if (slot == 22) { // Toggle status
            dropRule.setEnabled(!dropRule.isEnabled());
            render();
        } else if (slot == 26) { // Save & Return
            save();
            player.sendMessage(messageManager.getMessage("saved"));
            if (parentMenu != null) {
                parentMenu.open(player);
            }
        }
    }

    private void save() {
        List<DropRule> drops = storage.getDrops(mobDefinition.getKey());
        boolean found = false;
        for (int i = 0; i < drops.size(); i++) {
            if (drops.get(i).getId().equalsIgnoreCase(dropRule.getId())) {
                drops.set(i, dropRule);
                found = true;
                break;
            }
        }
        if (!found) {
            drops.add(dropRule);
        }
        storage.saveDrops(mobDefinition.getKey(), drops);
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        save();
    }
}
