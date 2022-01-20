package com.runicrealms.plugin;

import com.runicrealms.plugin.bank.BankManager;
import com.runicrealms.plugin.listener.BankClickListener;
import com.runicrealms.plugin.listener.BankNPCListener;
import com.runicrealms.plugin.listener.PlayerJoinListener;
import com.runicrealms.runicrestart.api.RunicRestartApi;
import com.runicrealms.runicrestart.api.ServerShutdownEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;

// todo: to optimize, don't save bank on-close every time. Enable a queue system like core
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

    @EventHandler
    public void onRunicShutdown(ServerShutdownEvent e) {
        /*
        Save current state of player data
         */
        getLogger().info(" §cRunicBank has been disabled.");
        // Used to be bank saving here, now handled by cache save event.
        /*
        Notify RunicRestart
         */
        RunicRestartApi.markPluginSaved("bank");
    }

    @Override
    public void onDisable() {
        plugin = null;
        bankManager = null;
        bankNPCs = null;
    }
}
