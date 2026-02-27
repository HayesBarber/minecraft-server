package com.hayesbarber.playerstatus;

import org.bukkit.plugin.java.JavaPlugin;

public class PlayerStatusPlugin extends JavaPlugin {

    private WebhookService webhookService;

    @Override
    public void onEnable() {
        webhookService = new WebhookService("");
        getLogger().info("PlayerStatusPlugin enabled");
    }

    @Override
    public void onLoad() {
        getLogger().info("PlayerStatusPlugin loaded");
    }

    @Override
    public void onDisable() {
        getLogger().info("PlayerStatusPlugin disabled");
    }
}
