package com.runicrealms.plugin.event;

import com.runicrealms.plugin.RunicBank;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class LogoutEvent implements Listener {

    /**
     * Remove players from virtual storage
     */
    @EventHandler
    public void onLogout(PlayerQuitEvent e) {
        RunicBank.getBankManager().getStorages().remove(e.getPlayer().getUniqueId());
    }
}
