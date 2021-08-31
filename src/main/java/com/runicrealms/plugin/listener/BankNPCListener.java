package com.runicrealms.plugin.listener;

import com.runicrealms.plugin.RunicBank;
import com.runicrealms.plugin.util.DataUtil;
import com.runicrealms.runicnpcs.api.NpcClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.metadata.FixedMetadataValue;

public class BankNPCListener implements Listener {

    private static final int NPC_CLICK_DELAY = 20;

    @EventHandler
    public void onRightClick(NpcClickEvent event) {

        /*
        Prevent players from SPAMMING NPCs
         */
        Player pl = event.getPlayer();
        if (pl.hasMetadata("INVENTORY_META")) {
//            event.setCancelled(true);
            return;
        }
        pl.setMetadata("INVENTORY_META", new FixedMetadataValue(RunicBank.getInstance(), null));
        Bukkit.getScheduler().runTaskLaterAsynchronously(RunicBank.getInstance(),
                () -> pl.removeMetadata("INVENTORY_META", RunicBank.getInstance()), NPC_CLICK_DELAY); // After 20 ticks/1 second, remove the metadata key

        /*
        Bank-specific stuff
         */
        if (RunicBank.getBankNPCs().contains(event.getNpc().getId())) {
            if (RunicBank.getBankManager().getStorages().get(pl.getUniqueId()) != null
                    && RunicBank.getBankManager().getStorages().get(pl.getUniqueId()).isOpened()) {
                pl.playSound(pl.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f);
                pl.sendMessage(ChatColor.RED + "The bank is already open!");
                return;
            }
            if (!RunicBank.getBankManager().getStorages().containsKey(pl.getUniqueId())) {
                DataUtil.createBankOrLoad(pl.getUniqueId());
            }
            RunicBank.getBankManager().openBank(pl.getUniqueId());
        }
    }
}