package com.mobdropeditor.drops;

import com.mobdropeditor.mob.VanillaDropMode;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DropResult {
    private final VanillaDropMode vanillaDropMode;
    private final List<ItemStack> customDrops;

    public DropResult(VanillaDropMode vanillaDropMode, List<ItemStack> customDrops) {
        this.vanillaDropMode = vanillaDropMode;
        this.customDrops = customDrops;
    }

    public VanillaDropMode getVanillaDropMode() {
        return vanillaDropMode;
    }

    public List<ItemStack> getCustomDrops() {
        return customDrops;
    }
}
