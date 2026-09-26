package com.arena.bloodmoon.event;

import com.arena.bloodmoon.BloodMoonPlugin;
import com.arena.bloodmoon.item.Items;
import com.arena.bloodmoon.mob.EliteFactory;
import com.arena.bloodmoon.mob.MobBuffer;
import com.arena.bloodmoon.tier.Tier;
import com.arena.bloodmoon.util.Spawns;
import com.arena.bloodmoon.util.Text;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Bat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 血月随机灾厄事件导演：每隔一段时间随机降下一种事件。
 */
public class EventDirector implements Listener {

    private final BloodMoonPlugin plugin;
    private final Random random = new Random();

    private BukkitTask task;
    private World world;
    private Tier tier;

    public EventDirector(BloodMoonPlugin plugin) {
        this.plugin = plugin;
    }

    public void start(World world, Tier tier) {
        this.world = world;
        this.tier = tier;
        long interval = 20L * plugin.getConfig().getInt("events.interval-seconds", 45);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::roll, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    // ------------------------------------------------------------- 抽取事件

    private record Weighted(String key, int weight, Runnable action) {
    }

    private void roll() {
        if (world == null || world.getPlayers().isEmpty()) {
            return;
        }
        List<Weighted> pool = new ArrayList<>();
        addIfEnabled(pool, "meteor", 14, this::meteorShower);
        addIfEnabled(pool, "horde", 20, this::hordeSurge);
        addIfEnabled(pool, "fog", 14, this::bloodFog);
        addIfEnabled(pool, "thunder", 12, this::thunderWrath);
        addIfEnabled(pool, "blessing", 10, this::crimsonBlessing);
        addIfEnabled(pool, "bats", 8, this::batSwarm);
        addIfEnabled(pool, "supply", 10, this::supplyDrop);
        addIfEnabled(pool, "gravity", 12, this::gravityAnomaly);
        if (pool.isEmpty()) {
            return;
        }
        int total = pool.stream().mapToInt(Weighted::weight).sum();
        int pick = random.nextInt(total);
        for (Weighted w : pool) {
            pick -= w.weight();
            if (pick < 0) {
                w.action().run();
                return;
            }
        }
    }

    private void addIfEnabled(List<Weighted> pool, String key, int weight, Runnable action) {
        if (plugin.getConfig().getBoolean("events." + key, true)) {
            pool.add(new Weighted(key, weight, action));
        }
    }

    private Player randomPlayer() {
        List<Player> players = world.getPlayers();
        return players.isEmpty() ? null : players.get(random.nextInt(players.size()));
    }

    // ------------------------------------------------------------- 各类事件

    /** 陨星雨：火球从天而降。 */
    private void meteorShower() {
        Player target = randomPlayer();
        if (target == null) {
            return;
        }
        Text.broadcast(world, Text.msg("\u2604 陨星雨! 血色的火球正撕裂夜空...", NamedTextColor.RED));
        world.playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 0.5f);
        int count = 6 + tier.level() * 2;
        for (int i = 0; i < count; i++) {
            int delay = i * 8;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!target.isOnline()) {
                    return;
                }
                Location base = target.getLocation();
                Location spawn = base.clone().add(
                        (random.nextDouble() - 0.5) * 30, 25 + random.nextInt(8), (random.nextDouble() - 0.5) * 30);
                world.spawn(spawn, Fireball.class, ball -> {
                    ball.setDirection(new Vector((random.nextDouble() - 0.5) * 0.2, -1, (random.nextDouble() - 0.5) * 0.2));
                    ball.setYield(2.0f);
                    ball.setIsIncendiary(plugin.getConfig().getBoolean("mobs.explosion-block-damage", false));
                    MobBuffer.mark(ball);
                });
            }, delay);
        }
    }

    /** 尸潮涌动：在随机玩家周围爆发一波强化怪。 */
    private void hordeSurge() {
        Player target = randomPlayer();
        if (target == null) {
            return;
        }
        Text.broadcast(world, Text.msg("\u2694 尸潮涌动! 大地之下传来无数抓挠声...", NamedTextColor.RED));
        world.playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 1.5f, 0.5f);
        int count = 5 + tier.level() * 2;
        for (int i = 0; i < count; i++) {
            Location spot = Spawns.findSpot(target.getLocation(), random, 8, 16);
            if (spot == null) {
                continue;
            }
            double roll = random.nextDouble();
            EntityType type = roll < 0.5 ? EntityType.ZOMBIE : roll < 0.8 ? EntityType.SKELETON : EntityType.SPIDER;
            EliteFactory.spawnBuffedBasic(type, spot, tier);
            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, spot, 8, 0.3, 0.5, 0.3, 0.01);
        }
    }

    /** 血雾弥漫：全员黑暗 + 缓慢。 */
    private void bloodFog() {
        Text.broadcast(world, Text.msg("\u2601 血雾弥漫... 你的视野被猩红吞噬。", NamedTextColor.DARK_RED));
        for (Player p : world.getPlayers()) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 200, 0, true, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 0, true, false));
            p.playSound(p.getLocation(), Sound.AMBIENT_BASALT_DELTAS_MOOD, 1f, 0.5f);
            world.spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 60, 4, 2, 4,
                    new Particle.DustOptions(Color.fromRGB(100, 0, 10), 2.0f));
        }
    }

    /** 猩红雷罚：随机玩家附近落雷。 */
    private void thunderWrath() {
        Text.broadcast(world, Text.msg("\u26a1 猩红雷罚! 血月的怒意化作雷霆...", NamedTextColor.RED));
        List<Player> players = world.getPlayers();
        int strikes = Math.min(players.size(), 3);
        for (int i = 0; i < strikes; i++) {
            Player p = players.get(random.nextInt(players.size()));
            Location near = p.getLocation().clone().add(
                    (random.nextDouble() - 0.5) * 8, 0, (random.nextDouble() - 0.5) * 8);
            world.strikeLightning(near);
        }
    }

    /** 血月赐福（正面事件）。 */
    private void crimsonBlessing() {
        Text.broadcast(world, Text.msg("\u2764 血月低语... 一股诡异的力量注入你的血脉。", NamedTextColor.LIGHT_PURPLE));
        for (Player p : world.getPlayers()) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 160, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 600, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 300, 0));
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.4f);
            world.spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 1.5, 0), 40, 0.6, 0.8, 0.6, 0.5);
        }
    }

    /** 蝠群蔽月：纯氛围恐吓。 */
    private void batSwarm() {
        Player target = randomPlayer();
        if (target == null) {
            return;
        }
        Text.broadcast(world, Text.msg("\ud83e\udd87 蝠群蔽月! 无数黑影掠过头顶...", NamedTextColor.GRAY));
        List<Bat> bats = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            Location loc = target.getLocation().clone().add(
                    (random.nextDouble() - 0.5) * 10, 3 + random.nextDouble() * 4, (random.nextDouble() - 0.5) * 10);
            bats.add(world.spawn(loc, Bat.class));
        }
        world.playSound(target.getLocation(), Sound.ENTITY_BAT_AMBIENT, 2f, 0.7f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Bat bat : bats) {
                if (bat.isValid()) {
                    world.spawnParticle(Particle.SMOKE, bat.getLocation(), 3, 0.1, 0.1, 0.1, 0.01);
                    bat.remove();
                }
            }
        }, 600L);
    }

    /** 空投血匣：限时宝箱 + 精英守卫。 */
    private void supplyDrop() {
        Player target = randomPlayer();
        if (target == null) {
            return;
        }
        Location spot = Spawns.findSpot(target.getLocation(), random, 10, 20);
        if (spot == null) {
            return;
        }
        Block block = spot.getBlock();
        if (!block.getType().isAir()) {
            return;
        }
        block.setType(Material.CHEST);
        if (block.getState() instanceof Chest chest) {
            chest.getBlockInventory().addItem(Items.shard(4 + random.nextInt(5)));
            chest.getBlockInventory().addItem(new org.bukkit.inventory.ItemStack(Material.GOLDEN_APPLE, 2));
            chest.getBlockInventory().addItem(new org.bukkit.inventory.ItemStack(Material.ARROW, 32));
            chest.getBlockInventory().addItem(new org.bukkit.inventory.ItemStack(Material.EXPERIENCE_BOTTLE, 8));
            if (random.nextDouble() < 0.3) {
                chest.getBlockInventory().addItem(Items.clot(2));
            }
            if (random.nextDouble() < 0.15) {
                chest.getBlockInventory().addItem(Items.horn());
            }
        }
        world.spawnParticle(Particle.END_ROD, spot.clone().add(0.5, 1, 0.5), 40, 0.3, 1.5, 0.3, 0.05);
        world.playSound(spot, Sound.BLOCK_ANVIL_LAND, 1f, 0.6f);
        Text.broadcast(world, Text.msg("\ud83c\udf81 空投血匣坠落于 [" + spot.getBlockX() + ", "
                + spot.getBlockY() + ", " + spot.getBlockZ() + "] — 90 秒后消失, 小心守卫!", NamedTextColor.GOLD));

        // 守卫
        for (int i = 0; i < 2; i++) {
            Location guardSpot = Spawns.findSpot(spot, random, 2, 4);
            if (guardSpot != null) {
                EliteFactory.spawnElite(EliteFactory.EliteType.random(random), guardSpot, tier);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (block.getType() == Material.CHEST) {
                block.setType(Material.AIR);
                world.spawnParticle(Particle.POOF, spot.clone().add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, 0.02);
            }
        }, 20L * 90);
    }

    /** 引力紊乱：周围魔物短暂漂浮。 */
    private void gravityAnomaly() {
        Text.broadcast(world, Text.msg("\ud83c\udf00 引力紊乱! 血月的引力扭曲了大地...", NamedTextColor.LIGHT_PURPLE));
        for (Player p : world.getPlayers()) {
            for (var e : p.getNearbyEntities(20, 10, 20)) {
                if (e instanceof Monster monster) {
                    monster.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 0, true, true));
                    world.spawnParticle(Particle.PORTAL, monster.getLocation(), 10, 0.3, 0.5, 0.3, 0.3);
                }
            }
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 120, 0, true, false));
        }
        world.playSound(world.getPlayers().get(0).getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 1f, 0.5f);
    }

    // ------------------------------------------------------------- 爆炸保护

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (plugin.getConfig().getBoolean("mobs.explosion-block-damage", false)) {
            return;
        }
        if (MobBuffer.isMarked(event.getEntity())) {
            event.blockList().clear();
        }
    }
}
