package com.runicrealms.plugin.bank;

import com.runicrealms.plugin.RunicBank;
import com.runicrealms.plugin.database.PlayerMongoData;
import com.runicrealms.plugin.database.util.DatabaseUtil;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class PlayerDataWrapper {

    private int maxPageIndex;
    private UUID uuid;
    private HashMap<Integer, ItemStack[]> bankInventories;

    public PlayerDataWrapper(UUID uuid) {
        PlayerMongoData playerMongoData = new PlayerMongoData(uuid.toString());
        this.maxPageIndex = playerMongoData.get("bank.max_page_index", Integer.class);
        this.uuid = uuid;
        bankInventories = new HashMap<>();
        for (int i = 0; i <= maxPageIndex; i++) {
            String bankInventory = playerMongoData.get("bank.pages." + i, String.class);
            if (bankInventory != null)
                bankInventories.put(i, DatabaseUtil.loadInventory(bankInventory, 54));
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
            Bukkit.getScheduler().runTaskAsynchronously(RunicBank.getInstance(), () -> {
                PlayerMongoData mongoData = new PlayerMongoData(uuid.toString());
                mongoData.set("bank.max_page_index", maxPageIndex);
                for (Integer inv : bankInventories.keySet()) {
                    mongoData.set("bank.pages." + inv, DatabaseUtil.serializeInventory(bankInventories.get(inv)));
                }
                mongoData.save();
            });
        } else {
            Bukkit.getScheduler().runTask(RunicBank.getInstance(), () -> {
                PlayerMongoData mongoData = new PlayerMongoData(uuid.toString());
                mongoData.set("bank.max_page_index", maxPageIndex);
                for (Integer inv : bankInventories.keySet()) {
                    mongoData.set("bank.pages." + inv, DatabaseUtil.serializeInventory(bankInventories.get(inv)));
                }
                mongoData.save();
            });
        }
    }
}
