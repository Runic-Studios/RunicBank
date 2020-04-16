package com.runicrealms.plugin.listener;

import com.runicrealms.plugin.RunicBank;
import com.runicrealms.plugin.util.DataUtil;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.metadata.FixedMetadataValue;

public class BankNPCListener implements Listener {

    private static final int NPC_CLICK_DELAY = 20;
    @EventHandler
    public void onRightClick(NPCRightClickEvent event) {

        /*
        Prevent players from SPAMMING NPCs
         */
        Player pl = event.getClicker();
        if (pl.hasMetadata("INVENTORY_META")) {
            event.setCancelled(true);
            return;
        }
        pl.setMetadata("INVENTORY_META", new FixedMetadataValue(RunicBank.getInstance(), null));

        Bukkit.getScheduler().runTaskLaterAsynchronously(RunicBank.getInstance(),
                () -> pl.removeMetadata("INVENTORY_META", RunicBank.getInstance()), NPC_CLICK_DELAY); // After 20 ticks/1 second, remove the metadata key

        /*
        Artifact Forge
         */
        if (RunicBank.getForgeNPCs().contains(event.getNPC().getId())) {
            Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "openforge " + pl.getName());
            return;
        }

        /*
        Jewel masters
         */
        if (RunicBank.getJewelerNPCs().contains(event.getNPC().getId())) {
            Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "jewelmaster " + pl.getName());
            return;
        }

        /*
        Scrappers
         */
        if (RunicBank.getScrapperNPCs().contains(event.getNPC().getId())) {
            Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "currency scrapper " + pl.getName());
            return;
        }

        /*
        Bank-specific stuff
         */
        if (RunicBank.getBankNPCs().contains(event.getNPC().getId())) {
            if (!RunicBank.getBankManager().getStorages().containsKey(pl.getUniqueId())) {
                DataUtil.createBankOrLoad(pl.getUniqueId());
            }
            RunicBank.getBankManager().openBank(pl.getUniqueId());
        }
    }
}