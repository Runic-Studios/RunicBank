package com.runicrealms.plugin;

import com.runicrealms.plugin.listener.BankClickListener;
import com.runicrealms.plugin.listener.BankNPCListener;
import com.runicrealms.plugin.listener.PlayerJoinListener;
import com.runicrealms.runicrestart.api.RunicRestartApi;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;

public final class RunicBank extends JavaPlugin implements Listener {

    private static RunicBank plugin;
    private static BankManager bankManager;
    private static HashSet<Integer> bankNPCs;

    public static RunicBank getInstance() {
        return plugin;
    }

    public static BankManager getBankManager() {
        return bankManager;
    }

    @Override
    public void onEnable() {

        plugin = this;
        bankManager = new BankManager();
        getLogger().info("§aRunic§6Bank §ahas been enabled.");

        // load config defaults
        getConfig().options().copyDefaults(true);
        saveConfig();

        // register events
        getServer().getPluginManager().registerEvents(new BankClickListener(), this);
        getServer().getPluginManager().registerEvents(new BankNPCListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        getServer().getPluginManager().registerEvents(bankManager, this);
        getServer().getPluginManager().registerEvents(this, this);

        // initialize NPCs
        initializeBankNPCs();

        RunicRestartApi.markPluginLoaded("bank");
    }

    // todo: move to config
    private void initializeBankNPCs() {
        bankNPCs = new HashSet<>();
        bankNPCs.add(428); // azana
        bankNPCs.add(429); // koldore
        bankNPCs.add(430); // koldore
        bankNPCs.add(431); // whaletown
        bankNPCs.add(432); // hilstead
        bankNPCs.add(481); // vale
        bankNPCs.add(482); // vale
        bankNPCs.add(483); // rest
        bankNPCs.add(484); // isfodar
        bankNPCs.add(485); // zenyth
        bankNPCs.add(486); // zenyth
        bankNPCs.add(487); // naheen
        bankNPCs.add(488); // nazmora
        bankNPCs.add(489); // nazmora
        bankNPCs.add(490); // nazmora
        bankNPCs.add(498); // frost
        bankNPCs.add(499); // frost
        bankNPCs.add(500); // frost
    }

    public static HashSet<Integer> getBankNPCs() {
        return bankNPCs;
    }

    @Override
    public void onDisable() {
        plugin = null;
        bankManager = null;
        bankNPCs = null;
    }
}
