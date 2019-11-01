package com.runicrealms.plugin.bank;

import com.runicrealms.plugin.util.FileUtil;
import com.runicrealms.plugin.util.Util;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class BankStorage {

    private int currentPage;
    private int numPages;
    private Inventory bankInv;
    private ItemStack[][] bankContents;
    private UUID ownerID;

    public BankStorage(int currentPage, int numPages, UUID ownerID) {
        this.currentPage = currentPage;
        this.numPages = numPages;
        this.bankInv = getNewStorage();
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
                if (FileUtil.getPlayerFileConfig(pl).getItemStack("page_" + i + ".items." + j) != null) {
                    ItemStack item = FileUtil.getPlayerFileConfig(pl).getItemStack("page_" + i + ".items." + j);
                    this.bankContents[i][j] = item;
                }
            }
        }
    }

    /**
     * Display specified page from virtual memory.
     */
    public void displayPage(int page) {
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
        this.bankInv.setItem(6, Util.menuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "&a&lAdd Page &f&l[&a&l" + 0 + "&f&l/5]", "&7Purchase a new bank page"));
        this.bankInv.setItem(7, Util.menuItem(Material.GREEN_STAINED_GLASS_PANE, "&f&lPrevious Page", "&7Display the previous page in your bank"));
        this.bankInv.setItem(8, Util.menuItem(Material.RED_STAINED_GLASS_PANE, "&f&lNext Page", "&7Display the next page in your bank"));
        // load page
        for (int i = 9; i < 54; i++) {
            if (this.bankContents[page][i] != null) {
                ItemStack item = this.bankContents[page][i];
                this.bankInv.setItem(i, item);
            }
        }
        pl.openInventory(this.bankInv);
    }

    /**
     * View the next page of the player's bank
     */
    public void nextPage(UUID uuid) {
        Player pl = Bukkit.getPlayer(uuid);
        if (pl == null) return;
        int currentMax = FileUtil.getPlayerMaxPages(pl);
        if (currentPage >= currentMax) return;
        // update the virtual memory page
        for (int i = 9; i < 54; i++) {
            ItemStack item = this.getBankInv().getItem(i);
            bankContents[currentPage][i] = item;
        }
        this.setCurrentPage(this.getCurrentPage()+1);
        this.displayPage(currentPage);
        Bukkit.broadcastMessage("current player viewing page is: " + this.getCurrentPage());
    }

    private Inventory getNewStorage() {
        Player pl = Bukkit.getPlayer(this.ownerID);
        if (pl == null) return null;
        String name = ChatColor.translateAlternateColorCodes('&', "&f&l" + pl.getName() + "&6&l's Bank");
        return Bukkit.createInventory(pl, 54, name);
    }

    private void savePage(int page) {}
    private void saveContents() {}

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getNumPages() {
        return numPages;
    }

    public void setNumPages(int numPages) {
        this.numPages = numPages;
    }

    public Inventory getBankInv() {
        return bankInv;
    }

    public void setBankInv(Inventory bankInv) {
        this.bankInv = bankInv;
    }

    public UUID getOwnerID() {
        return ownerID;
    }

    public void setOwnerID(UUID ownerID) {
        this.ownerID = ownerID;
    }
}