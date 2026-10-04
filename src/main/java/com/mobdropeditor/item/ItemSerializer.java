package com.mobdropeditor.item;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

public class ItemSerializer {

    public static ItemData serialize(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return new ItemData("AIR", 1);
        }

        ItemData data = new ItemData();
        data.setMaterial(item.getType().name());
        data.setAmount(item.getAmount());

        ItemMeta meta = null;
        try {
            meta = item.getItemMeta();
        } catch (Throwable ignored) {}

        if (meta != null) {
            try {
                if (meta.hasDisplayName()) {
                    data.setDisplayName(meta.getDisplayName());
                }
            } catch (Throwable ignored) {}

            try {
                if (meta.hasLore()) {
                    data.setLore(meta.getLore());
                }
            } catch (Throwable ignored) {}

            try {
                if (meta.hasEnchants()) {
                    Map<String, Integer> enchants = new HashMap<>();
                    for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                        try {
                            enchants.put(entry.getKey().getKey().toString(), entry.getValue());
                        } catch (Throwable t) {
                            enchants.put(entry.getKey().getName(), entry.getValue());
                        }
                    }
                    data.setEnchantments(enchants);
                }
            } catch (Throwable ignored) {}

            try {
                if (meta.hasCustomModelData()) {
                    data.setCustomModelData(meta.getCustomModelData());
                }
            } catch (Throwable ignored) {}

            try {
                data.setUnbreakable(meta.isUnbreakable());
            } catch (Throwable ignored) {}
        }

        // Try serializing to Base64
        try {
            String base64 = toBase64(item);
            data.setRawNbtBase64(base64);
        } catch (Throwable ignored) {}

        return data;
    }

    public static ItemStack deserialize(ItemData data) {
        if (data == null || data.getMaterial() == null || data.getMaterial().equalsIgnoreCase("AIR")) {
            return new ItemStack(Material.AIR);
        }

        // Attempt Base64 deserialization first if present
        if (data.getRawNbtBase64() != null && !data.getRawNbtBase64().isEmpty()) {
            try {
                ItemStack item = fromBase64(data.getRawNbtBase64());
                if (item != null) {
                    if (data.getAmount() > 0) {
                        item.setAmount(data.getAmount());
                    }
                    return item;
                }
            } catch (Throwable ignored) {
                // Fallback to manual reconstruction
            }
        }

        Material mat = Material.matchMaterial(data.getMaterial());
        if (mat == null) {
            try {
                mat = Material.valueOf(data.getMaterial().toUpperCase());
            } catch (Throwable t) {
                mat = Material.STONE;
            }
        }

        ItemStack item = new ItemStack(mat, Math.max(1, data.getAmount()));
        ItemMeta meta = null;
        try {
            meta = item.getItemMeta();
        } catch (Throwable ignored) {}

        if (meta != null) {
            try {
                if (data.getDisplayName() != null) {
                    meta.setDisplayName(data.getDisplayName());
                }
            } catch (Throwable ignored) {}

            try {
                if (data.getLore() != null && !data.getLore().isEmpty()) {
                    meta.setLore(data.getLore());
                }
            } catch (Throwable ignored) {}

            try {
                if (data.getEnchantments() != null) {
                    for (Map.Entry<String, Integer> entry : data.getEnchantments().entrySet()) {
                        Enchantment enchant = getEnchantment(entry.getKey());
                        if (enchant != null) {
                            meta.addEnchant(enchant, entry.getValue(), true);
                        }
                    }
                }
            } catch (Throwable ignored) {}

            try {
                if (data.getCustomModelData() != null) {
                    meta.setCustomModelData(data.getCustomModelData());
                }
            } catch (Throwable ignored) {}

            try {
                if (data.isUnbreakable() != null) {
                    meta.setUnbreakable(data.isUnbreakable());
                }
            } catch (Throwable ignored) {}

            try {
                item.setItemMeta(meta);
            } catch (Throwable ignored) {}
        }

        return item;
    }

    public static String toBase64(ItemStack item) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
        dataOutput.writeObject(item);
        dataOutput.close();
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    public static ItemStack fromBase64(String base64) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
        ItemStack item = (ItemStack) dataInput.readObject();
        dataInput.close();
        return item;
    }

    private static Enchantment getEnchantment(String key) {
        if (key == null) return null;
        try {
            if (key.contains(":")) {
                String[] parts = key.split(":");
                return Enchantment.getByKey(new NamespacedKey(parts[0], parts[1]));
            }
        } catch (Throwable ignored) {}
        try {
            return Enchantment.getByName(key.toUpperCase());
        } catch (Throwable ignored) {
            return null;
        }
    }
}
