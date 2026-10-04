package com.mobdropeditor.mob;

import java.util.Objects;

public class MobDefinition {
    private final String key; // e.g., "minecraft:zombie" or "modid:custom_mob"
    private final String displayName;
    private final boolean customEntity;

    public MobDefinition(String key, String displayName, boolean customEntity) {
        this.key = key.toLowerCase();
        this.displayName = displayName != null ? displayName : key;
        this.customEntity = customEntity;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isCustomEntity() {
        return customEntity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MobDefinition that = (MobDefinition) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return key + " (" + displayName + ")";
    }
}
