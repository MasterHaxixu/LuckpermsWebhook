package com.masterhaxixu.luckpermswebhook.listeners;

import com.masterhaxixu.luckpermswebhook.Main;
import com.masterhaxixu.luckpermswebhook.WebhookHandler;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.log.LogBroadcastEvent;

public class LuckpermsListener {

    private final Main plugin;

    public LuckpermsListener(Main plugin, LuckPerms luckPerms) {
        this.plugin = plugin;

        EventBus eventBus = luckPerms.getEventBus();

        eventBus.subscribe(this.plugin, LogBroadcastEvent.class, this::onLogBroadcastEvent);
    }

    private void onLogBroadcastEvent(LogBroadcastEvent e) {
        String description = e.getEntry().getDescription();
        if (description == null) {
            return;
        }

        WebhookHandler.sendWebhook(plugin, e);
    }
}
