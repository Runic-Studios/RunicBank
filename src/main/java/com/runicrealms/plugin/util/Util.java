package com.runicrealms.plugin.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Util {

    //private static final int PAGE_BASE_COST = 64;
    private static final int MAX_PAGES = 5;

    public static ItemStack menuItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        Objects.requireNonNull(meta).setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        if (!lore.equals("")) {
            List<String> loree = new ArrayList<>();
            for (String line : lore.split("\n")) {
                loree.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(loree);
        }
        item.setItemMeta(meta);
        return item;
    }

//    public static int getPageBaseCost() {
//        return PAGE_BASE_COST;
//    }

    public static int getMaxPages() {
        return MAX_PAGES;
    }
}
