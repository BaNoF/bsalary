package ru.mine.banoff.utils;

import ru.mine.banoff.BSalary;

public class ConfigUtil {

    private static BSalary plugin;

    public static void init(BSalary pluginInstance) {
        plugin = pluginInstance;
    }

    public static String getString(String path) {
        String str = plugin.getConfig().getString(path);
        return ColorUtil.colorize(str != null ? str : "");
    }

    public static int getInt(String path) {
        return plugin.getConfig().getInt(path);
    }

    public static Boolean getBoolean(String path) {
        return plugin.getConfig().getBoolean(path);
    }
}
