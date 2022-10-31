package com.runicrealms.plugin.listener;

import com.runicrealms.plugin.RunicBank;
import com.runicrealms.plugin.model.PlayerBankData;
import com.runicrealms.runicitems.RunicItemsAPI;
import com.runicrealms.runicitems.item.RunicItem;
import com.runicrealms.runicitems.item.stats.RunicItemTag;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.PlayerInventory;

public class BankClickListener implements Listener {

    @EventHandler
    public void clickEvent(InventoryClickEvent e) {

        // verify that we're looking at a bank inv
        if (RunicBank.getBankManager().getBankDataMap().get(e.getWhoClicked().getUniqueId()) != null
                && e.getView().getTitle().equals
                (RunicBank.getBankManager().getBankDataMap().get(e.getWhoClicked().getUniqueId()).getBankTitle())) {

            if (!(e.getWhoClicked() instanceof Player)) return;
            Player player = (Player) e.getWhoClicked();

            // disable interactions on first 9 slots
            if (RunicBank.getBankManager().getBankDataMap() == null) {
                Bukkit.getLogger().info("bank manager storages in null");
                player.closeInventory();
                return;
            }

            // todo: this will never fire! fix the handler
            if (RunicBank.getBankManager().getBankDataMap().get(player.getUniqueId()) == null) {
                Bukkit.getLogger().info("Error: bank storage for " + player.getName() + " is null!");
                player.closeInventory();
                return;
            }

            // disabled interactions w/ blocked items
            if (e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR) {
                RunicItem runicItem = RunicItemsAPI.getRunicItemFromItemStack(e.getCurrentItem());
                if (runicItem != null &&
                        (runicItem.getTags().contains(RunicItemTag.SOULBOUND) || runicItem.getTags().contains(RunicItemTag.QUEST_ITEM))) {
                    e.setCancelled(true);
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5f, 1.0f);
                    player.sendMessage(ChatColor.RED + "This item cannot be stored in the bank!");
                }
            }

            String bankTitle = RunicBank.getBankManager().getBankDataMap().get(player.getUniqueId()).getBankTitle();
            if (e.getView().getTitle().equalsIgnoreCase(bankTitle)) {
                if (e.getClickedInventory() != null && !(e.getClickedInventory() instanceof PlayerInventory) && e.getView().getTitle().equals(bankTitle)) {
                    if (e.getSlot() < 9) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                        e.setCancelled(true);
                        PlayerBankData playerBankData = RunicBank.getBankManager().getBankDataMap().get(player.getUniqueId());
                        switch (e.getSlot()) {
                            case 6:
                                playerBankData.addPage(player.getUniqueId(), e.getCurrentItem().getType());
                                break;
                            case 7:
                                playerBankData.prevPage(player.getUniqueId());
                                break;
                            case 8:
                                playerBankData.nextPage(player.getUniqueId());
                                break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Write bank info to file.
     */
    @EventHandler
    public void onBankClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (RunicBank.getBankManager().getBankDataMap() == null) return;
        if (RunicBank.getBankManager().getBankDataMap().get(player.getUniqueId()) == null) return;
        if (RunicBank.getBankManager().getBankDataMap().get(player.getUniqueId()).getBankInv() == null) return;
        String bankTitle = RunicBank.getBankManager().getBankDataMap().get(player.getUniqueId()).getBankTitle();
        if (event.getView().getTitle().equals(bankTitle)) {
            PlayerBankData playerBankData = RunicBank.getBankManager().getBankDataMap().get(player.getUniqueId());
            playerBankData.savePage(); // to array
            playerBankData.setOpened(false);
        }
    }
}
