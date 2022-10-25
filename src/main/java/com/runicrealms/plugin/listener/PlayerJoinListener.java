package com.runicrealms.plugin.listener;

import com.runicrealms.plugin.RunicBank;
import com.runicrealms.plugin.RunicCore;
import com.runicrealms.plugin.character.api.CharacterSelectEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onCharacterLoad(CharacterSelectEvent event) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(RunicCore.getInstance(),
                () -> RunicBank.getBankManager().loadPlayerBankData(event.getPlayer().getUniqueId()), 1L);
    }
}
