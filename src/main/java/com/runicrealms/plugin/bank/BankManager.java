package com.runicrealms.plugin.bank;

import com.runicrealms.plugin.RunicBank;
import com.runicrealms.plugin.util.DataUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BankManager {

    private static final int SAVE_PERIOD = 15;
    private final ConcurrentHashMap<UUID, BankStorage> storages;
    private final ConcurrentLinkedQueue<BankStorage> queuedStorages;

    public BankManager() {

        this.storages = new ConcurrentHashMap<>();
        this.queuedStorages = new ConcurrentLinkedQueue<>();

        new BukkitRunnable() {
            @Override
            public void run() {
                saveQueuedFiles(true, true);
            }
        }.runTaskTimerAsynchronously(RunicBank.getInstance(), (100+SAVE_PERIOD), SAVE_PERIOD*20); // wait for save, 15 sec period
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

    /**
     * Writes data async
     */
    public void saveQueuedFiles(boolean limitSize, boolean saveAsync) {
        int limit;
        if (limitSize) {
            limit = (int) Math.ceil(queuedStorages.size() / 4.0);
        } else {
            limit = queuedStorages.size();
        }
        if (limit < 1)
            return;
        for (int i = 0; i < limit; i++) {
            if (queuedStorages.size() < 1) continue;
            BankStorage queued = queuedStorages.iterator().next();
            DataUtil.saveData(queued.getPlayerDataWrapper().getUuid(), saveAsync);
            queuedStorages.remove(queued);
        }
    }

    public ConcurrentHashMap<UUID, BankStorage> getStorages() {
        return storages;
    }

    public ConcurrentLinkedQueue<BankStorage> getQueuedStorages() {
        return queuedStorages;
    }
}
