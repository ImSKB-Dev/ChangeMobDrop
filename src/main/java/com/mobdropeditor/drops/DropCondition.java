package com.mobdropeditor.drops;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface DropCondition {
    boolean evaluate(LivingEntity entity, Player killer, int lootingLevel);

    String getType();
}
