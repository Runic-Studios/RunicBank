package com.runicrealms.plugin.listener;

import com.runicrealms.plugin.RunicBank;
import com.runicrealms.plugin.api.NpcClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BankNPCListener implements Listener {
    public static final Set<UUID> databaseRequesters = new HashSet<>(); // Players making requests to Redis/Mongo
    private static final int NPC_CLICK_DELAY = 1; // seconds

    @EventHandler
    public void onRightClick(NpcClickEvent event) {
        if (databaseRequesters.contains(event.getPlayer().getUniqueId())) return;
        // Prevent players from spamming NPCs
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (player.hasMetadata("INVENTORY_META")) return;
        player.setMetadata("INVENTORY_META", new FixedMetadataValue(RunicBank.getInstance(), null));

        // After 3s, remove the metadata key
        Bukkit.getScheduler().runTaskLaterAsynchronously(RunicBank.getInstance(),
                () -> player.removeMetadata("INVENTORY_META", RunicBank.getInstance()), NPC_CLICK_DELAY * 20L);

        // Ensure bank npc
        if (!RunicBank.getBankNPCs().contains(event.getNpc().getId())) return;

        // Fix for players trying to spoof whether bank inventory is open
        if (RunicBank.getAPI().isViewingBank(player.getUniqueId())) {
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f);
            player.sendMessage(ChatColor.RED + "The bank is already open!");
            return;
        }

        RunicBank.getAPI().openBank(uuid);
    }
}