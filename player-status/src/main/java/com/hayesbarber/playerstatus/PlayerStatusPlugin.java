package com.hayesbarber.playerstatus;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStatusPlugin extends JavaPlugin implements Listener {

    private static final int COLOR_JOIN = 5763719;
    private static final int COLOR_LEFT = 9807270;
    private static final int COLOR_DEATH = 15158332;

    private WebhookService webhookService;
    private final Map<UUID, Instant> joinTimes = new ConcurrentHashMap<>();

    @Override
    public void onLoad() {
        saveDefaultConfig();
        getLogger().info("PlayerStatusPlugin loaded");
    }

    @Override
    public void onEnable() {
        final String url = getConfig().getString("Webhook-Url", "");
        if (url.isEmpty()) {
            getLogger().warning("Webhook Url is empty");
        }
        webhookService = new WebhookService(url);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("PlayerStatusPlugin enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("PlayerStatusPlugin disabled");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final String playerName = event.getPlayer().getName();
        final String uuidNoDashes = uuid.toString().replace("-", "");

        joinTimes.put(uuid, Instant.now());

        String avatarUrl = "https://mc-heads.net/avatar/" + uuidNoDashes + "/50";
        String timestamp = formatTimestamp(Instant.now());

        WebhookPayload payload = WebhookPayload.builder()
                .embeds(List.of(
                        WebhookPayload.Embed.builder()
                                .title("Player Joined")
                                .description(playerName + " joined")
                                .color(COLOR_JOIN)
                                .thumbnail(WebhookPayload.Thumbnail.builder().url(avatarUrl).build())
                                .footer(WebhookPayload.Footer.builder().text(timestamp).build())
                                .build()))
                .build();

        webhookService.sendMessage(payload);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final String playerName = event.getPlayer().getName();
        final String uuidNoDashes = uuid.toString().replace("-", "");

        Instant joinTime = joinTimes.remove(uuid);
        String sessionDuration = formatDuration(joinTime);

        String avatarUrl = "https://mc-heads.net/avatar/" + uuidNoDashes + "/50";

        WebhookPayload payload = WebhookPayload.builder()
                .embeds(List.of(
                        WebhookPayload.Embed.builder()
                                .title("Player Left")
                                .description(playerName + " left")
                                .color(COLOR_LEFT)
                                .thumbnail(WebhookPayload.Thumbnail.builder().url(avatarUrl).build())
                                .footer(WebhookPayload.Footer.builder().text("Session Duration: " + sessionDuration)
                                        .build())
                                .build()))
                .build();

        webhookService.sendMessage(payload);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final String uuidNoDashes = uuid.toString().replace("-", "");

        String deathMessage = event.getDeathMessage();
        if (deathMessage == null || deathMessage.isEmpty()) {
            deathMessage = event.getPlayer().getName() + " died";
        }

        String avatarUrl = "https://mc-heads.net/avatar/" + uuidNoDashes + "/50";
        String timestamp = formatTimestamp(Instant.now());

        WebhookPayload payload = WebhookPayload.builder()
                .embeds(List.of(
                        WebhookPayload.Embed.builder()
                                .title("Player Death")
                                .description(deathMessage)
                                .color(COLOR_DEATH)
                                .thumbnail(WebhookPayload.Thumbnail.builder().url(avatarUrl).build())
                                .footer(WebhookPayload.Footer.builder().text(timestamp).build())
                                .build()))
                .build();

        webhookService.sendMessage(payload);
    }

    private String formatTimestamp(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a")
                .withZone(ZoneId.of("America/New_York"));
        return instant.atZone(ZoneId.of("America/New_York")).format(formatter);
    }

    private String formatDuration(Instant joinTime) {
        if (joinTime == null) {
            return "unknown";
        }

        Instant now = Instant.now();
        long totalSeconds = java.time.Duration.between(joinTime, now).getSeconds();

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();

        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || (hours == 0 && seconds == 0)) {
            sb.append(minutes).append("m");
        }
        if (seconds > 0) {
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
                sb.append(minutes > 0 ? " " : "");
            }
            sb.append(seconds).append("s");
        }

        return sb.toString().trim();
    }
}
