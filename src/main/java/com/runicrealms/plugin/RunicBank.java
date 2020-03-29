package com.runicrealms.plugin;

import com.runicrealms.plugin.bank.BankManager;
//import com.runicrealms.plugin.command.BankCMD;
import com.runicrealms.plugin.event.ClickEvent;
import com.runicrealms.plugin.event.LogoutEvent;
import com.runicrealms.plugin.listener.BankNPCListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;

public final class RunicBank extends JavaPlugin {

    private static RunicBank plugin;
    private static BankManager bankManager;
    private static HashSet<Integer> bankNPCs;

    private static HashSet<Integer> alchemistNPCs;
    private static HashSet<Integer> blacksmithNPCs;
    private static HashSet<Integer> enchanterNPCs;
    private static HashSet<Integer> hunterNPCs;
    private static HashSet<Integer> bakerNPCs;

    private static HashSet<Integer> forgeNPCs;
    private static HashSet<Integer> jewelerNPCs;
    private static HashSet<Integer> scrapperNPCs;

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

        // register bank command
        //getCommand("runicbank").setExecutor(new BankCMD());

        // register events
        getServer().getPluginManager().registerEvents(new ClickEvent(), this);
        getServer().getPluginManager().registerEvents(new LogoutEvent(), this);
        getServer().getPluginManager().registerEvents(new BankNPCListener(), this);

        // initialize NPCs
        initializeNPCs();
    }

    // todo: move to config
    private void initializeNPCs() {
        bankNPCs = new HashSet<>();
        bankNPCs.add(512);
        bankNPCs.add(258);
        bankNPCs.add(259);
        bankNPCs.add(515);
        bankNPCs.add(516);
        bankNPCs.add(212);
        bankNPCs.add(211);
        bankNPCs.add(213);
        bankNPCs.add(214);
        bankNPCs.add(242);
        bankNPCs.add(303);
        bankNPCs.add(448);
        bankNPCs.add(450);
        bankNPCs.add(351);
        bankNPCs.add(452);
        bankNPCs.add(347);
        bankNPCs.add(350);
        bankNPCs.add(518);
        bankNPCs.add(519);
        bankNPCs.add(520);
        bankNPCs.add(563);
    }

    private void initializeHunterNPCs() {
        hunterNPCs = new HashSet<>();
        //hunterNPCs.add();
    }

    public static HashSet<Integer> getBankNPCs() {
        return bankNPCs;
    }

    public static HashSet<Integer> getAlchemistNPCs() {
        return alchemistNPCs;
    }

    public static HashSet<Integer> getBlacksmithNPCs() {
        return blacksmithNPCs;
    }

    public static HashSet<Integer> getEnchanterNPCs() {
        return enchanterNPCs;
    }

    public static HashSet<Integer> getHunterNPCs() {
        return hunterNPCs;
    }

    public static HashSet<Integer> getBakerNPCs() {
        return bakerNPCs;
    }

    public static HashSet<Integer> getForgeNPCs() {
        return forgeNPCs;
    }

    public static HashSet<Integer> getJewelerNPCs() {
        return jewelerNPCs;
    }

    public static HashSet<Integer> getScrapperNPCs() {
        return scrapperNPCs;
    }

    @Override
    public void onDisable() {
        plugin = null;
        bankManager = null;
        bankNPCs = null;
    }
}
