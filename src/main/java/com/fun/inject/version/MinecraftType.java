package com.fun.inject.version;

public enum MinecraftType {
    VANILLA("vanilla"),
    FORGE("forge"),
    MCP("mcp"),
    FABRIC("fabric"),
    NONE("none");

    private final String type;

    MinecraftType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

}
