package com.fun.inject.version;

public enum MinecraftVersion {
    VER_1710("1.7.10", "1.7.10.jar"),
    VER_189("1.8.9", "1.8.9.jar", "虎牙", "欧迈嘎"),
    VER_1122("1.12.2", "1.12.2.jar", "花雨庭"),
    VER_1165("1.16.5", "1.16.5.jar"),
    VER_1181("1.18.1", "1.18.1.jar", "布吉岛"),
    VER_1201("1.20.1", "1.20.1.jar", "布吉岛");


    public String injection;
    private final String ver;
    private final String[] clientNames;

    MinecraftVersion(String ver, String injection, String... clientNames) {
        this.injection = injection;
        this.ver = ver;
        this.clientNames = clientNames;
    }

    public static MinecraftVersion getMinecraftVersion() {
        for (MinecraftVersion value : values()) {
            if (System.getProperty("java.library.path").contains(value.ver))
                return value;
            if (System.getProperty("sun.java.command")
                    .contains(" " + value.ver.substring(0, value.ver.lastIndexOf("."))))
                return value;
        }
        return MinecraftVersion.VER_189;
    }

    public String getVer() {
        return ver;
    }

    public String getGeneralVer() {
        String[] split = getVer().split("\\.");
        return split[0] + "." + split[1];
    }

    public String[] getClientNames() {
        return clientNames;
    }

    public String getInjection() {
        return "/injections/" + injection;
    }
}
