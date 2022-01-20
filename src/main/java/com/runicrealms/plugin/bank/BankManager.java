package com.runicrealms.plugin.bank;

import com.runicrealms.plugin.database.event.CacheSaveEvent;
import com.runicrealms.plugin.database.event.CacheSaveReason;
import com.runicrealms.plugin.util.DataUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BankManager {

    private final ConcurrentHashMap<UUID, BankStorage> storages;

    public BankManager() {

        this.storages = new ConcurrentHashMap<>();
    }

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
        storage.setOpened(true);

        pl.playSound(pl.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    @EventHandler
    public void onCacheSave(CacheSaveEvent event) {
        DataUtil.saveData(event.getPlayer().getUniqueId(), event.cacheSaveReason() != CacheSaveReason.SERVER_SHUTDOWN);
    }

    public ConcurrentHashMap<UUID, BankStorage> getStorages() {
        return storages;
    }

}
