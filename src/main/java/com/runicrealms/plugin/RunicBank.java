package com.runicrealms.plugin;

import com.runicrealms.plugin.event.ClickEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class RunicBank extends JavaPlugin {

    private static RunicBank plugin;

    public static RunicBank getInstance() { return plugin; }

    @Override
    public void onEnable() {

        plugin = this;
        getLogger().info("§aRunic§6Bank §ahas been enabled.");

        // load config defaults
        getConfig().options().copyDefaults(true);
        saveConfig();

        // register bank command
        getCommand("runicbank").setExecutor(new BankCMD());

        // register events
        getServer().getPluginManager().registerEvents(new ClickEvent(), this);
    }

    @Override
    public void onDisable() {
        plugin = null;
    }
}
