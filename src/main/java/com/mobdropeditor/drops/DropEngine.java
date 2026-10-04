package com.mobdropeditor.drops;

import com.mobdropeditor.mob.VanillaDropMode;
import com.mobdropeditor.storage.Storage;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DropEngine {
    private final Storage storage;

    public DropEngine(Storage storage) {
        this.storage = storage;
    }

    public DropResult evaluateDrops(String mobKey, LivingEntity entity, Player killer, int lootingLevel) {
        VanillaDropMode mode = storage.getVanillaDropMode(mobKey);
        List<DropRule> rules = storage.getDrops(mobKey);
        List<ItemStack> customDrops = new ArrayList<>();

        if (mode == VanillaDropMode.VANILLA_ONLY || rules.isEmpty()) {
            return new DropResult(mode, customDrops);
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (DropRule rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }

            // Check conditions if any
            boolean conditionsMet = true;
            if (rule.getConditions() != null) {
                for (DropCondition condition : rule.getConditions()) {
                    if (!condition.evaluate(entity, killer, lootingLevel)) {
                        conditionsMet = false;
                        break;
                    }
                }
            }

            if (!conditionsMet) {
                continue;
            }

            // Roll chance (0.0 to 100.0)
            double roll = random.nextDouble() * 100.0;
            if (roll <= rule.getChance()) {
                int min = rule.getMinAmount();
                int max = rule.getMaxAmount();
                int amount;
                if (max <= min) {
                    amount = min;
                } else {
                    amount = random.nextInt(min, max + 1);
                }

                if (amount > 0) {
                    ItemStack drop = rule.createItemStack(amount);
                    if (drop != null) {
                        customDrops.add(drop);
                    }
                }
            }
        }

        return new DropResult(mode, customDrops);
    }
}
