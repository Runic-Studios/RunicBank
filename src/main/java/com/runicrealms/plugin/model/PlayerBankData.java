package com.runicrealms.plugin.model;

import com.runicrealms.plugin.rdb.RunicDatabase;
import com.runicrealms.plugin.rdb.model.SessionDataMongo;
import com.runicrealms.runicitems.item.RunicItem;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a player's bank data. Banks are account-wide!
 * This object is not cached in the server's memory (see BankHolder.java)
 * This is more like a Data Transfer Object to handle messages between server, redis, and mongo
 *
 * @author Skyfallin
 */
@SuppressWarnings("unused")
@Document(collection = "bank")
public class PlayerBankData implements SessionDataMongo {
    @Id
    private ObjectId id;
    @Field("playerUuid")
    private UUID uuid;
    @Transient
    private BankHolder bankHolder;
    private int maxPageIndex;
    private Map<Integer, RunicItem[]> pagesMap = new HashMap<>(); // Keyed by page number

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

    @SuppressWarnings("unchecked")
    @Override
    public PlayerBankData addDocumentToMongo() {
        MongoTemplate mongoTemplate = RunicDatabase.getAPI().getDataAPI().getMongoTemplate();
        return mongoTemplate.save(this);
    }

    public BankHolder getBankHolder() {
        return bankHolder;
    }

    public void setBankHolder(BankHolder bankHolder) {
        this.bankHolder = bankHolder;
    }

    public UUID getUuid() {
        return uuid;
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

    public Map<Integer, RunicItem[]> getPagesMap() {
        return pagesMap;
    }

    public void setPagesMap(Map<Integer, RunicItem[]> pagesMap) {
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
