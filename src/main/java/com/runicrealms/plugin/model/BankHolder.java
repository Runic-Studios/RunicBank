package com.runicrealms.plugin.model;

import com.runicrealms.plugin.RunicBank;
import com.runicrealms.plugin.util.Util;
import com.runicrealms.runicitems.RunicItemsAPI;
import com.runicrealms.runicitems.item.RunicItem;
import com.runicrealms.runicitems.util.CurrencyUtil;
import com.runicrealms.runicitems.util.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A class to represent a banks in-memory inventory and pages
 * We use this InventoryHolder to handle most interactions for performance.
 *
 * @author Skyfallin
 */
public class BankHolder implements InventoryHolder {
    private final UUID uuid;
    private final Map<Integer, ItemStack[]> memoryPagesMap;
    private Inventory inventory;
    private String title;
    private int currentPage = 0;
    private int maxPageIndex;
    private boolean isOpen = false;

    /**
     * A virtual bank holder inventory
     *
     * @param uuid         of the player
     * @param maxPageIndex from their data (how many bank pages?)
     * @param pagesMap     a map from redis/mongo with the contents on each paeg
     */
    public BankHolder(UUID uuid, int maxPageIndex, Map<Integer, RunicItem[]> pagesMap) {
        this.uuid = uuid;
        this.maxPageIndex = maxPageIndex;
        this.title = ChatColor.translateAlternateColorCodes('&', "&aBank &7pg. [&f" + (currentPage + 1) + "&7/" + (maxPageIndex + 1) + "]");
        this.inventory = Bukkit.createInventory(this, 54, title);
        HashMap<Integer, ItemStack[]> memoryPagesMap = new HashMap<>();
        for (Integer key : pagesMap.keySet()) {
            memoryPagesMap.put(key, this.getItemStacks(pagesMap.get(key)));
        }
        this.memoryPagesMap = memoryPagesMap;
    }

    /**
     * Logic to add a bank page for player
     *
     * @param uuid     of the player
     * @param material of the 'add' icon used to determine confirmation and/or max pages reached
     */
    public void addPage(UUID uuid, Material material) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        int maxIndex = this.getMaxPageIndex();
        int price = (int) Math.pow(2, maxIndex + 6);

        if (material != Material.SLIME_BALL) {
            if ((maxIndex + 1) >= Util.getMaxPages()) {
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5f, 1.0f);
                player.sendMessage(ChatColor.RED + "You already have the maximum number of pages!");
                return;
            }
            // ask for confirmation
            this.getInventory().setItem(6, Util.menuItem(Material.SLIME_BALL, "&a&lConfirm Purchase", "&7Purchase a new page for: &6&l" + price + "G"));
        } else {

            if (!player.getInventory().contains(Material.GOLD_NUGGET, price)) {
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5f, 1.0f);
                player.sendMessage(ChatColor.RED + "You don't have enough gold!");
                return;
            }
            RunicBank.getAPI().getLockedOutPlayers().add(uuid);
            player.closeInventory();
            ItemUtils.takeItem(player, CurrencyUtil.goldCoin(), price);
            this.setMaxPageIndex(maxIndex + 1);
            memoryPagesMap.put(maxIndex + 1, new ItemStack[54]);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
            player.sendMessage(ChatColor.GREEN + "You purchased a new bank page!");
            RunicBank.getBankWriteOperation().updatePlayerBankData
                    (
                            player.getUniqueId(),
                            RunicBank.getAPI().getBankHolderMap().get(player.getUniqueId()).getRunicItemContents(),
                            true,
                            () -> {

                            }
                    );
        }
    }

    /**
     * Display specified page from virtual memory.
     */
    public void displayPage(int page) {
        Player player = Bukkit.getPlayer(this.uuid);
        if (player == null) {
            return;
        }
        this.title = ChatColor.translateAlternateColorCodes('&', "&aBank &7pg. [&f" + (currentPage + 1) + "&7/" + (maxPageIndex + 1) + "]");
        this.inventory = Bukkit.createInventory(this, 54, title);
        // Load page from virtual memory
        ItemStack[] pageContents = memoryPagesMap.get(page);
        inventory.setContents(pageContents);
        // fill top row with black panes
        for (int i = 0; i < 4; i++) {
            inventory.setItem(i, Util.menuItem(Material.BLACK_STAINED_GLASS_PANE, "&r", ""));
        }
        inventory.setItem(5, Util.menuItem(Material.BLACK_STAINED_GLASS_PANE, "&r", ""));
        // menu buttons
        inventory.setItem(4, Util.menuItem(Material.YELLOW_STAINED_GLASS_PANE, "&6&lBank of Alterra", "&7Welcome to your bank\n&aPage: &f" + (page + 1)));
        inventory.setItem(6, Util.menuItem(Material.CYAN_STAINED_GLASS_PANE, "&a&lAdd Page &f&l[&a&l" + (this.getMaxPageIndex() + 1) + "&f&l/5]", "&7Purchase a new bank page"));
        inventory.setItem(7, Util.menuItem(Material.GREEN_STAINED_GLASS_PANE, "&f&lPrevious Page", "&7Display the previous page in your bank"));
        inventory.setItem(8, Util.menuItem(Material.RED_STAINED_GLASS_PANE, "&f&lNext Page", "&7Display the next page in your bank"));

//        for (int i = 9; i < 54; i++) {
//            if (pageContents[i] != null) { // Ignore null items
//                ItemStack item = pageContents[i];
//                inventory.setItem(i, item);
//            }
//        }
//        bankHolder.getInventory().setContents(pageContents);
        this.setOpen(true);
        player.openInventory(this.inventory);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    /**
     * Returns an ItemStack inventory associated with the page from our runic item map
     *
     * @param runicItems the array of runic items
     * @return an ItemStack inventory to display
     */
    public ItemStack[] getItemStacks(RunicItem[] runicItems) {
        ItemStack[] itemStacks = new ItemStack[54];
        for (int i = 0; i < runicItems.length; i++) {
            if (runicItems[i] == null) continue;
            itemStacks[i] = runicItems[i].generateItem();
        }
        this.setInventoryContents(itemStacks);
        return itemStacks;
    }

    public int getMaxPageIndex() {
        return maxPageIndex;
    }

    public void setMaxPageIndex(int maxPageIndex) {
        this.maxPageIndex = maxPageIndex;
    }

    /**
     * Converts an array of ItemStacks into RunicItems
     *
     * @return a map of runic item to be saved in redis/mongo
     */
    public Map<Integer, RunicItem[]> getRunicItemContents() {
        Map<Integer, RunicItem[]> pagesMap = new HashMap<>();
        for (Integer key : memoryPagesMap.keySet()) {
            pagesMap.put(key, new RunicItem[54]);
            for (int i = 9; i < memoryPagesMap.get(key).length; ++i) {
                if (memoryPagesMap.get(key)[i] != null) {
                    pagesMap.get(key)[i] = RunicItemsAPI.getRunicItemFromItemStack(memoryPagesMap.get(key)[i]);
                }
            }
        }
        return pagesMap;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    /**
     * View the next page of the player's bank
     */
    public void nextPage(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        int currentMax = this.getMaxPageIndex();
        if (currentPage >= currentMax) return;
        // update the virtual memory page
        savePage();
        this.setCurrentPage(this.getCurrentPage() + 1);
        this.displayPage(currentPage);
    }

    /**
     * View the previous page of the player's bank
     */
    public void prevPage(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        if (currentPage <= 0) return;
        // update the virtual memory page
        savePage();
        this.setCurrentPage(this.getCurrentPage() - 1);
        this.displayPage(currentPage);
    }

    /**
     * Updates the current bank page in virtual memory
     */
    public void savePage() {
        // Clear the current memory page
        ItemStack[] memoryContents = memoryPagesMap.get(currentPage);
        for (int i = 0; i < 54; i++) {
            memoryContents[i] = this.inventory.getItem(i);
        }
    }

    public void setInventoryContents(ItemStack[] contents) {
        this.inventory.setContents(contents);
    }

}