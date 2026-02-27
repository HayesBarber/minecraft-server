package com.hayesbarber.playerstatus;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerStatusPlugin extends JavaPlugin implements Listener {

    private WebhookService webhookService;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        getLogger().info("PlayerStatusPlugin loaded");
    }

    @Override
    public void onEnable() {
        webhookService = new WebhookService("");
        getLogger().info("PlayerStatusPlugin enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("PlayerStatusPlugin disabled");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
    }
}
