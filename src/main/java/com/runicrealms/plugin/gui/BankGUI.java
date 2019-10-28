package com.runicrealms.plugin.gui;

import com.runicrealms.plugin.util.FileUtil;
import com.runicrealms.plugin.util.Util;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class BankGUI {

    private static HashMap<UUID, Integer> player_pages = new HashMap<>();

    public BankGUI(UUID uuid) {
        Player pl = Bukkit.getPlayer(uuid);
        if (pl == null) return;
        if (!player_pages.keySet().contains(uuid)) player_pages.put(uuid, 1);
        Inventory inv = makeInventory(pl);
        File playerFile = FileUtil.getPlayerFile(pl.getUniqueId());
        FileConfiguration fileConfig = YamlConfiguration.loadConfiguration(playerFile);
        loadPage(inv, pl, fileConfig);
        pl.openInventory(inv);
    }

    private static Inventory makeInventory(Player pl) {
        String name = ChatColor.translateAlternateColorCodes('&', "&f&l" + pl.getName() + "&6&l's Bank");
        return Bukkit.createInventory(pl, 54, name);
    }

    // todo: update this method
    public static void loadPage(Inventory inv, Player pl, FileConfiguration fileConfig) {
        inv.clear();
        // fill top row with black panes
        for (int i = 0; i < 4; i++) {
            inv.setItem(i, Util.menuItem(Material.BLACK_STAINED_GLASS_PANE, "&r", ""));
        }
        inv.setItem(5, Util.menuItem(Material.BLACK_STAINED_GLASS_PANE, "&r", ""));
        // menu buttons
        inv.setItem(4, Util.menuItem(Material.YELLOW_STAINED_GLASS_PANE, "&6&lBank of Alterra", "&7Welcome to your bank"));
        int currentPages = fileConfig.getInt("pages");
        inv.setItem(6, Util.menuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "&a&lAdd Page &f&l[&a&l" + currentPages + "&f&l/5]", "&7Purchase a new bank page"));
        inv.setItem(7, Util.menuItem(Material.GREEN_STAINED_GLASS_PANE, "&f&lPrevious Page", "&7Display the previous page in your bank"));
        inv.setItem(8, Util.menuItem(Material.RED_STAINED_GLASS_PANE, "&f&lNext Page", "&7Display the next page in your bank"));
        // load page
        for (int i = 9; i < 54; i++) {
            ItemStack item = fileConfig.getItemStack("page_" + getPlayer_pages().get(pl.getUniqueId()) + ".items." + i);
            if (item != null) {
                inv.setItem(i, item);
            }
        }
    }

    public static HashMap<UUID, Integer> getPlayer_pages() {
        return player_pages;
    }

    public BankGUI getBankGUI() {
        return this;
    }
}
