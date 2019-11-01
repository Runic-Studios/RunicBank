package com.runicrealms.plugin.bank;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class BankManager {

    private HashMap<UUID, BankStorage> storages;

    public BankManager() {
        this.storages = new HashMap<>();
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
    }

    public HashMap<UUID, BankStorage> getStorages() { return this.storages; }
}
