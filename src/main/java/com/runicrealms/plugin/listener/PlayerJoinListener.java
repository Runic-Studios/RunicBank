package com.runicrealms.plugin.listener;

import com.runicrealms.plugin.RunicCore;
import com.runicrealms.plugin.util.DataUtil;
import com.runicrealms.runiccharacters.api.events.CharacterLoadEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onCharacterLoad(CharacterLoadEvent e) {
        Bukkit.getScheduler().scheduleAsyncDelayedTask(RunicCore.getInstance(),
                () -> DataUtil.createBankOrLoad(e.getPlayer().getUniqueId()), 1L);
    }
}
