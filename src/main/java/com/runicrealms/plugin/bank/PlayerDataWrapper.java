package com.runicrealms.plugin.bank;

import com.runicrealms.plugin.RunicBank;
import com.runicrealms.plugin.database.Data;
import com.runicrealms.plugin.database.PlayerMongoData;
import com.runicrealms.runicitems.DupeManager;
import com.runicrealms.runicitems.ItemManager;
import com.runicrealms.runicitems.config.ItemLoader;
import com.runicrealms.runicitems.item.RunicItem;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerDataWrapper {

    private int maxPageIndex;
    private final UUID uuid;
    private final HashMap<Integer, ItemStack[]> bankInventories;

    public PlayerDataWrapper(UUID uuid) {
        PlayerMongoData playerMongoData = new PlayerMongoData(uuid.toString());
        this.maxPageIndex = playerMongoData.get("bank.max_page_index", Integer.class);
        this.uuid = uuid;
        bankInventories = new HashMap<>();
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

    public void saveData(boolean saveAsync) {
        if (saveAsync) {
            Bukkit.getScheduler().runTaskAsynchronously(RunicBank.getInstance(), this::save);
        } else {
            save();
        }
    }

    private void save() {
        PlayerMongoData mongoData = new PlayerMongoData(uuid.toString());
        mongoData.set("bank.max_page_index", maxPageIndex);
        if (mongoData.has("bank.pages")) mongoData.remove("bank.pages");
        mongoData.set("bank.type", "runicitems");
        mongoData.save();
        for (Map.Entry<Integer, ItemStack[]> page : bankInventories.entrySet()) {
            ItemStack[] contents = page.getValue();
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] != null) {
                    RunicItem runicItem = ItemManager.getRunicItemFromItemStack(contents[i]);
                    if (runicItem != null) {
                        runicItem.addToData(mongoData, "bank.pages." + page.getKey() + "." + i);
                    }
                }
            }
        }
        mongoData.save();
    }
}
