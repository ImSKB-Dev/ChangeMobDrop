package com.mobdropeditor.listener;

import com.mobdropeditor.drops.DropEngine;
import com.mobdropeditor.drops.DropResult;
import com.mobdropeditor.mob.VanillaDropMode;
import com.mobdropeditor.platform.ItemAdapter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class EntityDeathListener implements Listener {
    private final DropEngine dropEngine;
    private final ItemAdapter itemAdapter;

    public EntityDeathListener(DropEngine dropEngine, ItemAdapter itemAdapter) {
        this.dropEngine = dropEngine;
        this.itemAdapter = itemAdapter;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) {
            return;
        }

        String mobKey = itemAdapter.getEntityKey(entity);
        Player killer = entity.getKiller();

        int lootingLevel = 0;
        if (killer != null) {
            ItemStack mainHand = itemAdapter.getItemInMainHand(killer);
            if (mainHand != null && mainHand.hasItemMeta()) {
                try {
                    lootingLevel = mainHand.getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS);
                } catch (Throwable ignored) {}
            }
        }

        DropResult result = dropEngine.evaluateDrops(mobKey, entity, killer, lootingLevel);

        if (result.getVanillaDropMode() == VanillaDropMode.CUSTOM_ONLY) {
            event.getDrops().clear();
        }

        if (result.getVanillaDropMode() != VanillaDropMode.VANILLA_ONLY && !result.getCustomDrops().isEmpty()) {
            event.getDrops().addAll(result.getCustomDrops());
        }
    }
}
