package com.runicrealms.plugin.bank;

import com.runicrealms.plugin.player.cache.PlayerCache;
import com.runicrealms.plugin.util.DataUtil;
import com.runicrealms.runiccharacters.api.RunicCharactersApi;
import com.runicrealms.runiccharacters.config.UserConfig;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class BankManager {

    private HashMap<UUID, BankStorage> storages;

    public BankManager() {
        this.storages = new HashMap<>();
    }

    // TODO: CREATE QUEUE TO SAVE PLAYER DATA OBJECTS FROM CORE

    public void openBank(UUID uuid) {
        Player pl = Bukkit.getPlayer(uuid);
        if (pl == null) return;
        BankStorage storage;
        if (!storages.containsKey(uuid)) {
            storage = new BankStorage(0, uuid);
            storages.put(uuid, storage);
        } else {
            storage = storages.get(uuid);
        }
        storage.displayPage(0);
        storage.setCurrentPage(0);

        pl.playSound(pl.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    /**
     * Writes data async
     */
    public void saveQueuedFiles(boolean limitSize) {
//        int limit;
//        if (limitSize) {
//            limit = (int) Math.ceil(queuedCaches.size() / 4);
//        } else {
//            limit = queuedCaches.size();
//        }
//        UserConfig userConfig;
//        for (int i = 0; i < limit; i++) {
//            if (queuedCaches.size() < 1) continue;
//            if (!queuedCaches.iterator().hasNext()) continue;
//            PlayerCache queued = queuedCaches.iterator().next();
//            userConfig = RunicCharactersApi.getUserConfig(queued.getPlayerID());
//            setFieldsSaveFile(queued, userConfig);
//            queuedCaches.remove(queued);
//        }
        for (UUID uuid : storages.keySet())
            DataUtil.saveData(uuid);
    }

    public HashMap<UUID, BankStorage> getStorages() {
        return this.storages;
    }
}
