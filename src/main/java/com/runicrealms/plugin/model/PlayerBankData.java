package com.runicrealms.plugin.model;

import com.runicrealms.plugin.RunicBank;
import com.runicrealms.plugin.api.RunicCoreAPI;
import com.runicrealms.plugin.database.Data;
import com.runicrealms.plugin.database.PlayerMongoData;
import com.runicrealms.plugin.item.util.ItemRemover;
import com.runicrealms.plugin.redis.RedisUtil;
import com.runicrealms.plugin.util.Util;
import com.runicrealms.plugin.utilities.CurrencyUtil;
import com.runicrealms.runicitems.DupeManager;
import com.runicrealms.runicitems.ItemManager;
import com.runicrealms.runicitems.config.ItemLoader;
import com.runicrealms.runicitems.item.RunicItem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerBankData implements SessionDataNested {

    public static final String MAX_PAGE_INDEX_STRING = "maxPageIndex";

    private boolean isOpen; // for hacked clients
    private int currentPage;
    private int maxPageIndex;
    private String bankTitle = "";
    private Inventory bankInv;
    private final UUID uuid;
    private final HashMap<Integer, ItemStack[]> bankInventories;

    /**
     * Builds the player's bank data wrapper from mongo, then writes to jedis and in-game memory for faster lookup
     *
     * @param uuid            of the player
     * @param playerMongoData of the player (usually from an event)
     * @param jedis           the jedis resource
     */
    public PlayerBankData(UUID uuid, PlayerMongoData playerMongoData, Jedis jedis, int currentPage) {
        this.isOpen = false;
        this.currentPage = currentPage;
        this.bankInv = getNewStorageInventory();
        this.uuid = uuid;
        bankInventories = new HashMap<>();
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        this.bankTitle = ChatColor.translateAlternateColorCodes
                ('&', "&f&l" + player.getName() + "&6&l's Bank");
        this.maxPageIndex = playerMongoData.get("bank.max_page_index", Integer.class);
        if (((!playerMongoData.has("bank.type")) // remove old-style bank saving data
                || (!playerMongoData.get("bank.type", String.class).equalsIgnoreCase("runicitems")))
                && playerMongoData.has("bank.pages")) {
            playerMongoData.remove("bank.pages");
            Bukkit.getScheduler().runTaskAsynchronously(RunicBank.getInstance(), () -> writeToMongo(playerMongoData));
        } else if (playerMongoData.has("bank.pages")) {
            for (int i = 0; i <= maxPageIndex; i++) {
                if (playerMongoData.has("bank.pages." + i)) {
                    Data pageData = playerMongoData.getSection("bank.pages." + i);
                    ItemStack[] contents = new ItemStack[54];
                    for (String key : pageData.getKeys()) {
                        if (!key.equalsIgnoreCase("type")) {
                            try {
                                RunicItem item = ItemLoader.loadItem(pageData.getSection(key), DupeManager.getNextItemId());
                                if (item != null) {
                                    contents[Integer.parseInt(key)] = item.generateItem();
                                }
                            } catch (Exception exception) {
                                Bukkit.getLogger().log(Level.WARNING, "[RunicItems] ERROR loading item " + key + " for player bank " + uuid);
                                exception.printStackTrace();
                            }
                        }
                    }
                    bankInventories.put(i, contents);
                }
            }
        }
        for (int i = 0; i <= maxPageIndex; i++) {
            if (!bankInventories.containsKey(i)) bankInventories.put(i, new ItemStack[54]);
        }
        writeToJedis(jedis);
        RunicBank.getBankManager().getBankDataMap().put(uuid, this); // add to in-game memory
    }

    /**
     * Builds the player's bank data from jedis, then adds to in-game memory
     *
     * @param uuid  of the player
     * @param jedis the jedis resource
     */
    public PlayerBankData(UUID uuid, Jedis jedis) {
        this.uuid = uuid;
        this.maxPageIndex = Integer.parseInt(jedis.get(getJedisKey(uuid) + ":maxPageIndex"));
        bankInventories = new HashMap<>();
        try {
            String parentKey = getJedisKey(uuid);
            for (int page = 0; page <= maxPageIndex; page++) {
                ItemStack[] contents = new ItemStack[54];
                for (int itemSlot = 0; itemSlot < contents.length; itemSlot++) {
                    if (!jedis.exists(parentKey + ":" + page + ":" + itemSlot)) continue;
                    Map<String, String> itemDataMap = jedis.hgetAll(parentKey + ":" + page + ":" + itemSlot); // get all item data for given slot
                    // Bukkit.broadcastMessage("item found");
                    try {
                        RunicItem item = ItemLoader.loadItem(itemDataMap, DupeManager.getNextItemId());
                        if (item != null) {
                            contents[itemSlot] = item.generateItem();
                        }
                    } catch (Exception exception) {
                        Bukkit.getLogger().log(Level.WARNING, "[ERROR]: loading BANK item " + itemSlot + " for player bank " + uuid);
                        exception.printStackTrace();
                    }
                }
                bankInventories.put(page, contents);
            }
            for (int i = 0; i <= maxPageIndex; i++) {
                if (!bankInventories.containsKey(i)) bankInventories.put(i, new ItemStack[54]);
            }
            RunicBank.getBankManager().getBankDataMap().put(uuid, this); // add to in-game memory
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ERROR]: There was a problem loading bank data from redis!");
            e.printStackTrace();
        }
    }

    /**
     * Display specified page from virtual memory.
     */
    public void displayPage(int page) {
        Player player = Bukkit.getPlayer(this.uuid);
        if (player == null) return;
        this.bankInv = getNewStorageInventory();
        // fill top row with black panes
        for (int i = 0; i < 4; i++) {
            this.bankInv.setItem(i, Util.menuItem(Material.BLACK_STAINED_GLASS_PANE, "&r", ""));
        }
        this.bankInv.setItem(5, Util.menuItem(Material.BLACK_STAINED_GLASS_PANE, "&r", ""));
        // menu buttons
        this.bankInv.setItem(4, Util.menuItem(Material.YELLOW_STAINED_GLASS_PANE, "&6&lBank of Alterra", "&7Welcome to your bank\n&aPage: &f" + (page + 1)));
        this.bankInv.setItem(6, Util.menuItem(Material.CYAN_STAINED_GLASS_PANE, "&a&lAdd Page &f&l[&a&l" + (this.getMaxPageIndex() + 1) + "&f&l/5]", "&7Purchase a new bank page"));
        this.bankInv.setItem(7, Util.menuItem(Material.GREEN_STAINED_GLASS_PANE, "&f&lPrevious Page", "&7Display the previous page in your bank"));
        this.bankInv.setItem(8, Util.menuItem(Material.RED_STAINED_GLASS_PANE, "&f&lNext Page", "&7Display the next page in your bank"));
        // load page from virtual memory
        ItemStack[] pageContents = this.getInventory(page);
        for (int i = 9; i < 54; i++) {
            if (pageContents[i] != null) {
                ItemStack item = pageContents[i];
                this.bankInv.setItem(i, item);
            }
        }
        player.openInventory(this.bankInv);
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
            this.getBankInv().setItem(6, Util.menuItem(Material.SLIME_BALL, "&a&lConfirm Purchase", "&7Purchase a new page for: &6&l" + price + "G"));
        } else {

            if (!player.getInventory().contains(Material.GOLD_NUGGET, price)) {
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5f, 1.0f);
                player.sendMessage(ChatColor.RED + "You don't have enough gold!");
                return;
            }
            ItemRemover.takeItem(player, CurrencyUtil.goldCoin(), price);
            this.setMaxPageIndex(maxIndex + 1);
            this.getBankInventories().put(maxIndex + 1, new ItemStack[54]);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
            player.sendMessage(ChatColor.GREEN + "You purchased a new bank page!");
            try (Jedis jedis = RunicCoreAPI.getNewJedisResource()) {
                writeToJedis(jedis);
            }
            player.closeInventory();
        }
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
     * Updates the current bank inventory in virtual memory
     */
    public void savePage() {
        // clear the current memory page
        ItemStack[] currentInv = this.getInventory(currentPage);
        for (int i = 0; i < 54; i++) {
            currentInv[i] = bankInv.getItem(i);
        }
    }

    /**
     * Builds an empty bank inventory
     */
    private Inventory getNewStorageInventory() {
        try {
            Player player = Bukkit.getPlayer(this.getUuid());
            if (player == null) return null;
            String name = bankTitle;
            return Bukkit.createInventory(player, 54, name);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public boolean isOpened() {
        return isOpen;
    }

    private int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public Inventory getBankInv() {
        return bankInv;
    }

    public String getBankTitle() {
        return bankTitle;
    }

    public void setOpened(boolean isOpen) {
        this.isOpen = isOpen;
    }

    public int getMaxPageIndex() {
        return maxPageIndex;
    }

    public UUID getUuid() {
        return uuid;
    }

    public HashMap<Integer, ItemStack[]> getBankInventories() {
        return bankInventories;
    }

    public ItemStack[] getInventory(int index) {
        return bankInventories.get(index);
    }

    public void setMaxPageIndex(int maxPageIndex) {
        this.maxPageIndex = maxPageIndex;
    }

    /**
     * Bank data is nested in redis (acc-wide), so here's a handy method to get the key
     *
     * @param uuid of the player
     * @return a string representing the location in jedis
     */
    public static String getJedisKey(UUID uuid) {
        return uuid + ":bank";
    }

    @Override
    public Map<String, String> toMap(Object nestedObject) {
        RunicItem runicItem = (RunicItem) nestedObject;
        return runicItem.addToJedis();
    }

    @Override
    public void writeToJedis(Jedis jedis, int... characterSlot) { // don't need 2nd param, bank is acc-wide
        // Bukkit.broadcastMessage("writing bank data to jedis");
        String key = getJedisKey(this.uuid);
        RedisUtil.removeAllFromRedis(jedis, key); // removes all sub-keys
        jedis.set(key + ":" + MAX_PAGE_INDEX_STRING, String.valueOf(this.maxPageIndex));
        jedis.expire(key + ":" + MAX_PAGE_INDEX_STRING, RedisUtil.EXPIRE_TIME);
        Map<String, Map<String, String>> itemDataMap = new HashMap<>(); // from all bank pages

        for (Map.Entry<Integer, ItemStack[]> page : bankInventories.entrySet()) {
            ItemStack[] contents = page.getValue();
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] != null) {
                    RunicItem runicItem = ItemManager.getRunicItemFromItemStack(contents[i]);
                    if (runicItem != null)
                        itemDataMap.put(page.getKey() + ":" + i, this.toMap(runicItem));
                }
            }
        }

        if (!itemDataMap.isEmpty()) {
            for (String pageAndItem : itemDataMap.keySet()) {
                if (itemDataMap.get(pageAndItem) == null) continue;
                jedis.hmset(key + ":" + pageAndItem, itemDataMap.get(pageAndItem));
                jedis.expire(key + ":" + pageAndItem, RedisUtil.EXPIRE_TIME);
            }
        }
    }

    @Override
    public PlayerMongoData writeToMongo(PlayerMongoData playerMongoData, int... ints) {
        try {
            if (playerMongoData.has("bank.pages"))
                playerMongoData.remove("bank.pages");
            playerMongoData.set("bank.max_page_index", maxPageIndex);
            for (Map.Entry<Integer, ItemStack[]> page : bankInventories.entrySet()) {
                ItemStack[] contents = page.getValue();
                for (int i = 0; i < contents.length; i++) {
                    if (contents[i] != null) {
                        RunicItem runicItem = ItemManager.getRunicItemFromItemStack(contents[i]);
                        if (runicItem != null) {
                            runicItem.addToDataSection(playerMongoData, "bank.pages." + page.getKey() + "." + i);
                        }
                    }
                }
            }
        } catch (Exception e) {
            RunicBank.getInstance().getLogger().warning("[ERROR]: There was a problem saving bank data to mongo!");
            e.printStackTrace();
        }
        return playerMongoData;
    }
}
