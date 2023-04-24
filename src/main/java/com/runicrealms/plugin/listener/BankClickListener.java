package com.runicrealms.plugin.listener;

import com.runicrealms.plugin.model.BankHolder;
import com.runicrealms.runicitems.RunicItemsAPI;
import com.runicrealms.runicitems.item.RunicItem;
import com.runicrealms.runicitems.item.stats.RunicItemTag;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.UUID;

public class BankClickListener implements Listener {

    @EventHandler
    public void clickEvent(InventoryClickEvent event) {
        // Verify that a player clicked
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();
        if (event.getClickedInventory() == null) return;

        // Verify that we're looking at a bank inventory
        if (!(event.getView().getTopInventory().getHolder() instanceof BankHolder)) return;
        BankHolder inventoryHolder = (BankHolder) event.getInventory().getHolder();
        if (inventoryHolder == null) return;
        if (!uuid.equals(inventoryHolder.getUuid())) return;

        // Handle blocked items
        if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
            RunicItem runicItem = RunicItemsAPI.getRunicItemFromItemStack(event.getCurrentItem());
            if (runicItem != null &&
                    (runicItem.getTags().contains(RunicItemTag.SOULBOUND) || runicItem.getTags().contains(RunicItemTag.QUEST_ITEM))) {
                event.setCancelled(true);
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5f, 1.0f);
                player.sendMessage(ChatColor.RED + "This item cannot be stored in the bank!");
            }
        }

        // Call the bank holder
        if (event.getClickedInventory() == event.getView().getTopInventory()) { // Verify they clicked the top inventory
            if (event.getSlot() < 9) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                event.setCancelled(true);
                switch (event.getSlot()) {
                    case 6:
                        inventoryHolder.addPage(player.getUniqueId(), event.getCurrentItem().getType());
                        break;
                    case 7:
                        inventoryHolder.prevPage(player.getUniqueId());
                        break;
                    case 8:
                        inventoryHolder.nextPage(player.getUniqueId());
                        break;
                }
            }
        }
    }

    /**
     * Write bank info to jedis on inventory close
     */
    @EventHandler
    public void onBankClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!(event.getView().getTopInventory().getHolder() instanceof BankHolder)) return;
        BankHolder bankHolder = (BankHolder) event.getInventory().getHolder();
        if (bankHolder == null) return;
        if (!uuid.equals(bankHolder.getUuid())) return;
        bankHolder.setOpen(false);
        bankHolder.savePage(); // Updates the current page in from the ui in memory
    }

}
