package com.runicrealms.plugin.util;

import com.runicrealms.plugin.RunicBank;
import com.runicrealms.plugin.bank.BankStorage;
import com.runicrealms.plugin.database.PlayerMongoData;

import java.util.UUID;

public class DataUtil {

    /**
     * Builds a 'bank' section in the player collection of Mongo
     */
    public static void createBankOrLoad(UUID uuid) {

        PlayerMongoData mongoData = new PlayerMongoData(uuid.toString());

        if ((!mongoData.has("bank"))
                || (mongoData.has("bank.type") && (!mongoData.get("bank.type", String.class).equalsIgnoreCase("runicitems")))) {

            if (mongoData.has("bank")) {
                mongoData.remove("bank");
                mongoData.save();
            }
            mongoData.set("bank.max_page_index", 0);
            mongoData.save();
        }

        BankStorage storage = new BankStorage(0, uuid);
        RunicBank.getBankManager().getStorages().put(uuid, storage);
    }

    public static void saveData(UUID uuid, boolean saveAsync) {
        if (RunicBank.getBankManager().getStorages().get(uuid) != null) {
            BankStorage storage = RunicBank.getBankManager().getStorages().get(uuid);
            storage.getPlayerDataWrapper().saveData(saveAsync);
        }
    }
}
