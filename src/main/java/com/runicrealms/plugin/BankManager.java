package com.runicrealms.plugin;

import com.runicrealms.plugin.character.api.CharacterQuitEvent;
import com.runicrealms.plugin.database.PlayerMongoData;
import com.runicrealms.plugin.database.event.MongoSaveEvent;
import com.runicrealms.plugin.model.PlayerBankData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import redis.clients.jedis.Jedis;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BankManager implements Listener {

    private final ConcurrentHashMap<UUID, PlayerBankData> bankDataMap;

    public BankManager() {
        this.bankDataMap = new ConcurrentHashMap<>();
    }

    /**
     * Opens a bank inventory for the given player
     *
     * @param uuid of the player
     */
    public void openBank(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        PlayerBankData playerBankData = RunicBank.getBankManager().loadPlayerBankData(uuid);
        playerBankData.displayPage(0);
        playerBankData.setCurrentPage(0);
        playerBankData.setOpened(true);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    @EventHandler
    public void onLoadedQuit(CharacterQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerBankData playerBankData = RunicBank.getBankManager().loadPlayerBankData(uuid);
        playerBankData.writeToJedis(event.getJedis());
        bankDataMap.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMongoSave(MongoSaveEvent event) {
        for (UUID uuid : event.getPlayersToSave().keySet()) {
            PlayerMongoData playerMongoData = event.getPlayersToSave().get(uuid).getPlayerMongoData();
            PlayerBankData playerBankData = loadPlayerBankData(uuid);
            playerBankData.writeToMongo(playerMongoData);
        }
        event.markPluginSaved("bank");
    }

    /**
     * Tries to retrieve a PlayerBankData object from server memory, otherwise falls back to redis / mongo
     *
     * @param uuid of the player
     * @return a PlayerBankData object
     */
    public PlayerBankData loadPlayerBankData(UUID uuid) {
        // Step 1: check if bank data is memoized
        PlayerBankData playerBankData = bankDataMap.get(uuid);
        if (playerBankData != null) return playerBankData;
        // Step 2: check if hunter data is cached in redis
        try (Jedis jedis = RunicCore.getRedisAPI().getNewJedisResource()) {
            return loadPlayerBankData(uuid, jedis);
        }
    }

    /**
     * Creates a PlayerBankData object. Tries to build it from session storage (Redis) first,
     * then falls back to Mongo
     *
     * @param uuid of player who is attempting to load their data
     */
    public PlayerBankData loadPlayerBankData(UUID uuid, Jedis jedis) {
        // Step 2: check if bank data is cached in redis
        PlayerBankData playerBankData = checkRedisForBankData(uuid, jedis);
        if (playerBankData != null) return playerBankData;
        // Step 2: check mongo documents
        PlayerMongoData playerMongoData = new PlayerMongoData(uuid.toString());
        return new PlayerBankData(uuid, playerMongoData, jedis, 0);
    }

    /**
     * Checks redis to see if the currently selected character's bank data is cached.
     * And if it is, returns the PlayerBankData object
     *
     * @param uuid  of player to check
     * @param jedis the jedis resource
     * @return a PlayerBankData object if it is found in redis
     */
    public PlayerBankData checkRedisForBankData(UUID uuid, Jedis jedis) {
        if (jedis.exists(PlayerBankData.getJedisKey(uuid) + ":" + PlayerBankData.MAX_PAGE_INDEX_STRING)) {
            // Bukkit.broadcastMessage(ChatColor.GREEN + "redis bank data found, building data from redis");
            return new PlayerBankData(uuid, jedis);
        }
        // Bukkit.broadcastMessage(ChatColor.RED + "redis bank data not found");
        return null;
    }


    /**
     * Returns a map of the in-memory bank storages for player-storage key-value pairs
     *
     * @return a map of uuid to storage
     */
    public ConcurrentHashMap<UUID, PlayerBankData> getBankDataMap() {
        return bankDataMap;
    }

}
