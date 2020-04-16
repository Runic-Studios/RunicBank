package com.runicrealms.plugin;

import com.runicrealms.plugin.bank.BankManager;
import com.runicrealms.plugin.listener.BankClickListener;
import com.runicrealms.plugin.listener.PlayerQuitListener;
import com.runicrealms.plugin.listener.BankNPCListener;
import com.runicrealms.plugin.listener.PlayerJoinListener;
import com.runicrealms.runicrestart.api.RunicRestartApi;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;

public final class RunicBank extends JavaPlugin {

    private static RunicBank plugin;
    private static BankManager bankManager;
    private static HashSet<Integer> bankNPCs;

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

        // register events
        getServer().getPluginManager().registerEvents(new BankClickListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
        getServer().getPluginManager().registerEvents(new BankNPCListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);

        // initialize NPCs
        initializeBankNPCs();
        initializeForgeNPCs();
        initializeJewelerNPCs();
        initializeScrapperNPCs();

        RunicRestartApi.markPluginLoaded("bank");
    }

    // todo: move to config
    private void initializeBankNPCs() {
        bankNPCs = new HashSet<>();
        bankNPCs.add(512); // azana
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

    // TODO HUNTERS

    private void initializeForgeNPCs() {
        forgeNPCs = new HashSet<>();
        forgeNPCs.add(514); // azana
        forgeNPCs.add(521);
        forgeNPCs.add(522);
        forgeNPCs.add(523);
        forgeNPCs.add(524);
        forgeNPCs.add(525);
        forgeNPCs.add(526);
        forgeNPCs.add(527);
        forgeNPCs.add(528);
    }

    private void initializeJewelerNPCs() {
        jewelerNPCs = new HashSet<>();
        jewelerNPCs.add(513);
        jewelerNPCs.add(551);
        jewelerNPCs.add(552);
        jewelerNPCs.add(553);
        jewelerNPCs.add(554);
        jewelerNPCs.add(555);
        jewelerNPCs.add(556);
        jewelerNPCs.add(557);
        jewelerNPCs.add(558);
        jewelerNPCs.add(559);
    }

    private void initializeScrapperNPCs() {
        scrapperNPCs = new HashSet<>();
        scrapperNPCs.add(235);
        scrapperNPCs.add(490);
        scrapperNPCs.add(492);
        scrapperNPCs.add(494);
        scrapperNPCs.add(495);
        scrapperNPCs.add(496);
        scrapperNPCs.add(497);
        scrapperNPCs.add(498);
        scrapperNPCs.add(499);
        scrapperNPCs.add(500);
        scrapperNPCs.add(501);
        scrapperNPCs.add(537);
        scrapperNPCs.add(548);
        scrapperNPCs.add(550);
    }

    public static HashSet<Integer> getBankNPCs() {
        return bankNPCs;
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
