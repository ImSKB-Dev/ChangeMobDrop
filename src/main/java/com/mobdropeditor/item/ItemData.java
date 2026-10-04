package com.mobdropeditor.item;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ItemData {
    private String material;
    private int amount;
    private String displayName;
    private List<String> lore;
    private Map<String, Integer> enchantments;
    private Integer customModelData;
    private Boolean unbreakable;
    private String rawNbtBase64; // Fallback Base64 serialized ItemStack for complete NBT / Component fidelity

    public ItemData() {
        this.material = "STONE";
        this.amount = 1;
        this.lore = new ArrayList<>();
        this.enchantments = new HashMap<>();
    }

    public ItemData(String material, int amount) {
        this.material = material;
        this.amount = amount;
        this.lore = new ArrayList<>();
        this.enchantments = new HashMap<>();
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore != null ? lore : new ArrayList<>();
    }

    public Map<String, Integer> getEnchantments() {
        return enchantments;
    }

    public void setEnchantments(Map<String, Integer> enchantments) {
        this.enchantments = enchantments != null ? enchantments : new HashMap<>();
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(Integer customModelData) {
        this.customModelData = customModelData;
    }

    public Boolean isUnbreakable() {
        return unbreakable;
    }

    public void setUnbreakable(Boolean unbreakable) {
        this.unbreakable = unbreakable;
    }

    public String getRawNbtBase64() {
        return rawNbtBase64;
    }

    public void setRawNbtBase64(String rawNbtBase64) {
        this.rawNbtBase64 = rawNbtBase64;
    }
}
