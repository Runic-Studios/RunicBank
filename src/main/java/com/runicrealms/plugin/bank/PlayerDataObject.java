package com.runicrealms.plugin.bank;

import com.runicrealms.plugin.database.PlayerMongoData;
import com.runicrealms.plugin.database.util.DatabaseUtil;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class PlayerDataObject {

    private int maxPageIndex;
    private UUID uuid;
    private HashMap<Integer, ItemStack[]> bankInventories;

    public PlayerDataObject(UUID uuid) {
        PlayerMongoData playerMongoData = new PlayerMongoData(uuid.toString());
        this.maxPageIndex = playerMongoData.get("bank.max_page_index", Integer.class);
        this.uuid = uuid;
        bankInventories = new HashMap<>();
        for (int i = 0; i < maxPageIndex; i++) {
            String bankInventory = playerMongoData.get("bank.pages." + i, String.class);
            if (bankInventory != null)
                bankInventories.put(i, DatabaseUtil.loadInventory(bankInventory));
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

    public void saveData() {
        PlayerMongoData mongoData = new PlayerMongoData(uuid.toString());
        mongoData.set("bank.max_page_index", maxPageIndex);
        for (Integer inv : bankInventories.keySet()) {
            mongoData.set("bank.pages." + inv, DatabaseUtil.serializeInventory(bankInventories.get(inv)));
        }
        mongoData.save();
    }
}
