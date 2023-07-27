package com.runicrealms.plugin.bank.model;

import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainAbortAction;
import com.mongodb.bulk.BulkWriteResult;
import com.runicrealms.plugin.bank.RunicBank;
import com.runicrealms.plugin.rdb.RunicDatabase;
import com.runicrealms.plugin.rdb.api.MongoTaskOperation;
import com.runicrealms.plugin.rdb.api.WriteCallback;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.query.Update;
import redis.clients.jedis.Jedis;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages the task that writes data from Redis --> MongoDB periodically
 *
 * @author Skyfallin
 */
public class MongoTask implements MongoTaskOperation {
    public static final TaskChainAbortAction<Player, String, ?> CONSOLE_LOG = new TaskChainAbortAction<Player, String, Object>() {
        public void onAbort(TaskChain<?> chain, Player player, String message) {
            Bukkit.getLogger().log(Level.SEVERE, ChatColor.translateAlternateColorCodes('&', message));
        }
    };
    private static final int MONGO_TASK_TIME = 30; // seconds
    private final BukkitTask task;

    public MongoTask() {
        task = Bukkit.getScheduler().runTaskTimerAsynchronously
                (
                        RunicBank.getInstance(),
                        () -> saveAllToMongo(() -> {
                        }),
                        MONGO_TASK_TIME * 20L,
                        MONGO_TASK_TIME * 20L
                );
    }

    @Override
    public String getCollectionName() {
        return "bank";
    }

    @Override
    public <T> Update getUpdate(T obj) {
        PlayerBankData playerBankData = (PlayerBankData) obj;
        return new Update()
                .set("maxPageIndex", playerBankData.getMaxPageIndex())
                .set("pagesMap", playerBankData.getPagesMap());
    }

    @Override
    public void saveAllToMongo(WriteCallback callback) {
        TaskChain<?> chain = RunicBank.newChain();
        chain
                .asyncFirst(this::sendBulkOperation)
                .abortIfNull(CONSOLE_LOG, null, "RunicBank failed to write to Mongo!")
                .syncLast(bulkWriteResult -> {
                    if (bulkWriteResult.wasAcknowledged()) {
                        Bukkit.getLogger().info("RunicBank modified " + bulkWriteResult.getModifiedCount() + " documents.");
                    }
                    callback.onWriteComplete();
                })
                .execute();
    }

    /**
     * A task that saves all players with the 'markedForSave:bank' key in redis to mongo.
     * Here's how this works:
     * - Whenever a player's data is written to Jedis, their UUID is added to a set in Jedis
     * - When this task runs, it checks for all players who have not been saved from Jedis --> Mongo and flushes the data, saving each entry
     * - The player is then no longer marked for save.
     */
    @Override
    public BulkWriteResult sendBulkOperation() {
        try (Jedis jedis = RunicDatabase.getAPI().getRedisAPI().getNewJedisResource()) {
            Set<String> playersToSave = jedis.smembers(getJedisSet());
            if (playersToSave.isEmpty()) return BulkWriteResult.unacknowledged();
            BulkOperations bulkOperations = RunicDatabase.getAPI().getDataAPI().getMongoTemplate().bulkOps(BulkOperations.BulkMode.UNORDERED, getCollectionName());
            for (String uuidString : playersToSave) {
                UUID uuid = UUID.fromString(uuidString);
                // Load their data async with a future
                PlayerBankData playerBankData = RunicBank.getAPI().loadPlayerBankData(uuid);
                if (RunicBank.getAPI().getBankHolderMap().get(uuid) != null) { // They are online, so ensure data is updated
                    playerBankData.sync(RunicBank.getAPI().getBankHolderMap().get(uuid));
                }
                // Player is no longer marked for save
                jedis.srem(getJedisSet(), uuid.toString());
                // Find the correct document to update
                bulkOperations.updateOne(getQuery(uuid), getUpdate(playerBankData));
            }
            return bulkOperations.execute();
        }
    }

    public BukkitTask getTask() {
        return task;
    }

}
