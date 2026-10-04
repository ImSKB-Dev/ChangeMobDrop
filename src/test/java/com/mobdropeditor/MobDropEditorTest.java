package com.mobdropeditor;

import com.mobdropeditor.drops.DropCondition;
import com.mobdropeditor.drops.DropEngine;
import com.mobdropeditor.drops.DropResult;
import com.mobdropeditor.drops.DropRule;
import com.mobdropeditor.drops.conditions.MinimumLootingCondition;
import com.mobdropeditor.drops.conditions.PlayerKillerCondition;
import com.mobdropeditor.item.ItemData;
import com.mobdropeditor.item.ItemSerializer;
import com.mobdropeditor.mob.MobDefinition;
import com.mobdropeditor.mob.MobRegistry;
import com.mobdropeditor.mob.VanillaDropMode;
import com.mobdropeditor.storage.SQLiteStorage;
import com.mobdropeditor.storage.Storage;
import com.mobdropeditor.storage.YamlStorage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class MobDropEditorTest {

    @TempDir
    Path tempDir;

    private Storage yamlStorage;
    private Storage sqliteStorage;

    @BeforeAll
    static void initBukkit() {
        if (Bukkit.getServer() == null) {
            Server server = Mockito.mock(Server.class, Mockito.RETURNS_DEEP_STUBS);
            Bukkit.setServer(server);
        }
    }

    @BeforeEach
    void setUp() {
        yamlStorage = new YamlStorage(tempDir.toFile());
        yamlStorage.init();

        sqliteStorage = new SQLiteStorage(tempDir.toFile());
        sqliteStorage.init();
    }

    @AfterEach
    void tearDown() {
        if (yamlStorage != null) yamlStorage.close();
        if (sqliteStorage != null) sqliteStorage.close();
    }

    @Test
    @DisplayName("1. Chance Calculation & Decimal Precision Test")
    void testChanceCalculation() {
        DropRule ruleHigh = new DropRule("rule1", 100.0, 1, 1, true, new ItemData("DIAMOND", 1));
        DropRule ruleZero = new DropRule("rule2", 0.0, 1, 1, true, new ItemData("DIRT", 1));
        DropRule ruleDecimal = new DropRule("rule3", 0.01, 1, 1, true, new ItemData("EMERALD", 1));

        assertEquals(100.0, ruleHigh.getChance());
        assertEquals(0.0, ruleZero.getChance());
        assertEquals(0.01, ruleDecimal.getChance());
    }

    @Test
    @DisplayName("2. Min/Max Amount Calculation Test")
    void testMinMaxAmounts() {
        DropRule rule = new DropRule("rule", 100.0, 3, 5, true, new ItemData("IRON_INGOT", 1));
        assertEquals(3, rule.getMinAmount());
        assertEquals(5, rule.getMaxAmount());

        DropEngine engine = new DropEngine(yamlStorage);
        yamlStorage.saveDrops("minecraft:zombie", Collections.singletonList(rule));

        DropResult result = engine.evaluateDrops("minecraft:zombie", null, null, 0);
        assertNotNull(result);
        assertEquals(1, result.getCustomDrops().size());
        ItemStack drop = result.getCustomDrops().get(0);
        assertTrue(drop.getAmount() >= 3 && drop.getAmount() <= 5);
    }

    @Test
    @DisplayName("3 & 4. ItemStack Serialization and Deserialization")
    void testItemSerialization() {
        ItemData data = new ItemData("DIAMOND_SWORD", 2);
        data.setDisplayName("§6Excalibur");
        data.setLore(Arrays.asList("§7Legendary Sword", "§7Second Line"));
        Map<String, Integer> enchants = new HashMap<>();
        enchants.put("DAMAGE_ALL", 5);
        data.setEnchantments(enchants);
        data.setCustomModelData(1001);

        ItemStack item = ItemSerializer.deserialize(data);
        assertNotNull(item);
        assertEquals(Material.DIAMOND_SWORD, item.getType());
        assertEquals(2, item.getAmount());

        ItemData reSerialized = ItemSerializer.serialize(item);
        assertEquals("DIAMOND_SWORD", reSerialized.getMaterial());
        assertEquals(2, reSerialized.getAmount());
    }

    @Test
    @DisplayName("5. Custom Item Meta & Base64 Fallback Test")
    void testCustomItemMeta() {
        ItemData data = new ItemData("GOLDEN_APPLE", 1);
        data.setRawNbtBase64("invalid_base64_fallback");

        ItemStack reconstructed = ItemSerializer.deserialize(data);
        assertNotNull(reconstructed);
        assertEquals(Material.GOLDEN_APPLE, reconstructed.getType());
    }

    @Test
    @DisplayName("6. Mob Configuration & Storage Persistence (Yaml & SQLite)")
    void testMobConfigurationStorage() {
        ItemData itemData = new ItemData("GOLD_INGOT", 4);
        DropRule rule = new DropRule("gold_drop", 25.5, 2, 4, true, itemData);

        // Test YAML
        yamlStorage.saveDrops("minecraft:zombie", Collections.singletonList(rule));
        yamlStorage.setVanillaDropMode("minecraft:zombie", VanillaDropMode.CUSTOM_ONLY);

        List<DropRule> yamlDrops = yamlStorage.getDrops("minecraft:zombie");
        assertEquals(1, yamlDrops.size());
        assertEquals(25.5, yamlDrops.get(0).getChance());
        assertEquals(VanillaDropMode.CUSTOM_ONLY, yamlStorage.getVanillaDropMode("minecraft:zombie"));

        // Test SQLite
        sqliteStorage.saveDrops("minecraft:skeleton", Collections.singletonList(rule));
        sqliteStorage.setVanillaDropMode("minecraft:skeleton", VanillaDropMode.VANILLA_ONLY);

        List<DropRule> sqliteDrops = sqliteStorage.getDrops("minecraft:skeleton");
        assertEquals(1, sqliteDrops.size());
        assertEquals(25.5, sqliteDrops.get(0).getChance());
        assertEquals(VanillaDropMode.VANILLA_ONLY, sqliteStorage.getVanillaDropMode("minecraft:skeleton"));
    }

    @Test
    @DisplayName("7. Dynamic Mob Identification (namespace:id)")
    void testDynamicMobIdentification() {
        MobRegistry registry = new MobRegistry();
        registry.registerMob(new MobDefinition("custommod:boss_dragon", "Boss Dragon", true));

        Optional<MobDefinition> foundModMob = registry.getMob("custommod:boss_dragon");
        assertTrue(foundModMob.isPresent());
        assertEquals("Boss Dragon", foundModMob.get().getDisplayName());
        assertTrue(foundModMob.get().isCustomEntity());

        Optional<MobDefinition> vanillaMob = registry.getMob("zombie");
        assertTrue(vanillaMob.isPresent());
        assertEquals("minecraft:zombie", vanillaMob.get().getKey());
    }

    @Test
    @DisplayName("8 & 9. Vanilla Drop Modes and Custom Drop Engine Evaluation")
    void testDropModesAndEngine() {
        DropRule rule = new DropRule("rule1", 100.0, 1, 1, true, new ItemData("DIAMOND", 1));
        yamlStorage.saveDrops("minecraft:creeper", Collections.singletonList(rule));

        DropEngine engine = new DropEngine(yamlStorage);

        // VANILLA_AND_CUSTOM
        yamlStorage.setVanillaDropMode("minecraft:creeper", VanillaDropMode.VANILLA_AND_CUSTOM);
        DropResult res1 = engine.evaluateDrops("minecraft:creeper", null, null, 0);
        assertEquals(VanillaDropMode.VANILLA_AND_CUSTOM, res1.getVanillaDropMode());
        assertEquals(1, res1.getCustomDrops().size());

        // CUSTOM_ONLY
        yamlStorage.setVanillaDropMode("minecraft:creeper", VanillaDropMode.CUSTOM_ONLY);
        DropResult res2 = engine.evaluateDrops("minecraft:creeper", null, null, 0);
        assertEquals(VanillaDropMode.CUSTOM_ONLY, res2.getVanillaDropMode());
        assertEquals(1, res2.getCustomDrops().size());

        // VANILLA_ONLY
        yamlStorage.setVanillaDropMode("minecraft:creeper", VanillaDropMode.VANILLA_ONLY);
        DropResult res3 = engine.evaluateDrops("minecraft:creeper", null, null, 0);
        assertEquals(VanillaDropMode.VANILLA_ONLY, res3.getVanillaDropMode());
        assertEquals(0, res3.getCustomDrops().size());
    }

    @Test
    @DisplayName("10. Drop Condition Evaluation Test")
    void testDropConditions() {
        DropRule rule = new DropRule("rule_cond", 100.0, 1, 1, true, new ItemData("NETHERITE_INGOT", 1));
        rule.setConditions(Arrays.asList(
                new PlayerKillerCondition(true),
                new MinimumLootingCondition(2)
        ));

        yamlStorage.saveDrops("minecraft:wither_skeleton", Collections.singletonList(rule));
        DropEngine engine = new DropEngine(yamlStorage);

        // Fails when killer is null
        DropResult resNoKiller = engine.evaluateDrops("minecraft:wither_skeleton", null, null, 2);
        assertEquals(0, resNoKiller.getCustomDrops().size());

        DropCondition condPlayer = new PlayerKillerCondition(true);
        DropCondition condLooting = new MinimumLootingCondition(2);

        assertFalse(condPlayer.evaluate(null, null, 2));
        assertFalse(condLooting.evaluate(null, null, 1));
        assertTrue(condLooting.evaluate(null, null, 2));
    }

    @Test
    @DisplayName("11. Configuration Copy and Reset Test")
    void testCopyAndResetConfig() {
        DropRule rule = new DropRule("rule_copy", 50.0, 1, 2, true, new ItemData("FEATHER", 1));
        yamlStorage.saveDrops("minecraft:chicken", Collections.singletonList(rule));
        yamlStorage.setVanillaDropMode("minecraft:chicken", VanillaDropMode.CUSTOM_ONLY);

        yamlStorage.copyConfig("minecraft:chicken", "minecraft:duck");

        List<DropRule> copied = yamlStorage.getDrops("minecraft:duck");
        assertEquals(1, copied.size());
        assertEquals(50.0, copied.get(0).getChance());
        assertEquals(VanillaDropMode.CUSTOM_ONLY, yamlStorage.getVanillaDropMode("minecraft:duck"));

        yamlStorage.resetConfig("minecraft:duck");
        assertTrue(yamlStorage.getDrops("minecraft:duck").isEmpty());
    }

    @Test
    @DisplayName("12. Concurrency and Thread Safety Test")
    void testConcurrencyAndThreadSafety() throws Exception {
        int threads = 10;
        int operationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String mobKey = "minecraft:mob_" + (threadId % 3);
                        DropRule rule = new DropRule("rule_" + j, 10.0 + j, 1, 2, true, new ItemData("STONE", 1));
                        yamlStorage.saveDrops(mobKey, Collections.singletonList(rule));
                        sqliteStorage.saveDrops(mobKey, Collections.singletonList(rule));

                        yamlStorage.getDrops(mobKey);
                        sqliteStorage.getDrops(mobKey);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(finished, "Concurrent storage operations completed safely.");
    }
}
