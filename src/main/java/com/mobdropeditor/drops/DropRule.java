package com.mobdropeditor.drops;

import com.mobdropeditor.item.ItemData;
import com.mobdropeditor.item.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DropRule {
    private String id;
    private double chance; // e.g. 50.0 for 50%, 0.5 for 0.5%, 0.01 for 0.01%
    private int minAmount;
    private int maxAmount;
    private boolean enabled;
    private ItemData itemData;
    private List<DropCondition> conditions;

    public DropRule() {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.chance = 100.0;
        this.minAmount = 1;
        this.maxAmount = 1;
        this.enabled = true;
        this.itemData = new ItemData();
        this.conditions = new ArrayList<>();
    }

    public DropRule(String id, double chance, int minAmount, int maxAmount, boolean enabled, ItemData itemData) {
        this.id = id != null ? id : UUID.randomUUID().toString().substring(0, 8);
        this.chance = chance;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.enabled = enabled;
        this.itemData = itemData != null ? itemData : new ItemData();
        this.conditions = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getChance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = chance;
    }

    public int getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(int minAmount) {
        this.minAmount = minAmount;
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(int maxAmount) {
        this.maxAmount = maxAmount;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ItemData getItemData() {
        return itemData;
    }

    public void setItemData(ItemData itemData) {
        this.itemData = itemData;
    }

    public List<DropCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<DropCondition> conditions) {
        this.conditions = conditions != null ? conditions : new ArrayList<>();
    }

    public ItemStack createItemStack(int amount) {
        ItemStack item = ItemSerializer.deserialize(itemData);
        if (item != null) {
            item.setAmount(Math.max(1, amount));
        }
        return item;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DropRule dropRule = (DropRule) o;
        return Objects.equals(id, dropRule.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
