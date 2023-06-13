package com.runicrealms.plugin;

import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import com.runicrealms.plugin.api.BankWriteOperation;
import com.runicrealms.plugin.api.RunicBankAPI;
import com.runicrealms.plugin.listener.BankClickListener;
import com.runicrealms.plugin.listener.BankNPCListener;
import com.runicrealms.plugin.model.MongoTask;
import com.runicrealms.runicitems.RunicItemsAPI;
import com.runicrealms.runicrestart.RunicRestart;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;

public final class RunicBank extends JavaPlugin implements Listener {
    private static RunicBank plugin;
    private static TaskChainFactory taskChainFactory;
    private static RunicBankAPI runicBankAPI;
    private static HashSet<Integer> bankNPCs;
    private static MongoTask mongoTask;
    private static BankWriteOperation bankWriteOperation;

    public static RunicBank getInstance() {
        return plugin;
    }

    public static RunicBankAPI getAPI() {
        return runicBankAPI;
    }

    public static HashSet<Integer> getBankNPCs() {
        return bankNPCs;
    }

    public static MongoTask getMongoTask() {
        return mongoTask;
    }

    public static <T> TaskChain<T> newChain() {
        return taskChainFactory.newChain();
    }

    public static BankWriteOperation getBankWriteOperation() {
        return bankWriteOperation;
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
        bankNPCs.add(724); // stonehaven
    }

    @Override
    public void onDisable() {
        plugin = null;
        runicBankAPI = null;
        mongoTask = null;
        bankNPCs = null;
        taskChainFactory = null;
        bankWriteOperation = null;
    }

    @Override
    public void onEnable() {
        plugin = this;
        taskChainFactory = BukkitTaskChainFactory.create(this);
        BankManager bankManager = new BankManager();
        runicBankAPI = bankManager;
        bankWriteOperation = bankManager;
        mongoTask = new MongoTask();
        getLogger().info("§aRunic§6Bank §ahas been enabled.");

        // Load config defaults
        getConfig().options().copyDefaults(true);
        saveConfig();

        // Register events
        getServer().getPluginManager().registerEvents(new BankClickListener(), this);
        getServer().getPluginManager().registerEvents(new BankNPCListener(), this);
        getServer().getPluginManager().registerEvents(this, this);

        // Initialize NPCs
        initializeBankNPCs();

        // Mark plugin loading complete
        RunicRestart.getAPI().markPluginLoaded("bank");

        RunicItemsAPI.registerAntiDupeInventoryHandler(player -> RunicBank.getAPI().isViewingBank(player.getUniqueId()));
    }
}
