package com.runicrealms.plugin.event;

import com.runicrealms.plugin.RunicBank;
import com.runicrealms.plugin.bank.BankStorage;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

@SuppressWarnings("deprecation")
public class ClickEvent implements Listener {

    @EventHandler
    public void clickEvent(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player pl = (Player) e.getWhoClicked();
        Inventory inv = e.getInventory();
        // disable interactions on first 9 slots
        Inventory bankInv = RunicBank.getBankManager().getStorages().get(pl.getUniqueId()).getBankInv();
        if (inv.getTitle().equals(bankInv.getTitle())) {
            if (e.getClickedInventory() != null
                    && e.getClickedInventory().getTitle().equals(bankInv.getTitle()) && e.getSlot() < 9) {
                pl.playSound(pl.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                e.setCancelled(true);
                BankStorage storage = RunicBank.getBankManager().getStorages().get(pl.getUniqueId());
                switch (e.getSlot()) {
                    case 6:
                        storage.addPage(pl.getUniqueId(), e.getCurrentItem().getType());
                        break;
                    case 7:
                        storage.prevPage(pl.getUniqueId());
                        break;
                    case 8:
                        storage.nextPage(pl.getUniqueId());
                        break;
                }
            }
        }
    }

    /**
     * Write bank info to file.
     */
    @EventHandler
    public void onBankClose(InventoryCloseEvent e) {
        Player pl = (Player) e.getPlayer();
        if (RunicBank.getBankManager().getStorages() == null) return;
        if (RunicBank.getBankManager().getStorages().get(pl.getUniqueId()) == null) return;
        if (RunicBank.getBankManager().getStorages().get(pl.getUniqueId()).getBankInv() == null) return;
        Inventory bankInv = RunicBank.getBankManager().getStorages().get(pl.getUniqueId()).getBankInv();
        Inventory inv = e.getInventory();
        if (inv.getTitle().equals(bankInv.getTitle())) {
            BankStorage storage = RunicBank.getBankManager().getStorages().get(pl.getUniqueId());
            storage.savePage(); // to array
            storage.saveContents(); // to flat file
        }
    }
}
