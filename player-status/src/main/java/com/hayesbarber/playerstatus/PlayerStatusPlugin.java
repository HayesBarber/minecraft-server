package com.hayesbarber.playerstatus;

import org.bukkit.Material;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStatusPlugin extends JavaPlugin implements Listener {

    private static final int COLOR_JOIN = 5763719;
    private static final int COLOR_LEFT = 9807270;
    private static final int COLOR_DEATH = 15158332;
    private static final int COLOR_DIAMOND = 4886754;
    private static final int COLOR_ENDERMAN = 5527302;
    private static final int DIAMOND_DELAY_TICKS = 200;
    private static final int DIAMOND_CAP = 10;

    private WebhookService webhookService;
    private final Map<UUID, Instant> joinTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> diamondCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> scheduledTaskIds = new ConcurrentHashMap<>();

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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        joinTimes.put(player.getUniqueId(), Instant.now());

        String avatarUrl = getAvatarUrl(player.getUniqueId());
        String footer = String.format("Level %d • XP: %.2f", player.getLevel(), player.getExp());

        WebhookPayload payload = buildEmbed("Player Joined", player.getName() + " joined the game", COLOR_JOIN,
                avatarUrl,
                footer);

        webhookService.sendMessage(payload)
                .thenAccept(statusCode -> getLogger().info("Join webhook response: " + statusCode));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Instant joinTime = joinTimes.remove(uuid);
        String sessionDuration = formatDuration(joinTime);

        String avatarUrl = getAvatarUrl(uuid);

        WebhookPayload payload = buildEmbed("Player Left", player.getName() + " left the game", COLOR_LEFT, avatarUrl,
                "Session Duration: " + sessionDuration);

        webhookService.sendMessage(payload)
                .thenAccept(statusCode -> getLogger().info("Quit webhook response: " + statusCode));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        String deathMessage = event.getDeathMessage();
        if (deathMessage == null || deathMessage.isEmpty()) {
            deathMessage = player.getName() + " died";
        }

        String avatarUrl = getAvatarUrl(player.getUniqueId());
        String footer = String.format("Player was level %d", player.getLevel());

        WebhookPayload payload = buildEmbed("Player Death", deathMessage, COLOR_DEATH, avatarUrl, footer);

        webhookService.sendMessage(payload)
                .thenAccept(statusCode -> getLogger().info("Death webhook response: " + statusCode));
    }

    private WebhookPayload buildEmbed(String title, String description, int color, String avatarUrl,
            String footerText) {
        return WebhookPayload.builder()
                .embeds(List.of(
                        WebhookPayload.Embed.builder()
                                .title(title)
                                .description(description)
                                .color(color)
                                .thumbnail(WebhookPayload.Thumbnail.builder().url(avatarUrl).build())
                                .footer(WebhookPayload.Footer.builder().text(footerText).build())
                                .build()))
                .build();
    }

    private String getAvatarUrl(UUID uuid) {
        return "https://mc-heads.net/avatar/" + getUuidNoDashes(uuid) + "/50";
    }

    private String getUuidNoDashes(UUID uuid) {
        return uuid.toString().replace("-", "");
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
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0) {
            sb.append(seconds).append("s ");
        }

        return sb.toString().trim();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.DIAMOND_ORE) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        int currentCount = diamondCounts.merge(uuid, 1, Integer::sum);
        if (currentCount > DIAMOND_CAP) {
            diamondCounts.put(uuid, DIAMOND_CAP);
        }

        Integer existingTaskId = scheduledTaskIds.get(uuid);
        if (existingTaskId != null) {
            getServer().getScheduler().cancelTask(existingTaskId);
        }

        int taskId = getServer().getScheduler().runTaskLater(this, () -> {
            Integer count = diamondCounts.remove(uuid);
            scheduledTaskIds.remove(uuid);

            if (count == null || count == 0) {
                return;
            }

            String description;
            if (count >= DIAMOND_CAP) {
                description = player.getName() + " mined 10+ diamonds";
            } else {
                description = player.getName() + " mined " + count + " diamonds";
            }

            String avatarUrl = getAvatarUrl(uuid);
            String footer = String.format("Level %d • XP: %.2f", player.getLevel(), player.getExp());

            WebhookPayload payload = buildEmbed("Diamonds Found", description,
                    COLOR_DIAMOND, avatarUrl, footer);

            webhookService.sendMessage(payload)
                    .thenAccept(statusCode -> getLogger().info("Diamond webhook response: " + statusCode));
        }, DIAMOND_DELAY_TICKS).getTaskId();

        scheduledTaskIds.put(uuid, taskId);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEndermanKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Enderman)) {
            return;
        }

        Player player = event.getEntity().getKiller();
        if (player == null) {
            return;
        }

        String avatarUrl = getAvatarUrl(player.getUniqueId());
        String footer = String.format("Level %d • XP: %.2f", player.getLevel(), player.getExp());

        WebhookPayload payload = buildEmbed("Enderman Killed", player.getName() + " killed an enderman",
                COLOR_ENDERMAN, avatarUrl, footer);

        webhookService.sendMessage(payload)
                .thenAccept(statusCode -> getLogger().info("Enderman webhook response: " + statusCode));
    }
}
