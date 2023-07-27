package com.runicrealms.plugin.bank.api;

import com.runicrealms.plugin.rdb.api.WriteCallback;
import com.runicrealms.plugin.runicitems.item.RunicItem;

import java.util.Map;
import java.util.UUID;

public interface BankWriteOperation {

    /**
     * Updates the "pagesMap" field of a PlayerBankData document object
     *
     * @param uuid          of the player
     * @param newValue      the new value for the field
     * @param removeLockout whether to unlock the bank when the save is complete (false when server shut down)
     * @param callback      a sync function to execute when TaskChain is complete
     */
    void updatePlayerBankData(UUID uuid, Map<Integer, RunicItem[]> newValue, int maxPageIndex, boolean removeLockout, WriteCallback callback);

}
