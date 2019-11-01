package com.runicrealms.plugin.gui;

import com.runicrealms.plugin.util.FileUtil;
import com.runicrealms.plugin.util.Util;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.*;

public class BankGUI {

    private static HashMap<UUID, Integer> player_pages = new HashMap<>();
    private static HashMap<UUID, ItemStack[][]> bank_inventory = new HashMap<>();

    public BankGUI(UUID uuid) {
        Player pl = Bukkit.getPlayer(uuid);
        if (pl == null) return;
        player_pages.put(uuid, 1);//if (!player_pages.keySet().contains(uuid))
        Bukkit.broadcastMessage(getPlayer_pages().get(uuid) + "");
        Inventory inv = makeInventory(pl);
        if (!bank_inventory.containsKey(uuid)) {
            bank_inventory.put(uuid, new ItemStack[Util.getMaxPages()][54]); // create a 2D array w/ max # of pages
            //loadFileContents(pl);
        }
        loadPage(pl, inv,0);
        pl.openInventory(inv);
    }

    private static Inventory makeInventory(Player pl) {
        String name = ChatColor.translateAlternateColorCodes('&', "&f&l" + pl.getName() + "&6&l's Bank");
        return Bukkit.createInventory(pl, 54, name);
    }

    /**
     * Load specified page from virtual memory
     */
    public static void loadPage(Player pl, Inventory inv, int pageIndex) {
        inv.clear();
        // fill top row with black panes
        for (int i = 0; i < 4; i++) {
            inv.setItem(i, Util.menuItem(Material.BLACK_STAINED_GLASS_PANE, "&r", ""));
        }
        inv.setItem(5, Util.menuItem(Material.BLACK_STAINED_GLASS_PANE, "&r", ""));
        // menu buttons
        inv.setItem(4, Util.menuItem(Material.YELLOW_STAINED_GLASS_PANE, "&6&lBank of Alterra", "&7Welcome to your bank"));
        inv.setItem(6, Util.menuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "&a&lAdd Page &f&l[&a&l" + 0 + "&f&l/5]", "&7Purchase a new bank page"));
        inv.setItem(7, Util.menuItem(Material.GREEN_STAINED_GLASS_PANE, "&f&lPrevious Page", "&7Display the previous page in your bank"));
        inv.setItem(8, Util.menuItem(Material.RED_STAINED_GLASS_PANE, "&f&lNext Page", "&7Display the next page in your bank"));
        // load page
        ItemStack[] page = bank_inventory.get(pl.getUniqueId())[pageIndex];
        for (int i = 9; i < 54; i++) {
            ItemStack item = page[i];
            if (item != null) {
                inv.setItem(i, item);
            }
        }
        pl.openInventory(inv);
    }

    /**
     * Save bank contents to player data file
     */
    public static void saveBankContents(Player pl) {

        FileConfiguration itemConfig = FileUtil.getPlayerFileConfig(pl);
        ItemStack[][] bankContents = bank_inventory.get(pl.getUniqueId());

        // save new contents
        for (int i = 0; i < FileUtil.getPlayerMaxPages(pl); i++) {
            for (int j = 9; j < 54; j++) {
                ItemStack item = bankContents[i][j];
                if (item != null)
                    itemConfig.set("page_" + i + ".items." + j, item);
            }
        }

//        // delete removed items
//        ConfigurationSection items = itemConfig.getConfigurationSection("page_" + page + ".items");
//        if (items == null) return;
//        for (String s : items.getKeys(false)) {
//            if (contents[Integer.parseInt(s)] == null
//                    || (contents[Integer.parseInt(s)] != null && contents[Integer.parseInt(s)].getType() == Material.AIR)) {
//                items.set(s, null);
//            }
//        }

        // save file
        try {
            itemConfig.save(FileUtil.getPlayerFile(pl.getUniqueId()));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Save the player's current inventory screen (bank) to virtual memory
     */
    public static void savePage(Player pl, ItemStack[] contents, int pageIndex) {

        ItemStack[][] bankContents = bank_inventory.get(pl.getUniqueId());

        for (int i = 9; i < 54; i++) {
            ItemStack item = contents[i];
            bankContents[pageIndex][i] = item;
        }
    }

    public static HashMap<UUID, Integer> getPlayer_pages() {
        return player_pages;
    }
}
