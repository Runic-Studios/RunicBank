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

import java.util.UUID;

public class BankNPCListener implements Listener {

    private static final int NPC_CLICK_DELAY = 20;

    @EventHandler
    public void onRightClick(NpcClickEvent event) {

        // Prevent players from spamming NPCs
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (player.hasMetadata("INVENTORY_META")) return;
        player.setMetadata("INVENTORY_META", new FixedMetadataValue(RunicBank.getInstance(), null));

        // After 20 ticks (1s), remove the metadata key
        Bukkit.getScheduler().runTaskLaterAsynchronously(RunicBank.getInstance(),
                () -> player.removeMetadata("INVENTORY_META", RunicBank.getInstance()), NPC_CLICK_DELAY);

        // Ensure bank npc
        if (!RunicBank.getBankNPCs().contains(event.getNpc().getId())) return;

        // Fix for players trying to spoof whether bank inv is open
        if (RunicBank.getBankManager().getBankDataMap().get(player.getUniqueId()) != null
                && RunicBank.getBankManager().getBankDataMap().get(player.getUniqueId()).isOpened()) {
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f);
            player.sendMessage(ChatColor.RED + "The bank is already open!");
            return;
        }

        // todo: split here into mongo and jedis
        /*
        Create bank in mongo
         */
        RunicBank.getBankManager().loadPlayerBankData(uuid);
        RunicBank.getBankManager().openBank(uuid);

//        try (Jedis jedis = RunicCoreAPI.getNewJedisResource()) {
//            Bukkit.getScheduler().runTaskAsynchronously(RunicBank.getInstance(), () -> {
//                if (!RunicBank.getBankManager().getStorages().containsKey(player.getUniqueId())) {
//                    DataUtil.createBankOrLoad(player.getUniqueId(), jedis);
//                }
//                Bukkit.getScheduler().runTask(RunicBank.getInstance(), () -> RunicBank.getBankManager().openBank(player.getUniqueId()));
//            });
//        }
    }
}