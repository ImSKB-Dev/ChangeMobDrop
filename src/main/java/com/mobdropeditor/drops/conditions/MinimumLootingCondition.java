package com.mobdropeditor.drops.conditions;

import com.mobdropeditor.drops.DropCondition;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class MinimumLootingCondition implements DropCondition {
    private final int minLooting;

    public MinimumLootingCondition(int minLooting) {
        this.minLooting = minLooting;
    }

    @Override
    public boolean evaluate(LivingEntity entity, Player killer, int lootingLevel) {
        return lootingLevel >= minLooting;
    }

    @Override
    public String getType() {
        return "MINIMUM_LOOTING";
    }

    public int getMinLooting() {
        return minLooting;
    }
}
