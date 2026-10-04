package com.mobdropeditor.storage;

import com.mobdropeditor.drops.DropRule;
import com.mobdropeditor.item.ItemData;
import com.mobdropeditor.mob.VanillaDropMode;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SQLiteStorage implements Storage {
    private final File dbFile;
    private Connection connection;
    private final Map<String, List<DropRule>> cache = new ConcurrentHashMap<>();
    private final Map<String, VanillaDropMode> modeCache = new ConcurrentHashMap<>();

    public SQLiteStorage(File dataDirectory) {
        this.dbFile = new File(dataDirectory, "database.db");
    }

    @Override
    public void init() {
        try {
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS mob_settings (mob_key TEXT PRIMARY KEY, drop_mode TEXT)");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS mob_drops (" +
                        "id TEXT PRIMARY KEY, " +
                        "mob_key TEXT, " +
                        "chance REAL, " +
                        "min_amount INT, " +
                        "max_amount INT, " +
                        "enabled INT, " +
                        "material TEXT, " +
                        "display_name TEXT, " +
                        "lore_data TEXT, " +
                        "enchantments_data TEXT, " +
                        "custom_model_data INT, " +
                        "unbreakable INT, " +
                        "raw_nbt TEXT)");
            }
            loadAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAll() {
        cache.clear();
        modeCache.clear();
        try {
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM mob_settings");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String mobKey = rs.getString("mob_key");
                    String mode = rs.getString("drop_mode");
                    try {
                        modeCache.put(mobKey, VanillaDropMode.valueOf(mode));
                    } catch (Exception e) {
                        modeCache.put(mobKey, VanillaDropMode.VANILLA_AND_CUSTOM);
                    }
                }
            }

            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM mob_drops");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String mobKey = rs.getString("mob_key");
                    double chance = rs.getDouble("chance");
                    int min = rs.getInt("min_amount");
                    int max = rs.getInt("max_amount");
                    boolean enabled = rs.getInt("enabled") == 1;

                    ItemData itemData = new ItemData();
                    itemData.setMaterial(rs.getString("material"));
                    itemData.setDisplayName(rs.getString("display_name"));

                    String loreStr = rs.getString("lore_data");
                    if (loreStr != null && !loreStr.isEmpty()) {
                        itemData.setLore(Arrays.asList(loreStr.split("\n")));
                    }

                    String enchantsStr = rs.getString("enchantments_data");
                    if (enchantsStr != null && !enchantsStr.isEmpty()) {
                        Map<String, Integer> map = new HashMap<>();
                        for (String pair : enchantsStr.split(";")) {
                            String[] kv = pair.split("=");
                            if (kv.length == 2) {
                                try {
                                    map.put(kv[0], Integer.parseInt(kv[1]));
                                } catch (Exception ignored) {}
                            }
                        }
                        itemData.setEnchantments(map);
                    }

                    int cmd = rs.getInt("custom_model_data");
                    if (!rs.wasNull()) {
                        itemData.setCustomModelData(cmd);
                    }

                    int unbr = rs.getInt("unbreakable");
                    if (!rs.wasNull()) {
                        itemData.setUnbreakable(unbr == 1);
                    }

                    itemData.setRawNbtBase64(rs.getString("raw_nbt"));

                    DropRule rule = new DropRule(id, chance, min, max, enabled, itemData);
                    cache.computeIfAbsent(mobKey, k -> new ArrayList<>()).add(rule);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<DropRule> getDrops(String mobKey) {
        return cache.getOrDefault(mobKey.toLowerCase(), new ArrayList<>());
    }

    @Override
    public synchronized void saveDrops(String mobKey, List<DropRule> drops) {
        String key = mobKey.toLowerCase();
        cache.put(key, new ArrayList<>(drops));

        try {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM mob_drops WHERE mob_key = ?")) {
                ps.setString(1, key);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO mob_drops (id, mob_key, chance, min_amount, max_amount, enabled, material, display_name, lore_data, enchantments_data, custom_model_data, unbreakable, raw_nbt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                for (DropRule rule : drops) {
                    ps.setString(1, rule.getId());
                    ps.setString(2, key);
                    ps.setDouble(3, rule.getChance());
                    ps.setInt(4, rule.getMinAmount());
                    ps.setInt(5, rule.getMaxAmount());
                    ps.setInt(6, rule.isEnabled() ? 1 : 0);

                    ItemData item = rule.getItemData();
                    ps.setString(7, item != null ? item.getMaterial() : "STONE");
                    ps.setString(8, item != null ? item.getDisplayName() : null);

                    if (item != null && item.getLore() != null && !item.getLore().isEmpty()) {
                        ps.setString(9, String.join("\n", item.getLore()));
                    } else {
                        ps.setNull(9, Types.VARCHAR);
                    }

                    if (item != null && item.getEnchantments() != null && !item.getEnchantments().isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (Map.Entry<String, Integer> entry : item.getEnchantments().entrySet()) {
                            if (sb.length() > 0) sb.append(";");
                            sb.append(entry.getKey()).append("=").append(entry.getValue());
                        }
                        ps.setString(10, sb.toString());
                    } else {
                        ps.setNull(10, Types.VARCHAR);
                    }

                    if (item != null && item.getCustomModelData() != null) {
                        ps.setInt(11, item.getCustomModelData());
                    } else {
                        ps.setNull(11, Types.INTEGER);
                    }

                    if (item != null && item.isUnbreakable() != null) {
                        ps.setInt(12, item.isUnbreakable() ? 1 : 0);
                    } else {
                        ps.setNull(12, Types.INTEGER);
                    }

                    ps.setString(13, item != null ? item.getRawNbtBase64() : null);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public VanillaDropMode getVanillaDropMode(String mobKey) {
        return modeCache.getOrDefault(mobKey.toLowerCase(), VanillaDropMode.VANILLA_AND_CUSTOM);
    }

    @Override
    public synchronized void setVanillaDropMode(String mobKey, VanillaDropMode mode) {
        String key = mobKey.toLowerCase();
        modeCache.put(key, mode);

        try (PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO mob_settings (mob_key, drop_mode) VALUES (?, ?)")) {
            ps.setString(1, key);
            ps.setString(2, mode.name());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, List<DropRule>> getAllDrops() {
        return Collections.unmodifiableMap(cache);
    }

    @Override
    public Map<String, VanillaDropMode> getAllVanillaDropModes() {
        return Collections.unmodifiableMap(modeCache);
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

        saveDrops(targetMobKey, copiedDrops);
        setVanillaDropMode(targetMobKey, sourceMode);
    }

    @Override
    public synchronized void resetConfig(String mobKey) {
        String key = mobKey.toLowerCase();
        cache.remove(key);
        modeCache.remove(key);

        try {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM mob_drops WHERE mob_key = ?")) {
                ps.setString(1, key);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM mob_settings WHERE mob_key = ?")) {
                ps.setString(1, key);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
