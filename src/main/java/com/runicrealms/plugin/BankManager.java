package com.runicrealms.plugin;

import com.runicrealms.libs.taskchain.TaskChain;
import com.runicrealms.plugin.api.RunicBankAPI;
import com.runicrealms.plugin.character.api.CharacterQuitEvent;
import com.runicrealms.plugin.database.event.MongoSaveEvent;
import com.runicrealms.plugin.listener.BankNPCListener;
import com.runicrealms.plugin.model.BankHolder;
import com.runicrealms.plugin.model.CharacterField;
import com.runicrealms.plugin.model.PlayerBankData;
import com.runicrealms.runicitems.item.RunicItem;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import redis.clients.jedis.Jedis;

import java.util.*;

import static com.runicrealms.plugin.model.MongoTask.CONSOLE_LOG;

public class BankManager implements Listener, RunicBankAPI {
    // For storing bank inventories during runtime
    private final HashMap<UUID, BankHolder> bankHolderMap = new HashMap<>();
    // Prevent players from accessing bank during save
    private final Set<UUID> lockedOutPlayers = new HashSet<>();

    public BankManager() {
        Bukkit.getServer().getPluginManager().registerEvents(this, RunicBank.getInstance());
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
        String database = RunicCore.getDataAPI().getMongoDatabase().getName();
        if (jedis.exists(database + ":" + PlayerBankData.getJedisKey(uuid) + ":" + PlayerBankData.MAX_PAGE_INDEX_STRING)) {
            return new PlayerBankData(uuid, jedis);
        }
        return null;
    }

    @Override
    public HashMap<UUID, BankHolder> getBankHolderMap() {
        return bankHolderMap;
    }

    @Override
    public Set<UUID> getLockedOutPlayers() {
        return lockedOutPlayers;
    }

    @Override
    public boolean isViewingBank(UUID uuid) {
        if (bankHolderMap.isEmpty() || !bankHolderMap.containsKey(uuid)) {
            return false;
        }
        return bankHolderMap.get(uuid).isOpen();
    }

    @Override
    public PlayerBankData loadPlayerBankData(UUID uuid) {
        try (Jedis jedis = RunicCore.getRedisAPI().getNewJedisResource()) {
            // Step 1: Check if bank data exists in redis
            PlayerBankData playerBankData = checkRedisForBankData(uuid, jedis);
            if (playerBankData != null) return playerBankData;
        }
        // Step 2: Check the mongo database
        Query query = new Query();
        query.addCriteria(Criteria.where(CharacterField.PLAYER_UUID.getField()).is(uuid));
        MongoTemplate mongoTemplate = RunicCore.getDataAPI().getMongoTemplate();
        List<PlayerBankData> results = mongoTemplate.find(query, PlayerBankData.class);
        if (results.size() > 0) {
            PlayerBankData result = results.get(0);
            result.setBankHolder(new BankHolder(result.getUuid(), result.getMaxPageIndex(), result.getPagesMap()));
            try (Jedis jedis = RunicCore.getRedisAPI().getNewJedisResource()) {
                result.writeToJedis(jedis);
            }
            return result;
        }
        // Step 3: If no data is found, we create some data and save it to the collection
        HashMap<Integer, RunicItem[]> pageContents = new HashMap<Integer, RunicItem[]>() {{
            put(0, new RunicItem[54]);
        }};
        PlayerBankData playerBankData = new PlayerBankData
                (
                        new ObjectId(),
                        uuid,
                        0,
                        pageContents
                );
        // Write new data to mongo
        playerBankData.addDocumentToMongo();
        // Write to redis
        try (Jedis jedis = RunicCore.getRedisAPI().getNewJedisResource()) {
            playerBankData.writeToJedis(jedis);
        }
        return playerBankData;
    }

    @Override
    public void openBank(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        if (lockedOutPlayers.contains(uuid)) {
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5f, 1.0f);
            player.sendMessage(ChatColor.YELLOW + "Your bank is saving! Try again in a moment.");
            return;
        }
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 0.5f, 1.0f);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        BankHolder bankHolder = bankHolderMap.get(uuid);
        // Lazy-load bank the first time it is opened
        if (bankHolder == null) {
            BankNPCListener.databaseRequesters.add(uuid);
            TaskChain<?> chain = RunicBank.newChain();
            chain
                    .asyncFirst(() -> loadPlayerBankData(uuid))
                    .abortIfNull(CONSOLE_LOG, player, "RunicBank failed to load on openBank()!")
                    .syncLast(playerBankData -> {
                        BankNPCListener.databaseRequesters.remove(uuid);
                        bankHolderMap.put(uuid, playerBankData.getBankHolder());
                        bankHolderMap.get(uuid).setCurrentPage(0);
                        bankHolderMap.get(uuid).displayPage(0);
                    })
                    .execute();
        } else {
            bankHolder.setCurrentPage(0);
            bankHolder.displayPage(0);
        }
    }

    @Override
    public void saveBank(Player player) {
        // Since we lazy-load banks on open, we can ignore players who didn't interact with the bank
        if (!bankHolderMap.containsKey(player.getUniqueId())) return;
        lockedOutPlayers.add(player.getUniqueId());
        UUID uuid = player.getUniqueId();
        TaskChain<?> chain = RunicBank.newChain();
        chain
                .asyncFirst(() -> loadPlayerBankData(uuid))
                .abortIfNull(CONSOLE_LOG, player, "RunicBank failed to save on quit!")
                .sync(playerBankData -> {
                    // Sync current bank to object retrieved from Redis/Mongo (ensure they match)
                    playerBankData.sync(bankHolderMap.get(uuid));
                    return playerBankData;
                })
                .asyncLast(playerBankData -> {
                    try (Jedis jedis = RunicCore.getRedisAPI().getNewJedisResource()) {
                        playerBankData.writeToJedis(jedis);
                    }
                    lockedOutPlayers.remove(player.getUniqueId());
                })
                .execute();
    }

    /**
     * Save bank data to redis on character logout ASYNC
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onLoadedQuit(CharacterQuitEvent event) {
        saveBank(event.getPlayer());
        bankHolderMap.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Saves all marked players to mongo on server shutdown as a bulk operation
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onMongoSave(MongoSaveEvent event) {
        // Cancel the task timer
        RunicBank.getMongoTask().getTask().cancel();
        // Manually save all data (flush players marked for save)
        RunicBank.getMongoTask().saveAllToMongo(() -> event.markPluginSaved("bank"));
    }

}
