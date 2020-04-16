package com.runicrealms.plugin.listener;

import com.runicrealms.plugin.RunicBank;
import com.runicrealms.plugin.util.DataUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    /**
     * Save data, remove player from virtual storage
     */
    @EventHandler
    public void onLogout(PlayerQuitEvent e) {
        Bukkit.getScheduler().scheduleAsyncDelayedTask(RunicBank.getInstance(),
                () -> DataUtil.saveData(e.getPlayer().getUniqueId()), 1L);
    }
}
