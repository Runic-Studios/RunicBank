package com.runicrealms.plugin.listener;

import com.runicrealms.plugin.RunicCore;
import com.runicrealms.plugin.character.api.CharacterSelectEvent;
import com.runicrealms.plugin.util.DataUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onCharacterLoad(CharacterSelectEvent e) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(RunicCore.getInstance(),
                () -> DataUtil.createBankOrLoad(e.getPlayer().getUniqueId()), 1L);
    }
}
