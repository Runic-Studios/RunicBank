package com.runicrealms.plugin.api;

import com.runicrealms.plugin.RunicBank;
import com.runicrealms.plugin.model.PlayerBankData;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface RunicBankAPI {

    /**
     * A check to determine whether a player currently has their bank open
     *
     * @param player to check
     * @return true if bank is open
     */
    static boolean isViewingBank(Player player) {
        if (RunicBank.getBankManager().getBankDataMap() == null) return false;
        if (RunicBank.getBankManager().getBankDataMap().get(player.getUniqueId()) == null) return false;
        if (RunicBank.getBankManager().getBankDataMap().get(player.getUniqueId()).getBankInv() == null) return false;
        PlayerBankData playerBankData = RunicBank.getBankManager().loadPlayerBankData(player.getUniqueId());
        return playerBankData.isOpened();
    }

    /**
     * A check to determine whether a player currently has their bank open
     *
     * @param uuid of player to check
     * @return true if bank is open
     */
    static boolean isViewingBank(UUID uuid) {
        if (RunicBank.getBankManager().getBankDataMap() == null) return false;
        if (RunicBank.getBankManager().getBankDataMap().get(uuid) == null) return false;
        if (RunicBank.getBankManager().getBankDataMap().get(uuid).getBankInv() == null) return false;
        PlayerBankData playerBankData = RunicBank.getBankManager().loadPlayerBankData(uuid);
        return playerBankData.isOpened();
    }
}
