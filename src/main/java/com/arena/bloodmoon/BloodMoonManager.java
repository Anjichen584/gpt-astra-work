package com.arena.bloodmoon;

import com.arena.bloodmoon.mob.EliteFactory;
import com.arena.bloodmoon.tier.Tier;
import com.arena.bloodmoon.util.Spawns;
import com.arena.bloodmoon.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * 血月状态机：触发判定、夜间循环、氛围、刷怪压力、黎明结算。
 */
public class BloodMoonManager {

    private static final EntityType[] PRESSURE_TYPES = {
            EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.SKELETON,
            EntityType.SPIDER, EntityType.HUSK, EntityType.CREEPER
    };

    private final BloodMoonPlugin plugin;
    private final Random random = new Random();

    private BossBar bar;
    private BukkitTask task;

    private boolean active;
    private Tier tier = Tier.CRIMSON;
    private boolean checkedTonight;
    private boolean bossSpawnedTonight;
    private boolean firstBloodClaimed;
    private int seconds;

    private final Set<UUID> participants = new HashSet<>();
    private final Set<UUID> fallen = new HashSet<>();

    public BloodMoonManager(BloodMoonPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ tick

    public void startTicking() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void tick() {
        World world = getPrimaryWorld();
        if (world == null) {
            return;
        }
        long time = world.getTime();

        if (!active) {
            if (time < 12000L) {
                checkedTonight = false;
            } else if (!checkedTonight && time >= 12950L) {
                checkedTonight = true;
                if (shouldTrigger(world)) {
                    begin(rollTier(world), world, true);
                }
            }
            return;
        }

        // 已激活
        if (time < 12000L) {
            end(true);
            return;
        }

        seconds++;
        double progress = 1.0 - Math.min(1.0, Math.max(0.0, (time - 13000.0) / 10700.0));
        if (bar != null) {
            bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
            for (Player p : world.getPlayers()) {
                if (!bar.getPlayers().contains(p)) {
                    bar.addPlayer(p);
                }
            }
        }

        ambience(world);

        if (plugin.getConfig().getBoolean("mobs.spawn-pressure", true) && seconds % 30 == 0) {
            spawnPressure(world);
        }

        if (!bossSpawnedTonight
                && plugin.getConfig().getBoolean("boss.enabled", true)
                && tier.level() >= plugin.getConfig().getInt("boss.min-tier", 4)
                && time >= 17500L && time <= 18600L) {
            bossSpawnedTonight = true;
            plugin.getBossManager().spawnNearRandomPlayer(world, tier);
        }
    }

    // ------------------------------------------------------------- 触发判定

    private boolean shouldTrigger(World world) {
        boolean fullMoon = isFullMoon(world);
        if (plugin.getConfig().getBoolean("trigger.only-full-moon", false) && !fullMoon) {
            return false;
        }
        double chance = plugin.getConfig().getDouble("trigger.chance", 0.25);
        if (fullMoon) {
            chance += plugin.getConfig().getDouble("trigger.full-moon-bonus", 0.35);
        }
        return random.nextDouble() < chance;
    }

    public boolean isFullMoon(World world) {
        return (world.getFullTime() / 24000L) % 8L == 0L;
    }

    private Tier rollTier(World world) {
        List<Integer> weights = plugin.getConfig().getIntegerList("tiers.weights");
        if (weights.size() < 5) {
            weights = List.of(30, 28, 22, 14, 6);
        }
        int total = 0;
        for (int i = 0; i < 5; i++) {
            total += Math.max(0, weights.get(i));
        }
        int roll = random.nextInt(Math.max(1, total));
        int level = 1;
        for (int i = 0; i < 5; i++) {
            roll -= Math.max(0, weights.get(i));
            if (roll < 0) {
                level = i + 1;
                break;
            }
        }
        // 满月推波助澜：35% 概率阶层 +1
        if (isFullMoon(world) && level < 5 && random.nextDouble() < 0.35) {
            level++;
        }
        return Tier.ofLevel(level);
    }

    // ------------------------------------------------------------- 开始/结束

    public void begin(Tier newTier, World world, boolean natural) {
        this.active = true;
        this.tier = newTier;
        this.bossSpawnedTonight = false;
        this.firstBloodClaimed = false;
        this.seconds = 0;
        this.fallen.clear();
        this.participants.clear();
        for (Player p : world.getPlayers()) {
            participants.add(p.getUniqueId());
        }

        int count = plugin.getStatsManager().incrementMoonCount();

        bar = Bukkit.createBossBar(
                "\u00a74\u00a7l\u2620 血月 \u00b7 " + tier.display() + " \u00a78(第 " + count + " 次降临)",
                BarColor.RED, BarStyle.SEGMENTED_10);
        for (Player p : world.getPlayers()) {
            bar.addPlayer(p);
        }

        if (plugin.getConfig().getBoolean("night.storm", true)) {
            world.setStorm(true);
            world.setThundering(true);
        }

        Text.broadcast(world, Component.empty());
        Text.broadcast(world, Component.text("\u2620 ================================ \u2620", NamedTextColor.DARK_RED));
        Text.broadcast(world, Text.msg("血月升起 —— " + tier.display() + " (第 " + tier.level() + " 阶) 降临此世!", NamedTextColor.RED));
        Text.broadcast(world, Text.msg("魔物被血光浸染, 变得强大而狂暴...", NamedTextColor.GRAY));
        Text.broadcast(world, Text.msg("击杀魔物掉落 [血月残晶], 输入 /bm shop 打开血月黑市。", NamedTextColor.GOLD));
        if (tier.level() >= plugin.getConfig().getInt("boss.min-tier", 4)) {
            Text.broadcast(world, Text.msg("阶层足够高... 血月领主今夜可能亲临。", NamedTextColor.DARK_PURPLE));
        }
        Text.broadcast(world, Component.text("\u2620 ================================ \u2620", NamedTextColor.DARK_RED));
        Text.broadcast(world, Component.empty());

        for (Player p : world.getPlayers()) {
            Text.title(p, "\u2620 血月升起 \u2620", NamedTextColor.DARK_RED, tier.display() + " · 活到黎明", NamedTextColor.RED);
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.9f, 0.6f);
            p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 0.5f);
        }

        plugin.getEventDirector().start(world, tier);
    }

    public void end(boolean dawn) {
        if (!active) {
            return;
        }
        active = false;
        World world = getPrimaryWorld();

        plugin.getEventDirector().stop();
        plugin.getBossManager().despawn();
        plugin.getCombatListener().resetStreaks();

        if (bar != null) {
            bar.removeAll();
            bar = null;
        }

        if (world != null) {
            world.setStorm(false);
            world.setThundering(false);

            if (plugin.getConfig().getBoolean("night.purge-elites-at-dawn", true)) {
                purgeElites(world);
            }

            if (dawn) {
                int base = plugin.getConfig().getInt("rewards.dawn-shards-per-tier", 3) * tier.level();
                int bonus = plugin.getConfig().getInt("rewards.survivor-bonus", 4);
                for (UUID id : participants) {
                    Player p = Bukkit.getPlayer(id);
                    if (p == null || !p.isOnline() || !p.getWorld().equals(world)) {
                        continue;
                    }
                    boolean survived = !fallen.contains(id);
                    int amount = base + (survived ? bonus : 0);
                    giveShards(p, amount);
                    if (survived) {
                        plugin.getStatsManager().addNightSurvived(id);
                        Text.title(p, "黎明将至", NamedTextColor.GOLD, "你在血月下幸存 · +" + amount + " 血月残晶", NamedTextColor.YELLOW);
                    } else {
                        Text.title(p, "黎明将至", NamedTextColor.GOLD, "虽死犹战 · +" + amount + " 血月残晶", NamedTextColor.GRAY);
                    }
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                }
                Text.broadcast(world, Text.msg("血月退散, 大地重归寂静... 幸存者获得了血月的馈赠。", NamedTextColor.GOLD));
            } else {
                Text.broadcast(world, Text.msg("血月被强行驱散了。", NamedTextColor.GRAY));
            }
        }

        participants.clear();
        fallen.clear();
        plugin.getStatsManager().save();
    }

    private void purgeElites(World world) {
        for (UUID id : new ArrayList<>(EliteFactory.registry().keySet())) {
            Entity e = Bukkit.getEntity(id);
            if (e instanceof LivingEntity living && living.isValid() && living.getWorld().equals(world)) {
                world.spawnParticle(Particle.POOF, living.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.02);
                living.remove();
            }
            EliteFactory.registry().remove(id);
        }
    }

    public void giveShards(Player p, int amount) {
        if (amount <= 0) {
            return;
        }
        var leftover = p.getInventory().addItem(com.arena.bloodmoon.item.Items.shard(amount));
        for (var stack : leftover.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), stack);
        }
        plugin.getStatsManager().addShards(p.getUniqueId(), amount);
    }

    // ------------------------------------------------------------- 氛围/压力

    private void ambience(World world) {
        for (Player p : world.getPlayers()) {
            // 环绕血雾粒子
            if (seconds % 2 == 0) {
                Location base = p.getLocation().add(0, 1, 0);
                for (int i = 0; i < 6; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double r = 6 + random.nextDouble() * 6;
                    Location loc = base.clone().add(Math.cos(angle) * r, random.nextDouble() * 3 - 1, Math.sin(angle) * r);
                    world.spawnParticle(Particle.DUST, loc, 1,
                            new Particle.DustOptions(Color.fromRGB(140, 0, 10), 1.6f));
                }
            }
            // 诡异环境音
            if (seconds % 25 == 0 && random.nextDouble() < 0.6) {
                Sound s = random.nextBoolean() ? Sound.ENTITY_WARDEN_HEARTBEAT : Sound.AMBIENT_CAVE;
                p.playSound(p.getLocation(), s, 0.5f, random.nextBoolean() ? 0.6f : 0.8f);
            }
        }
    }

    private void spawnPressure(World world) {
        List<Player> players = world.getPlayers();
        if (players.isEmpty()) {
            return;
        }
        int cap = plugin.getConfig().getInt("mobs.pressure-cap", 24);
        for (Player p : players) {
            if (random.nextDouble() > 0.4) {
                continue;
            }
            int nearby = 0;
            for (Entity e : p.getNearbyEntities(28, 16, 28)) {
                if (e instanceof Enemy) {
                    nearby++;
                }
            }
            if (nearby >= cap) {
                continue;
            }
            int packSize = 2 + tier.level();
            for (int i = 0; i < packSize; i++) {
                Location spot = Spawns.findSpot(p.getLocation(), random, 12, 22);
                if (spot != null) {
                    EliteFactory.spawnBuffedBasic(PRESSURE_TYPES[random.nextInt(PRESSURE_TYPES.length)], spot, tier);
                }
            }
        }
    }

    // ------------------------------------------------------------- 命令入口

    public void forceStart(Tier forceTier) {
        World world = getPrimaryWorld();
        if (world == null) {
            return;
        }
        if (active) {
            end(false);
        }
        long time = world.getTime();
        if (time < 12500L || time > 23000L) {
            world.setTime(13500L);
        }
        checkedTonight = true;
        begin(forceTier, world, false);
    }

    public void forceStop() {
        end(false);
    }

    // ------------------------------------------------------------- 查询/工具

    public boolean isActive() {
        return active;
    }

    public Tier getTier() {
        return tier;
    }

    public boolean isBloodMoonWorld(World world) {
        if (world == null) {
            return false;
        }
        List<String> worlds = plugin.getConfig().getStringList("worlds");
        return worlds.isEmpty() ? world.getEnvironment() == World.Environment.NORMAL : worlds.contains(world.getName());
    }

    public World getPrimaryWorld() {
        List<String> worlds = plugin.getConfig().getStringList("worlds");
        if (!worlds.isEmpty()) {
            World w = Bukkit.getWorld(worlds.get(0));
            if (w != null) {
                return w;
            }
        }
        for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() == World.Environment.NORMAL) {
                return w;
            }
        }
        return null;
    }

    public void markFallen(UUID id) {
        fallen.add(id);
    }

    public void addParticipant(UUID id) {
        if (active) {
            participants.add(id);
        }
    }

    public boolean claimFirstBlood() {
        if (!firstBloodClaimed) {
            firstBloodClaimed = true;
            return true;
        }
        return false;
    }

    public BossBar getBar() {
        return bar;
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
        if (active) {
            // 停服时静默收尾, 不发奖励
            plugin.getEventDirector().stop();
            plugin.getBossManager().despawn();
            if (bar != null) {
                bar.removeAll();
                bar = null;
            }
            active = false;
        }
    }
}
