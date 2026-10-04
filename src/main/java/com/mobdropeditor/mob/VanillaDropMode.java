package com.mobdropeditor.mob;

public enum VanillaDropMode {
    VANILLA_AND_CUSTOM("Vanilla + Custom"),
    CUSTOM_ONLY("Custom Only"),
    VANILLA_ONLY("Vanilla Only");

    private final String displayName;

    VanillaDropMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public VanillaDropMode next() {
        VanillaDropMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
