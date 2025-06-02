package ru.mine.banoff;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mine.banoff.utils.ConfigUtil;
import ru.mine.banoff.utils.DBUtil;

public final class BSalary extends JavaPlugin{

    private static Economy econ;
    private static DBUtil DBUtil;
    private static Permission perms;

    public static Permission getPermission() {
        return perms;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ConfigUtil.init(this);
        DBUtil = new DBUtil(this);
        DBUtil.initDB();
        setupPermissions();
        getCommand("salary").setExecutor(new Command(this, DBUtil));
        getServer().getPluginManager().registerEvents(DBUtil, this);
    }

}
