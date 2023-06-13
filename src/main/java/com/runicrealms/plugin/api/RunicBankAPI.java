package com.runicrealms.plugin.api;

import com.runicrealms.plugin.model.BankHolder;
import com.runicrealms.plugin.model.PlayerBankData;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public interface RunicBankAPI {

    /**
     * Returns the map of UUID to InventoryHolder, which adds bank inventory data to memory
     *
     * @return the holder map
     */
    HashMap<UUID, BankHolder> getBankHolderMap();

    /**
     * @return A list of players who are locked out of the bank during saving and other mechanics
     */
    Set<UUID> getLockedOutPlayers();

    /**
     * A check to determine whether a player currently has their bank open
     *
     * @param uuid of player to check
     * @return true if bank is open
     */
    boolean isViewingBank(UUID uuid);

    /**
     * Loads the player bank data ASYNC.
     * Loads from redis (if it can), otherwise falls back to MongoDB or creates a new document.
     *
     * @param uuid of the player
     * @return their bank data object
     */
    PlayerBankData loadPlayerBankData(UUID uuid);

    /**
     * Opens the bank for the player
     *
     * @param uuid of the player
     */
    void openBank(UUID uuid);

}
