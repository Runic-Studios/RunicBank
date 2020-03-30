package com.runicrealms.plugin.bank;

import com.runicrealms.plugin.item.util.ItemRemover;
import com.runicrealms.plugin.util.FileUtil;
import com.runicrealms.plugin.util.Util;
import com.runicrealms.plugin.utilities.CurrencyUtil;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.UUID;

public class BankStorage {

    private int currentPage;
    private Inventory bankInv;
    private String bankTitle = "";
    private ItemStack[][] bankContents;
    private UUID ownerID;

    BankStorage(int currentPage, UUID ownerID) {
        this.currentPage = currentPage;
        this.bankInv = getNewStorage();
        Player pl = Bukkit.getPlayer(ownerID);
        if (pl == null) return;
        this.bankTitle = ChatColor.translateAlternateColorCodes
                ('&', "&f&l" + pl.getName() + "&6&l's Bank");
        this.bankContents = new ItemStack[Util.getMaxPages()][54];
        this.ownerID = ownerID;
        loadFileContents();
    }

    /**
     * Load all stored items from player data file into virtual memory, to be saved on bank close.
     */
    private void loadFileContents() {
        Player pl = Bukkit.getPlayer(this.ownerID);
        if (pl == null) return;
        for (int i = 0; i < Util.getMaxPages(); i++) {
            for (int j = 0; j < 54; j++) {
                if (FileUtil.getPlayerFileConfig(this.ownerID).getItemStack("page_" + i + ".items." + j) != null) {
                    ItemStack item = FileUtil.getPlayerFileConfig(this.ownerID).getItemStack("page_" + i + ".items." + j);
                    this.bankContents[i][j] = item;
                }
            }
        }
    }

    /**
     * Display specified page from virtual memory.
     */
    void displayPage(int page) {
        Player pl = Bukkit.getPlayer(this.ownerID);
        if (pl == null) return;
        this.bankInv = getNewStorage();
        // fill top row with black panes
        for (int i = 0; i < 4; i++) {
            this.bankInv.setItem(i, Util.menuItem(Material.BLACK_STAINED_GLASS_PANE, "&r", ""));
        }
        this.bankInv.setItem(5, Util.menuItem(Material.BLACK_STAINED_GLASS_PANE, "&r", ""));
        // menu buttons
        this.bankInv.setItem(4, Util.menuItem(Material.YELLOW_STAINED_GLASS_PANE, "&6&lBank of Alterra", "&7Welcome to your bank"));
        this.bankInv.setItem(6, Util.menuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "&a&lAdd Page &f&l[&a&l" + (FileUtil.getMaxPageIndex(pl)+1) + "&f&l/5]", "&7Purchase a new bank page"));
        this.bankInv.setItem(7, Util.menuItem(Material.GREEN_STAINED_GLASS_PANE, "&f&lPrevious Page", "&7Display the previous page in your bank"));
        this.bankInv.setItem(8, Util.menuItem(Material.RED_STAINED_GLASS_PANE, "&f&lNext Page", "&7Display the next page in your bank"));
        // load page from virtual memory
        for (int i = 9; i < 54; i++) {
            if (this.bankContents[page][i] != null) {
                ItemStack item = this.bankContents[page][i];
                this.bankInv.setItem(i, item);
            }
        }
        pl.openInventory(this.bankInv);
    }

    /**
     * Allow player to purchase a bank page
     */
    public void addPage(UUID uuid, Material mat) {

        Player pl = Bukkit.getPlayer(uuid);
        if (pl == null) return;
        FileConfiguration fileConfig = FileUtil.getPlayerFileConfig(this.ownerID);
        int maxIndex = FileUtil.getMaxPageIndex(pl);
        int price = (int) Math.pow(2, maxIndex + 6);

        if (mat != Material.SLIME_BALL) {
            if ((maxIndex+1) >= Util.getMaxPages()) {
                pl.playSound(pl.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5f, 1.0f);
                pl.sendMessage(ChatColor.RED + "You already have the maximum number of pages!");
                return;
            }
            // ask for confirmation
            this.getBankInv().setItem(6, Util.menuItem(Material.SLIME_BALL, "&a&lConfirm Purchase", "&7Purchase a new page for: &6&l" + price + "G"));
        } else {

            if (!pl.getInventory().contains(Material.GOLD_NUGGET, price)) {
                pl.playSound(pl.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5f, 1.0f);
                pl.sendMessage(ChatColor.RED + "You don't have enough gold!");
                return;
            }
            ItemRemover.takeItem(pl, CurrencyUtil.goldCoin(), price);
            fileConfig.set("max_page_index", maxIndex+1);
            pl.playSound(pl.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
            pl.sendMessage(ChatColor.GREEN + "You purchased a new bank page!");
            FileUtil.saveFile(fileConfig, uuid);
            pl.closeInventory();
        }
    }

    /**
     * View the previous page of the player's bank
     */
    public void prevPage(UUID uuid) {
        Player pl = Bukkit.getPlayer(uuid);
        if (pl == null) return;
        if (currentPage <= 0) return;
        // update the virtual memory page
        savePage();
        this.setCurrentPage(this.getCurrentPage()-1);
        this.displayPage(currentPage);
    }

    /**
     * View the next page of the player's bank
     */
    public void nextPage(UUID uuid) {
        Player pl = Bukkit.getPlayer(uuid);
        if (pl == null) return;
        int currentMax = FileUtil.getMaxPageIndex(pl);
        if (currentPage >= currentMax) return;
        // update the virtual memory page
        savePage();
        this.setCurrentPage(this.getCurrentPage()+1);
        this.displayPage(currentPage);
    }

    /**
     * Updates the current bank inventory in virtual memory
     */
    public void savePage() {
        // clear the current memory page
        Arrays.fill(this.bankContents[currentPage], null);
        Inventory current = this.getBankInv();
        for (int i = 0; i < 54; i++) {
            this.bankContents[currentPage][i] = current.getItem(i);
        }
    }

    /**
     * Saves all contents loaded in memory to flat file.
     */
    public void saveContents() {
        FileConfiguration fileConfig = FileUtil.getPlayerFileConfig(this.ownerID);
        for (int i = 0; i < Util.getMaxPages(); i++) {
            fileConfig.set("page_" + i, null); // wipe currently saved items
            for (int j = 9; j < 54; j++) {
                if (this.bankContents[i][j] != null) {
                    ItemStack item = this.bankContents[i][j];
                    fileConfig.set("page_" + i + ".items." + j, item);
                }
            }
        }
        FileUtil.saveFile(fileConfig, this.ownerID);
    }

    /**
     * Builds an empty bank inventory
     */
    private Inventory getNewStorage() {
        Player pl = Bukkit.getPlayer(this.ownerID);
        if (pl == null) return null;
        String name = bankTitle;
        return Bukkit.createInventory(pl, 54, name);
    }

    private int getCurrentPage() {
        return currentPage;
    }

    void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public Inventory getBankInv() {
        return bankInv;
    }

    public String getBankTitle() {
        return bankTitle;
    }
}