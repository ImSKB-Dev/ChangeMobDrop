package com.mobdropeditor.drops.conditions;

import com.mobdropeditor.drops.DropCondition;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class PlayerKillerCondition implements DropCondition {
    private final boolean requirePlayerKiller;

    public PlayerKillerCondition(boolean requirePlayerKiller) {
        this.requirePlayerKiller = requirePlayerKiller;
    }

    @Override
    public boolean evaluate(LivingEntity entity, Player killer, int lootingLevel) {
        if (!requirePlayerKiller) {
            return true;
        }
        return killer != null;
    }

    @Override
    public String getType() {
        return "PLAYER_KILLER";
    }

    public boolean isRequirePlayerKiller() {
        return requirePlayerKiller;
    }
}
