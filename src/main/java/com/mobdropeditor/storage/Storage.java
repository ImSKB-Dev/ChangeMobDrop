package com.mobdropeditor.storage;

import com.mobdropeditor.drops.DropRule;
import com.mobdropeditor.mob.VanillaDropMode;

import java.util.List;
import java.util.Map;

public interface Storage {
    void init();
    void close();

    List<DropRule> getDrops(String mobKey);
    void saveDrops(String mobKey, List<DropRule> drops);

    VanillaDropMode getVanillaDropMode(String mobKey);
    void setVanillaDropMode(String mobKey, VanillaDropMode mode);

    Map<String, List<DropRule>> getAllDrops();
    Map<String, VanillaDropMode> getAllVanillaDropModes();

    void copyConfig(String sourceMobKey, String targetMobKey);
    void resetConfig(String mobKey);
}
