package com.mobdropeditor.mob;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MobRegistry {
    private final Map<String, MobDefinition> registeredMobs = new ConcurrentHashMap<>();

    public MobRegistry() {
        registerVanillaMobs();
    }

    public void registerVanillaMobs() {
        for (EntityType type : EntityType.values()) {
            boolean isAlive = false;
            try {
                isAlive = type.isAlive();
            } catch (Throwable t) {
                // Fallback for 1.12.2 Bukkit where isAlive() method does not exist
                Class<?> entityClass = type.getEntityClass();
                if (entityClass != null && LivingEntity.class.isAssignableFrom(entityClass)) {
                    isAlive = true;
                }
            }

            if (isAlive && type != EntityType.PLAYER) {
                String key;
                try {
                    key = type.getKey().toString().toLowerCase();
                } catch (Throwable t) {
                    // Fallback for 1.12.2 Spigot
                    key = "minecraft:" + type.name().toLowerCase();
                }

                String name = formatName(type.name());
                registerMob(new MobDefinition(key, name, false));
            }
        }
    }

    public void registerMob(MobDefinition mobDefinition) {
        registeredMobs.put(mobDefinition.getKey().toLowerCase(), mobDefinition);
    }

    public MobDefinition registerMobIfAbsent(String key, String displayName) {
        if (key == null) return null;
        String searchKey = key.toLowerCase();
        if (!searchKey.contains(":")) {
            searchKey = "minecraft:" + searchKey;
        }

        final String finalKey = searchKey;
        return registeredMobs.computeIfAbsent(finalKey, k -> {
            String name = displayName != null ? displayName : formatName(k.contains(":") ? k.split(":")[1] : k);
            return new MobDefinition(k, name, !k.startsWith("minecraft:"));
        });
    }

    public Optional<MobDefinition> getMob(String key) {
        if (key == null) return Optional.empty();
        String searchKey = key.toLowerCase();
        if (!searchKey.contains(":")) {
            searchKey = "minecraft:" + searchKey;
        }

        MobDefinition mob = registeredMobs.get(searchKey);
        if (mob == null) {
            // Dynamically register mod mob if queried
            mob = registerMobIfAbsent(searchKey, null);
        }
        return Optional.ofNullable(mob);
    }

    public Collection<MobDefinition> getAllMobs() {
        return Collections.unmodifiableCollection(registeredMobs.values());
    }

    private String formatName(String name) {
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)))
              .append(part.substring(1).toLowerCase())
              .append(" ");
        }
        return sb.toString().trim();
    }
}
