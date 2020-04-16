package com.runicrealms.plugin.listener;

import com.runicrealms.plugin.RunicBank;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    /**
     * Save data, remove player from virtual storage
     */
    @EventHandler
    public void onLogout(PlayerQuitEvent e) {
        RunicBank.getBankManager().getPlayerDataObject(e.getPlayer().getUniqueId()).saveData();
        RunicBank.getBankManager().getStorages().remove(e.getPlayer().getUniqueId());
    }
}
