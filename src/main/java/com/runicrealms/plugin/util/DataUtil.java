package com.runicrealms.plugin.util;

import com.runicrealms.plugin.RunicBank;
import com.runicrealms.plugin.bank.PlayerDataObject;
import com.runicrealms.plugin.database.PlayerMongoData;

import java.util.UUID;

public class DataUtil {

    /**
     * Builds a 'bank' section in the player collection of Mongo
     */
    public static void createBankOrLoad(UUID uuid) {

        PlayerMongoData mongoData = new PlayerMongoData(uuid.toString());

        if (mongoData.get("bank") == null) {
            mongoData.set("bank.max_page_index", 0);
            mongoData.set("bank.pages.0", "");
            mongoData.save();
        }

        PlayerDataObject playerDataObject = new PlayerDataObject(uuid);
        RunicBank.getBankManager().getPlayerDataObjects().add(playerDataObject);
    }
}
