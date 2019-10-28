package com.runicrealms.plugin.event;

import com.runicrealms.plugin.gui.BankGUI;
import com.runicrealms.plugin.professions.Workstation;
import com.runicrealms.plugin.util.FileUtil;
import com.runicrealms.plugin.util.Util;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
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
        String title = ChatColor.translateAlternateColorCodes(
                '&', "&f&l" + pl.getName() + "&6&l's Bank");
        // disable interactions on first 9 slots
        if (inv.getTitle().equals(title)) {
            if (e.getClickedInventory() != null
                    && e.getClickedInventory().getTitle().equals(title) && e.getSlot() < 9) {
                pl.playSound(pl.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                e.setCancelled(true);
                switch (e.getSlot()) {
                    case 6:
                        addPage(inv, e.getWhoClicked().getUniqueId(), inv.getItem(6).getType());
                        break;
                    case 7:
                        prevPage(inv, inv.getContents(), pl.getUniqueId());
                        break;
                    case 8:
                        nextPage(inv, inv.getContents(), pl.getUniqueId());
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

            ItemStack[] contents = e.getInventory().getContents();
            // save contents to proper page section
            saveContents(contents, (Player) e.getPlayer(), BankGUI.getPlayer_pages().get(e.getPlayer().getUniqueId()));
            BankGUI.getPlayer_pages().remove(e.getPlayer().getUniqueId());
        }
    }

    private void addPage(Inventory inv, UUID uuid, Material mat) {

        Player pl = Bukkit.getPlayer(uuid);
        if (pl == null) return;
        File file = FileUtil.getPlayerFile(uuid);
        FileConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);
        int currentPages = fileConfig.getInt("pages");
        int price = (int) Math.pow(2, currentPages + 5);

        if (mat != Material.SLIME_BALL) {
            if (currentPages < 1) currentPages = 1;
            if (currentPages >= Util.getMaxPages()) {
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
            saveContents(inv.getContents(), pl, BankGUI.getPlayer_pages().get(uuid));
            fileConfig.set("pages", currentPages+1);
            pl.playSound(pl.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
            pl.sendMessage(ChatColor.GREEN + "You purchased a new bank page!");
            try {
                fileConfig.save(file);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            pl.closeInventory();
        }
    }

    private void nextPage(Inventory inv, ItemStack[] contents, UUID uuid) {
        Player pl = Bukkit.getPlayer(uuid);
        if (pl == null) return;
        int currentPage = BankGUI.getPlayer_pages().get(uuid);
        File file = FileUtil.getPlayerFile(pl.getUniqueId());
        FileConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);
        int currentMax = fileConfig.getInt("pages");
        if (currentPage >= currentMax) return;
        saveContents(contents, pl, currentPage);
        BankGUI.getPlayer_pages().put(uuid, currentPage+1);
        BankGUI.loadPage(inv, pl, fileConfig);
        Bukkit.broadcastMessage("current player viewing page is: " + BankGUI.getPlayer_pages().get(uuid));
    }

    private void prevPage(Inventory inv, ItemStack[] contents, UUID uuid) {
        Player pl = Bukkit.getPlayer(uuid);
        if (pl == null) return;
        int currentPage = BankGUI.getPlayer_pages().get(uuid);
        if (currentPage <= 1) return;
        File file = FileUtil.getPlayerFile(pl.getUniqueId());
        FileConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);
        saveContents(contents, pl, currentPage);
        BankGUI.getPlayer_pages().put(uuid, currentPage-1);
        BankGUI.loadPage(inv, pl, fileConfig);
        Bukkit.broadcastMessage("current player viewing page is: " + BankGUI.getPlayer_pages().get(uuid));
    }

    /**
     * Save bank contents to player data file
     * @param page is the current page of the player's bank
     */
    private void saveContents(ItemStack[] contents, Player pl, int page) {
        File playerFile = FileUtil.getPlayerFile(pl.getUniqueId());
        FileConfiguration itemConfig = YamlConfiguration.loadConfiguration(playerFile);

        // save new contents
        for (int i = 9; i < 54; i++) {
            if (contents[i] != null) {
                itemConfig.set("page_" + page + ".items." + i, contents[i]);
            }
        }

        // delete removed items
        ConfigurationSection items = itemConfig.getConfigurationSection("page_" + page + ".items");
        if (items == null) return;
        for (String s : items.getKeys(false)) {
            if (contents[Integer.parseInt(s)] == null
                    || (contents[Integer.parseInt(s)] != null && contents[Integer.parseInt(s)].getType() == Material.AIR)) {
                items.set(s, null);
            }
        }

        // save file
        try {
            itemConfig.save(playerFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
