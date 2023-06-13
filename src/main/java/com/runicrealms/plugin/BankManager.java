package com.runicrealms.plugin;

import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainAbortAction;
import com.runicrealms.plugin.api.BankWriteOperation;
import com.runicrealms.plugin.api.RunicBankAPI;
import com.runicrealms.plugin.listener.BankNPCListener;
import com.runicrealms.plugin.model.BankHolder;
import com.runicrealms.plugin.model.PlayerBankData;
import com.runicrealms.plugin.rdb.RunicDatabase;
import com.runicrealms.plugin.rdb.api.WriteCallback;
import com.runicrealms.plugin.rdb.event.CharacterQuitEvent;
import com.runicrealms.plugin.rdb.event.MongoSaveEvent;
import com.runicrealms.plugin.rdb.model.CharacterField;
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
import org.springframework.data.mongodb.core.query.Update;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class BankManager implements BankWriteOperation, Listener, RunicBankAPI {
    public static final TaskChainAbortAction<Player, String, ?> CONSOLE_LOG = new TaskChainAbortAction<>() {
        public void onAbort(TaskChain<?> chain, Player player, String message) {
            Bukkit.getLogger().log(Level.SEVERE, ChatColor.translateAlternateColorCodes('&', message));
        }
    };

    // For storing bank inventories during runtime
    private final HashMap<UUID, BankHolder> bankHolderMap = new HashMap<>();
    // Prevent players from accessing bank during save
    private final Set<UUID> lockedOutPlayers = new HashSet<>();

    public BankManager() {
        Bukkit.getServer().getPluginManager().registerEvents(this, RunicBank.getInstance());
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
        // Step 1: Check the mongo database
        Query query = new Query();
        query.addCriteria(Criteria.where(CharacterField.PLAYER_UUID.getField()).is(uuid));
        MongoTemplate mongoTemplate = RunicDatabase.getAPI().getDataAPI().getMongoTemplate();
        PlayerBankData result = mongoTemplate.findOne(query, PlayerBankData.class);
        if (result != null) {
            result.setBankHolder(new BankHolder(result.getUuid(), result.getMaxPageIndex(), result.getPagesMap()));
            return result;
        }
        // Step 2: If no data is found, we create some data and save it to the collection
        HashMap<Integer, RunicItem[]> pageContents = new HashMap<>() {{
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

    /**
     * Save bank data to redis on character logout ASYNC
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onLoadedQuit(CharacterQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        RunicBank.getBankWriteOperation().updatePlayerBankData
                (
                        event.getPlayer().getUniqueId(),
                        bankHolderMap.get(uuid).getRunicItemContents(),
                        true,
                        () -> bankHolderMap.remove(uuid)
                );
    }

    /**
     * Saves all marked players to mongo on server shutdown as a bulk operation
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onMongoSave(MongoSaveEvent event) {
        // Remove all currently viewing players
        Iterator<Map.Entry<UUID, BankHolder>> iterator = bankHolderMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BankHolder> entry = iterator.next();
            UUID uuid = entry.getKey();
            BankHolder bankHolder = entry.getValue();
            RunicBank.getBankWriteOperation().updatePlayerBankData
                    (
                            uuid,
                            bankHolder.getRunicItemContents(),
                            false,
                            iterator::remove
                    );
        }

        // Cancel the task timer
        RunicBank.getMongoTask().getTask().cancel();
        // Manually save all data (flush players marked for save)
        RunicBank.getMongoTask().saveAllToMongo(() -> event.markPluginSaved("bank"));
    }

    @Override
    public void updatePlayerBankData(UUID uuid, Map<Integer, RunicItem[]> newValue, boolean removeLockout, WriteCallback callback) {
        // Since we lazy-load banks on open, we can ignore players who didn't interact with the bank
        if (!bankHolderMap.containsKey(uuid)) return;
        lockedOutPlayers.add(uuid);
        MongoTemplate mongoTemplate = RunicDatabase.getAPI().getDataAPI().getMongoTemplate();
        TaskChain<?> chain = RunicBank.newChain();
        chain
                .asyncFirst(() -> {
                    // Define a query to find the InventoryData for this player
                    Query query = new Query();
                    query.addCriteria(Criteria.where(CharacterField.PLAYER_UUID.getField()).is(uuid));

                    // Define an update to set the specific field
                    Update update = new Update();
                    update.set("pagesMap", newValue);

                    // Execute the update operation
                    return mongoTemplate.updateFirst(query, update, PlayerBankData.class);
                })
                .abortIfNull(CONSOLE_LOG, null, "RunicBank failed to write to contentsMap!")
                .syncLast(updateResult -> {
                    if (removeLockout) {
                        lockedOutPlayers.remove(uuid);
                    }
                    callback.onWriteComplete();
                })
                .execute();
    }

}
