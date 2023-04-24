package com.runicrealms.plugin.model;

import com.runicrealms.plugin.RunicCore;
import com.runicrealms.runicitems.DupeManager;
import com.runicrealms.runicitems.config.ItemLoader;
import com.runicrealms.runicitems.item.RunicItem;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Represents a player's bank data. Banks are account-wide!
 * This object is not cached in the server's memory (see BankHolder.java)
 * This is more like a Data Transfer Object to handle messages between server, redis, and mongo
 *
 * @author Skyfallin
 */
@Document(collection = "bank")
public class PlayerBankData implements SessionDataMongo, SessionDataNested {
    public static final String MAX_PAGE_INDEX_STRING = "maxPageIndex";
    @Id
    private ObjectId id;
    @Field("playerUuid")
    private UUID uuid;
    @Transient
    private BankHolder bankHolder;
    private int maxPageIndex;
    private HashMap<Integer, RunicItem[]> pagesMap = new HashMap<>(); // Keyed by page number

    @SuppressWarnings("unused")
    public PlayerBankData() {
        // Default constructor for Spring
    }

    /**
     * Constructor used for new players
     */
    public PlayerBankData(
            ObjectId id,
            UUID uuid,
            int maxPageIndex,
            HashMap<Integer, RunicItem[]> pagesMap) {
        this.id = id;
        this.uuid = uuid;
        this.maxPageIndex = maxPageIndex;
        this.pagesMap = pagesMap;
        this.bankHolder = new BankHolder(uuid, maxPageIndex, pagesMap); // Create in-memory object
    }

    /**
     * Constructor that builds the player's bank data from jedis, then adds to in-game memory
     *
     * @param uuid  of the player
     * @param jedis the jedis resource
     */
    public PlayerBankData(UUID uuid, Jedis jedis) {
        String database = RunicCore.getDataAPI().getMongoDatabase().getName();
        String parentKey = database + ":" + getJedisKey(uuid);
        this.uuid = uuid;
        this.maxPageIndex = Integer.parseInt(jedis.get(parentKey + ":maxPageIndex"));
        pagesMap = new HashMap<>();
        try {
            for (int page = 0; page <= maxPageIndex; page++) {
                RunicItem[] contents = new RunicItem[54];
                for (int itemSlot = 0; itemSlot < contents.length; itemSlot++) {
                    if (!jedis.exists(parentKey + ":" + page + ":" + itemSlot)) continue;
                    // Get all item data for given slot
                    Map<String, String> itemDataMap = jedis.hgetAll(parentKey + ":" + page + ":" + itemSlot);
                    try {
                        RunicItem item = ItemLoader.loadItem(itemDataMap, DupeManager.getNextItemId());
                        if (item != null) {
                            contents[itemSlot] = item;
                        }
                    } catch (Exception exception) {
                        Bukkit.getLogger().log(Level.SEVERE, "Loading RunicBank item " + itemSlot + " for player bank " + uuid);
                        exception.printStackTrace();
                    }
                }
                pagesMap.put(page, contents);
            }
            for (int i = 0; i <= maxPageIndex; i++) {
                if (!pagesMap.containsKey(i)) pagesMap.put(i, new RunicItem[54]);
            }
            this.bankHolder = new BankHolder(uuid, maxPageIndex, pagesMap); // Create in-memory object
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "There was a problem loading bank data from redis!");
            e.printStackTrace();
        }
    }

    /**
     * Bank data is nested in redis (acc-wide), so here's a handy method to get the key
     *
     * @param uuid of the player
     * @return a string representing the location in jedis
     */
    public static String getJedisKey(UUID uuid) {
        return uuid + ":bank";
    }

    @SuppressWarnings("unchecked")
    @Override
    public PlayerBankData addDocumentToMongo() {
        MongoTemplate mongoTemplate = RunicCore.getDataAPI().getMongoTemplate();
        return mongoTemplate.save(this);
    }

    public BankHolder getBankHolder() {
        return bankHolder;
    }

    public void setBankHolder(BankHolder bankHolder) {
        this.bankHolder = bankHolder;
    }

    @Override
    public Map<String, String> getDataMapFromJedis(Jedis jedis, Object o, int... ints) {
        return null;
    }

    @Override
    public List<String> getFields() {
        return null;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public Map<String, String> toMap(Object nestedObject) {
        RunicItem runicItem = (RunicItem) nestedObject;
        return runicItem.addToJedis();
    }

    @Override
    public void writeToJedis(Jedis jedis, int... ignored) {
        String database = RunicCore.getDataAPI().getMongoDatabase().getName();
        // Inform the server that this player should be saved to mongo on next task (jedis data is refreshed)
        jedis.sadd(database + ":" + "markedForSave:bank", this.uuid.toString());
        // Store the bank data
        String key = getJedisKey(this.uuid);
        RunicCore.getRedisAPI().removeAllFromRedis(jedis, database + ":" + key); // removes all sub-keys
        jedis.set(database + ":" + key + ":" + MAX_PAGE_INDEX_STRING, String.valueOf(this.maxPageIndex));
        jedis.expire(database + ":" + key + ":" + MAX_PAGE_INDEX_STRING, RunicCore.getRedisAPI().getExpireTime());
        Map<String, Map<String, String>> itemDataMap = new HashMap<>(); // from all bank pages

        for (Map.Entry<Integer, RunicItem[]> page : pagesMap.entrySet()) {
            RunicItem[] contents = page.getValue();
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] != null) {
                    itemDataMap.put(page.getKey() + ":" + i, this.toMap(contents[i]));
                }
            }
        }

        if (!itemDataMap.isEmpty()) {
            for (String pageAndItem : itemDataMap.keySet()) {
                if (itemDataMap.get(pageAndItem) == null) continue;
                jedis.hmset(database + ":" + key + ":" + pageAndItem, itemDataMap.get(pageAndItem));
                jedis.expire(database + ":" + key + ":" + pageAndItem, RunicCore.getRedisAPI().getExpireTime());
            }
        }
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public int getMaxPageIndex() {
        return maxPageIndex;
    }

    public void setMaxPageIndex(int maxPageIndex) {
        this.maxPageIndex = maxPageIndex;
    }

    public HashMap<Integer, RunicItem[]> getPagesMap() {
        return pagesMap;
    }

    public void setPagesMap(HashMap<Integer, RunicItem[]> pagesMap) {
        this.pagesMap = pagesMap;
    }

    /**
     * Ensures that our DTO is up-to-date before a save
     *
     * @param bankHolder the in-memory contents object
     */
    public void sync(BankHolder bankHolder) {
        this.setMaxPageIndex(bankHolder.getMaxPageIndex());
        this.setPagesMap(bankHolder.getRunicItemContents());
    }
}
