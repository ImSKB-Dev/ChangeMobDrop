package com.mobdropeditor.storage;

import com.mobdropeditor.drops.DropRule;
import com.mobdropeditor.item.ItemData;
import com.mobdropeditor.mob.VanillaDropMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class YamlStorage implements Storage {
    private final File dataFile;
    private final Map<String, List<DropRule>> dropsMap = new ConcurrentHashMap<>();
    private final Map<String, VanillaDropMode> dropModeMap = new ConcurrentHashMap<>();

    public YamlStorage(File dataDirectory) {
        this.dataFile = new File(dataDirectory, "drops.yml");
    }

    @Override
    public void init() {
        if (!dataFile.getParentFile().exists()) {
            dataFile.getParentFile().mkdirs();
        }
        load();
    }

    @Override
    public void close() {
        save();
    }

    private synchronized void load() {
        dropsMap.clear();
        dropModeMap.clear();

        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection mobsSection = config.getConfigurationSection("mobs");
        if (mobsSection == null) return;

        for (String key : mobsSection.getKeys(false)) {
            ConfigurationSection mobSec = mobsSection.getConfigurationSection(key);
            if (mobSec == null) continue;

            String modeStr = mobSec.getString("drop-mode", VanillaDropMode.VANILLA_AND_CUSTOM.name());
            VanillaDropMode mode;
            try {
                mode = VanillaDropMode.valueOf(modeStr.toUpperCase());
            } catch (Exception e) {
                mode = VanillaDropMode.VANILLA_AND_CUSTOM;
            }
            dropModeMap.put(key.toLowerCase(), mode);

            List<DropRule> rules = new ArrayList<>();
            List<Map<?, ?>> rawDrops = mobSec.getMapList("drops");
            for (Map<?, ?> raw : rawDrops) {
                String id = raw.get("id") != null ? raw.get("id").toString() : UUID.randomUUID().toString().substring(0, 8);
                double chance = raw.get("chance") instanceof Number ? ((Number) raw.get("chance")).doubleValue() : 100.0;
                int min = raw.get("min") instanceof Number ? ((Number) raw.get("min")).intValue() : 1;
                int max = raw.get("max") instanceof Number ? ((Number) raw.get("max")).intValue() : 1;
                boolean enabled = raw.get("enabled") != null ? Boolean.parseBoolean(raw.get("enabled").toString()) : true;

                ItemData itemData = new ItemData();
                if (raw.get("item") instanceof Map) {
                    Map<?, ?> itemMap = (Map<?, ?>) raw.get("item");
                    if (itemMap.get("material") != null) {
                        itemData.setMaterial(itemMap.get("material").toString());
                    }
                    if (itemMap.get("amount") instanceof Number) {
                        itemData.setAmount(((Number) itemMap.get("amount")).intValue());
                    }
                    if (itemMap.get("display-name") != null) {
                        itemData.setDisplayName(itemMap.get("display-name").toString());
                    }
                    if (itemMap.get("lore") instanceof List) {
                        List<String> lore = new ArrayList<>();
                        for (Object l : (List<?>) itemMap.get("lore")) {
                            lore.add(l.toString());
                        }
                        itemData.setLore(lore);
                    }
                    if (itemMap.get("enchantments") instanceof Map) {
                        Map<String, Integer> enchants = new HashMap<>();
                        for (Map.Entry<?, ?> entry : ((Map<?, ?>) itemMap.get("enchantments")).entrySet()) {
                            if (entry.getValue() instanceof Number) {
                                enchants.put(entry.getKey().toString(), ((Number) entry.getValue()).intValue());
                            }
                        }
                        itemData.setEnchantments(enchants);
                    }
                    if (itemMap.get("custom-model-data") instanceof Number) {
                        itemData.setCustomModelData(((Number) itemMap.get("custom-model-data")).intValue());
                    }
                    if (itemMap.get("unbreakable") != null) {
                        itemData.setUnbreakable(Boolean.parseBoolean(itemMap.get("unbreakable").toString()));
                    }
                    if (itemMap.get("raw-nbt") != null) {
                        itemData.setRawNbtBase64(itemMap.get("raw-nbt").toString());
                    }
                }

                DropRule rule = new DropRule(id, chance, min, max, enabled, itemData);
                rules.add(rule);
            }
            dropsMap.put(key.toLowerCase(), rules);
        }
    }

    public synchronized void save() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection mobsSection = config.createSection("mobs");

        Set<String> keys = new HashSet<>(dropsMap.keySet());
        keys.addAll(dropModeMap.keySet());

        for (String key : keys) {
            ConfigurationSection mobSec = mobsSection.createSection(key);
            VanillaDropMode mode = dropModeMap.getOrDefault(key, VanillaDropMode.VANILLA_AND_CUSTOM);
            mobSec.set("drop-mode", mode.name());

            List<Map<String, Object>> rawDrops = new ArrayList<>();
            List<DropRule> rules = dropsMap.getOrDefault(key, Collections.emptyList());
            for (DropRule rule : rules) {
                Map<String, Object> raw = new LinkedHashMap<>();
                raw.put("id", rule.getId());
                raw.put("chance", rule.getChance());
                raw.put("min", rule.getMinAmount());
                raw.put("max", rule.getMaxAmount());
                raw.put("enabled", rule.isEnabled());

                ItemData item = rule.getItemData();
                Map<String, Object> itemMap = new LinkedHashMap<>();
                if (item != null) {
                    itemMap.put("material", item.getMaterial());
                    itemMap.put("amount", item.getAmount());
                    if (item.getDisplayName() != null) itemMap.put("display-name", item.getDisplayName());
                    if (item.getLore() != null && !item.getLore().isEmpty()) itemMap.put("lore", item.getLore());
                    if (item.getEnchantments() != null && !item.getEnchantments().isEmpty()) itemMap.put("enchantments", item.getEnchantments());
                    if (item.getCustomModelData() != null) itemMap.put("custom-model-data", item.getCustomModelData());
                    if (item.isUnbreakable() != null) itemMap.put("unbreakable", item.isUnbreakable());
                    if (item.getRawNbtBase64() != null) itemMap.put("raw-nbt", item.getRawNbtBase64());
                }
                raw.put("item", itemMap);
                rawDrops.add(raw);
            }
            mobSec.set("drops", rawDrops);
        }

        // Atomic write to prevent file corruption
        File tempFile = new File(dataFile.getAbsolutePath() + ".tmp");
        try {
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
                writer.write(config.saveToString());
            }
            Files.move(tempFile.toPath(), dataFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            if (tempFile.exists()) {
                tempFile.delete();
            }
            try {
                config.save(dataFile);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public List<DropRule> getDrops(String mobKey) {
        return dropsMap.getOrDefault(mobKey.toLowerCase(), new ArrayList<>());
    }

    @Override
    public void saveDrops(String mobKey, List<DropRule> drops) {
        dropsMap.put(mobKey.toLowerCase(), new ArrayList<>(drops));
        save();
    }

    @Override
    public VanillaDropMode getVanillaDropMode(String mobKey) {
        return dropModeMap.getOrDefault(mobKey.toLowerCase(), VanillaDropMode.VANILLA_AND_CUSTOM);
    }

    @Override
    public void setVanillaDropMode(String mobKey, VanillaDropMode mode) {
        dropModeMap.put(mobKey.toLowerCase(), mode);
        save();
    }

    @Override
    public Map<String, List<DropRule>> getAllDrops() {
        return Collections.unmodifiableMap(dropsMap);
    }

    @Override
    public Map<String, VanillaDropMode> getAllVanillaDropModes() {
        return Collections.unmodifiableMap(dropModeMap);
    }

    @Override
    public void copyConfig(String sourceMobKey, String targetMobKey) {
        List<DropRule> sourceDrops = getDrops(sourceMobKey);
        VanillaDropMode sourceMode = getVanillaDropMode(sourceMobKey);

        List<DropRule> copiedDrops = new ArrayList<>();
        for (DropRule rule : sourceDrops) {
            DropRule copy = new DropRule(
                    UUID.randomUUID().toString().substring(0, 8),
                    rule.getChance(),
                    rule.getMinAmount(),
                    rule.getMaxAmount(),
                    rule.isEnabled(),
                    rule.getItemData()
            );
            copiedDrops.add(copy);
        }

        dropsMap.put(targetMobKey.toLowerCase(), copiedDrops);
        dropModeMap.put(targetMobKey.toLowerCase(), sourceMode);
        save();
    }

    @Override
    public void resetConfig(String mobKey) {
        dropsMap.remove(mobKey.toLowerCase());
        dropModeMap.remove(mobKey.toLowerCase());
        save();
    }
}
