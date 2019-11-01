package com.runicrealms.plugin.event;

import com.runicrealms.plugin.RunicBank;
import com.runicrealms.plugin.bank.BankStorage;
import com.runicrealms.plugin.gui.BankGUI;
import com.runicrealms.plugin.professions.Workstation;
import com.runicrealms.plugin.util.FileUtil;
import com.runicrealms.plugin.util.Util;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

// TODO: store all pages in virtual memory on bank open, only save on bank close.
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
                        addPage(inv, e.getWhoClicked().getUniqueId(), inv.getItem(6).getType());
                        break;
                    case 7:
                        prevPage(inv, inv.getContents(), pl.getUniqueId());
                        break;
                    case 8:
                        storage.nextPage(pl.getUniqueId());
                        break;
                }
            }
        }
    }

    @EventHandler
    public void onBankClose(InventoryCloseEvent e) {
        if (e.getInventory().getTitle().equals(
                ChatColor.translateAlternateColorCodes(
                        '&', "&f&l" + e.getPlayer().getName() + "&6&l's Bank"))) {

            //ItemStack[] contents = e.getInventory().getContents();
            // save contents to proper page section
            BankGUI.saveBankContents((Player) e.getPlayer());
            BankGUI.getPlayer_pages().remove(e.getPlayer().getUniqueId());
        }
    }

    /**
     * Allow player to purchase a bank page
     */
    private void addPage(Inventory inv, UUID uuid, Material mat) {

        Player pl = Bukkit.getPlayer(uuid);
        if (pl == null) return;
        FileConfiguration fileConfig = FileUtil.getPlayerFileConfig(pl);
        int currentMax = FileUtil.getPlayerMaxPages(pl);
        int price = (int) Math.pow(2, currentMax + 5);

        if (mat != Material.SLIME_BALL) {
            if (currentMax < 1) currentMax = 1;
            if (currentMax >= Util.getMaxPages()) {
                pl.playSound(pl.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5f, 1.0f);
                pl.sendMessage(ChatColor.RED + "You already have the maximum number of pages!");
                return;
            }
            // ask for confirmation
            inv.setItem(6, Util.menuItem(Material.SLIME_BALL, "&a&lConfirm Purchase", "&7Purchase a new page for: &6&l" + price + "G"));
        } else {

            if (!pl.getInventory().contains(Material.GOLD_NUGGET, price)) {
                pl.playSound(pl.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5f, 1.0f);
                pl.sendMessage(ChatColor.RED + "You don't have enough gold!");
                return;
            }
            Workstation.takeItem(pl, Material.GOLD_NUGGET, price);
            fileConfig.set("pages", currentMax+1);
            pl.playSound(pl.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
            pl.sendMessage(ChatColor.GREEN + "You purchased a new bank page!");
            try {
                fileConfig.save(FileUtil.getPlayerFile(uuid));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            pl.closeInventory();
        }
    }

    private void prevPage(Inventory inv, ItemStack[] contents, UUID uuid) {
        Player pl = Bukkit.getPlayer(uuid);
        if (pl == null) return;
        int currentPage = BankGUI.getPlayer_pages().get(uuid);
        if (currentPage <= 1) return;
        BankGUI.savePage(pl, contents, currentPage);
        BankGUI.getPlayer_pages().put(uuid, currentPage-1);
        BankGUI.loadPage(pl, inv, currentPage-1);
        Bukkit.broadcastMessage("current player viewing page is: " + BankGUI.getPlayer_pages().get(uuid));
    }
}
