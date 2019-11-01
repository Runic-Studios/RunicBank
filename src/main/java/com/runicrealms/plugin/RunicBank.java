package com.runicrealms.plugin;

import com.runicrealms.plugin.bank.BankManager;
import com.runicrealms.plugin.command.BankCMD;
import com.runicrealms.plugin.event.ClickEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class RunicBank extends JavaPlugin {

    private static RunicBank plugin;
    private static BankManager bankManager;

    public static RunicBank getInstance() { return plugin; }
    public static BankManager getBankManager() { return bankManager; }

    @Override
    public void onEnable() {

        plugin = this;
        bankManager = new BankManager();
        getLogger().info("§aRunic§6Bank §ahas been enabled.");

        // load config defaults
        getConfig().options().copyDefaults(true);
        saveConfig();
        //

        // register bank command
        getCommand("runicbank").setExecutor(new BankCMD());

        // register events
        getServer().getPluginManager().registerEvents(new ClickEvent(), this);
    }

    @Override
    public void onDisable() {
        plugin = null;
        bankManager = null;
    }
}
