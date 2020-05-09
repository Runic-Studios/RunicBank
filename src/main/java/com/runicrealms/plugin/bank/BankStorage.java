package com.runicrealms.plugin.bank;

import com.runicrealms.plugin.item.util.ItemRemover;
import com.runicrealms.plugin.util.Util;
import com.runicrealms.plugin.utilities.CurrencyUtil;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class BankStorage {

    private boolean isOpen; // for hacked clients
    private int currentPage;
    private Inventory bankInv;
    private String bankTitle = "";
    private PlayerDataWrapper playerDataWrapper;

    public BankStorage(int currentPage, UUID ownerID) {
        this.isOpen = false;
        this.currentPage = currentPage;
        this.playerDataWrapper = new PlayerDataWrapper(ownerID);
        this.bankInv = getNewStorage();
        Player pl = Bukkit.getPlayer(ownerID);
        if (pl == null) return;
        this.bankTitle = ChatColor.translateAlternateColorCodes
                ('&', "&f&l" + pl.getName() + "&6&l's Bank");
    }

    /**
     * Display specified page from virtual memory.
     */
    void displayPage(int page) {
        Player pl = Bukkit.getPlayer(this.playerDataWrapper.getUuid());
        if (pl == null) return;
        this.bankInv = getNewStorage();
        // fill top row with black panes
        for (int i = 0; i < 4; i++) {
            this.bankInv.setItem(i, Util.menuItem(Material.BLACK_STAINED_GLASS_PANE, "&r", ""));
        }
        this.bankInv.setItem(5, Util.menuItem(Material.BLACK_STAINED_GLASS_PANE, "&r", ""));
        // menu buttons
        this.bankInv.setItem(4, Util.menuItem(Material.YELLOW_STAINED_GLASS_PANE, "&6&lBank of Alterra", "&7Welcome to your bank"));
        this.bankInv.setItem(6, Util.menuItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "&a&lAdd Page &f&l[&a&l" + (playerDataWrapper.getMaxPageIndex()+1) + "&f&l/5]", "&7Purchase a new bank page"));
        this.bankInv.setItem(7, Util.menuItem(Material.GREEN_STAINED_GLASS_PANE, "&f&lPrevious Page", "&7Display the previous page in your bank"));
        this.bankInv.setItem(8, Util.menuItem(Material.RED_STAINED_GLASS_PANE, "&f&lNext Page", "&7Display the next page in your bank"));
        // load page from virtual memory
        ItemStack[] pageContents = playerDataWrapper.getInventory(page);
        for (int i = 9; i < 54; i++) {
            if (pageContents[i] != null) {
                ItemStack item = pageContents[i];
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
        int maxIndex = playerDataWrapper.getMaxPageIndex();
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
            playerDataWrapper.setMaxPageIndex(maxIndex+1);
            playerDataWrapper.getBankInventories().put(maxIndex+1, new ItemStack[54]);
            pl.playSound(pl.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
            pl.sendMessage(ChatColor.GREEN + "You purchased a new bank page!");
            playerDataWrapper.saveData(true);
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
        int currentMax = playerDataWrapper.getMaxPageIndex();
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
        ItemStack[] currentInv = playerDataWrapper.getInventory(currentPage);
        for (int i = 0; i < 54; i++) {
            currentInv[i] = bankInv.getItem(i);
        }
    }

    /**
     * Builds an empty bank inventory
     */
    private Inventory getNewStorage() {
        try {
            Player pl = Bukkit.getPlayer(playerDataWrapper.getUuid());
            if (pl == null) return null;
            String name = bankTitle;
            return Bukkit.createInventory(pl, 54, name);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public boolean getOpened() {
        return isOpen;
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

    public PlayerDataWrapper getPlayerDataWrapper() {
        return playerDataWrapper;
    }

    public void setOpened(boolean isOpen) {
        this.isOpen = isOpen;
    }
}