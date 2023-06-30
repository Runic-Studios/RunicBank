package com.runicrealms.plugin.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * An event that fires right before a player opens their bank
 *
 * @author BoBoBalloon
 * @since 6/29/23
 */
public class BankOpenEvent extends PlayerEvent implements Cancellable {
    private boolean cancelled;

    private static final HandlerList handlers = new HandlerList();

    public BankOpenEvent(@NotNull Player player) {
        super(player);
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
