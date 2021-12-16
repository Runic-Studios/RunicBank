package com.runicrealms.plugin.api;

import com.runicrealms.plugin.RunicBank;
import com.runicrealms.plugin.bank.BankStorage;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RunicBankAPI {

    /**
     * A check to determine whether a player currently has their bank open
     *
     * @param player to check
     * @return true if bank is open
     */
    public static boolean isViewingBank(Player player) {
        if (RunicBank.getBankManager().getStorages() == null) return false;
        if (RunicBank.getBankManager().getStorages().get(player.getUniqueId()) == null) return false;
        if (RunicBank.getBankManager().getStorages().get(player.getUniqueId()).getBankInv() == null) return false;
        BankStorage bankStorage = RunicBank.getBankManager().getStorages().get(player.getUniqueId());
        return bankStorage.isOpened();
    }

    /**
     * A check to determine whether a player currently has their bank open
     *
     * @param uuid of player to check
     * @return true if bank is open
     */
    public static boolean isViewingBank(UUID uuid) {
        if (RunicBank.getBankManager().getStorages() == null) return false;
        if (RunicBank.getBankManager().getStorages().get(uuid) == null) return false;
        if (RunicBank.getBankManager().getStorages().get(uuid).getBankInv() == null) return false;
        BankStorage bankStorage = RunicBank.getBankManager().getStorages().get(uuid);
        return bankStorage.isOpened();
    }
}
