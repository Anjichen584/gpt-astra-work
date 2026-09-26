package com.arena.bloodmoon.boss;

import com.arena.bloodmoon.BloodMoonPlugin;
import com.arena.bloodmoon.config.Cfg;
import com.arena.bloodmoon.item.Items;
import com.arena.bloodmoon.mob.EliteFactory;
import com.arena.bloodmoon.mob.MobBuffer;
import com.arena.bloodmoon.mob.MobDisplay;
import com.arena.bloodmoon.tier.Tier;
import com.arena.bloodmoon.util.Spawns;
import com.arena.bloodmoon.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.UUID;

/**
 * 血月领主 —— 多阶段 Boss。
 */
public class BossManager implements Listener {

    private final BloodMoonPlugin plugin;
    private final Random random = new Random();
    private final NamespacedKey bossKey;

    private UUID bossId;
    private BossBar bossBar;
    private BukkitTask task;
    private boolean phase2Announced;
    private boolean phase3Announced;

    public BossManager(BloodMoonPlugin plugin) {
        this.plugin = plugin;
        this.bossKey = new NamespacedKey(plugin, "bm_boss");
    }

    public boolean isBoss(Entity entity) {
        return entity.getPersistentDataContainer().has(bossKey, PersistentDataType.BYTE);
    }

    public boolean isAlive() {
        if (bossId == null) {
            return false;
        }
        Entity e = Bukkit.getEntity(bossId);
        return e instanceof LivingEntity living && living.isValid() && !living.isDead();
    }

    // ------------------------------------------------------------- 召唤

    public void spawnNearRandomPlayer(World world, Tier tier) {
        if (world.getPlayers().isEmpty() || isAlive()) {
            return;
        }
        Player target = world.getPlayers().get(random.nextInt(world.getPlayers().size()));
        Location spot = Spawns.findSpot(target.getLocation(), random, 12, 20);
        if (spot == null) {
            spot = target.getLocation();
        }
        spawn(spot, tier);
    }

    public void spawn(Location loc, Tier tier) {
        World world = loc.getWorld();
        if (world == null || isAlive()) {
            return;
        }
        phase2Announced = false;
        phase3Announced = false;

        double health = plugin.getConfig().getDouble("boss.health", 600.0)
                * (1.0 + 0.25 * (tier.level() - 1));

        WitherSkeleton boss = world.spawn(loc, WitherSkeleton.class, ws -> {
            set(ws, Attribute.SCALE, 2.4);
            set(ws, Attribute.MAX_HEALTH, health);
            set(ws, Attribute.KNOCKBACK_RESISTANCE, 1.0);
            set(ws, Attribute.ATTACK_DAMAGE, 14.0);
            set(ws, Attribute.MOVEMENT_SPEED, 0.32);
            set(ws, Attribute.FOLLOW_RANGE, 64.0);
            set(ws, Attribute.ARMOR, 12.0);
            ws.setHealth(health);

            EntityEquipment eq = ws.getEquipment();
            ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
            sword.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
            sword.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 2);
            eq.setItemInMainHand(sword);
            eq.setItemInMainHandDropChance(0f);
            eq.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
            eq.setHelmetDropChance(0f);

            // 领主名称 / 高亮 / 显示范围 同样由 config 统一管理
            MobDisplay.apply(ws, Cfg.bossDisplayName(), Cfg.nameBossFormat(), Cfg.nameBoss(), Cfg.glowBoss());
            ws.setPersistent(true);
            ws.setRemoveWhenFarAway(false);
            ws.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, true, false));
            ws.getPersistentDataContainer().set(bossKey, PersistentDataType.BYTE, (byte) 1);
            MobBuffer.mark(ws);
        });

        bossId = boss.getUniqueId();

        bossBar = Bukkit.createBossBar("\u00a74\u00a7l\u2620 " + Cfg.bossDisplayName() + " \u2620",
                BarColor.PURPLE, BarStyle.SEGMENTED_20);
        for (Player p : world.getPlayers()) {
            bossBar.addPlayer(p);
        }

        world.strikeLightningEffect(loc);
        world.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 2f, 0.5f);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc.clone().add(0, 1, 0), 3);
        Text.broadcast(world, Component.empty());
        Text.broadcast(world, Text.msg("\u2620 血月领主 · 卡尔诺斯 已然降临! 大地在他的脚下颤抖...", NamedTextColor.DARK_PURPLE));
        Text.broadcast(world, Text.msg("将他斩杀, 血月的宝藏就是你们的!", NamedTextColor.GOLD));
        Text.broadcast(world, Component.empty());
        for (Player p : world.getPlayers()) {
            Text.title(p, "\u2620 血月领主降临 \u2620", NamedTextColor.DARK_PURPLE, "卡尔诺斯 · 血月的行刑者", NamedTextColor.RED);
        }

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::bossTick, 50L, 50L);
    }

    private void set(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance inst = entity.getAttribute(attribute);
        if (inst != null) {
            inst.setBaseValue(value);
        }
    }

    // ------------------------------------------------------------- Boss AI

    private void bossTick() {
        Entity entity = bossId == null ? null : Bukkit.getEntity(bossId);
        if (!(entity instanceof WitherSkeleton boss) || !boss.isValid() || boss.isDead()) {
            cleanup();
            return;
        }
        World world = boss.getWorld();
        AttributeInstance maxHp = boss.getAttribute(Attribute.MAX_HEALTH);
        double max = maxHp != null ? maxHp.getValue() : 600.0;
        double pct = boss.getHealth() / max;

        if (bossBar != null) {
            bossBar.setProgress(Math.max(0.0, Math.min(1.0, pct)));
            for (Player p : world.getPlayers()) {
                if (!bossBar.getPlayers().contains(p)) {
                    bossBar.addPlayer(p);
                }
            }
        }

        // 阶段转换
        if (!phase2Announced && pct <= 0.66) {
            phase2Announced = true;
            boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0, true, false));
            Text.broadcast(world, Text.msg("卡尔诺斯: \"血液...沸腾了!\" (领主进入狂暴)", NamedTextColor.RED));
            world.playSound(boss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 2f, 0.5f);
        }
        if (!phase3Announced && pct <= 0.33) {
            phase3Announced = true;
            boss.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 0, true, false));
            Text.broadcast(world, Text.msg("卡尔诺斯: \"见识血月真正的怒火吧!\" (领主进入最终形态)", NamedTextColor.DARK_RED));
            world.playSound(boss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 2f, 0.4f);
        }

        // 技能轮盘
        int roll = random.nextInt(100);
        if (roll < 30) {
            shockwave(boss);
        } else if (roll < 55) {
            skullVolley(boss);
        } else if (roll < 75) {
            summonMinions(boss);
        } else if (roll < 90 && pct < 0.9) {
            lifeDrain(boss);
        }
    }

    private void shockwave(WitherSkeleton boss) {
        boolean hit = false;
        for (Entity e : boss.getNearbyEntities(6, 4, 6)) {
            if (e instanceof Player p && !p.isDead()) {
                Vector away = p.getLocation().toVector().subtract(boss.getLocation().toVector());
                if (away.lengthSquared() < 0.01) {
                    away = new Vector(0, 0.2, 0);
                }
                p.setVelocity(away.normalize().multiply(1.3).setY(0.9));
                p.damage(6.0, boss);
                hit = true;
            }
        }
        if (hit) {
            boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
            boss.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, boss.getLocation(), 2);
        }
    }

    private void skullVolley(WitherSkeleton boss) {
        World world = boss.getWorld();
        int fired = 0;
        for (Player p : world.getPlayers()) {
            if (fired >= 3 || p.isDead()) {
                continue;
            }
            double dist = p.getLocation().distanceSquared(boss.getLocation());
            if (dist > 30 * 30 || dist < 4) {
                continue;
            }
            Vector dir = p.getEyeLocation().toVector()
                    .subtract(boss.getEyeLocation().toVector()).normalize();
            Location from = boss.getEyeLocation().add(dir.clone().multiply(1.5));
            world.spawn(from, WitherSkull.class, skull -> {
                skull.setShooter(boss);
                skull.setDirection(dir);
                MobBuffer.mark(skull);
            });
            fired++;
        }
        if (fired > 0) {
            world.playSound(boss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.7f);
        }
    }

    private void summonMinions(WitherSkeleton boss) {
        World world = boss.getWorld();
        Tier tier = plugin.getMoonManager().getTier();
        int count = 2 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            Location spot = Spawns.findSpot(boss.getLocation(), random, 3, 7);
            if (spot == null) {
                continue;
            }
            if (random.nextDouble() < 0.25) {
                EliteFactory.spawnElite(EliteFactory.EliteType.ASH_WALKER, spot, tier);
            } else {
                EliteFactory.spawnBuffedBasic(random.nextBoolean()
                        ? EntityType.WITHER_SKELETON : EntityType.ZOMBIE, spot, tier);
            }
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, spot, 15, 0.3, 0.5, 0.3, 0.02);
        }
        world.playSound(boss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.5f, 0.6f);
        Text.broadcast(world, Text.msg("卡尔诺斯从血月中拽出了他的仆从!", NamedTextColor.GRAY));
    }

    private void lifeDrain(WitherSkeleton boss) {
        World world = boss.getWorld();
        double drained = 0;
        for (Entity e : boss.getNearbyEntities(8, 5, 8)) {
            if (e instanceof Player p && !p.isDead()) {
                p.damage(4.0, boss);
                p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0, true, true));
                drained += 8.0;
                world.spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 20, 0.3, 0.6, 0.3,
                        new Particle.DustOptions(Color.fromRGB(160, 0, 20), 1.6f));
            }
        }
        if (drained > 0) {
            AttributeInstance maxHp = boss.getAttribute(Attribute.MAX_HEALTH);
            double cap = maxHp != null ? maxHp.getValue() : boss.getHealth();
            boss.setHealth(Math.min(cap, boss.getHealth() + drained));
            world.playSound(boss.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.5f, 0.5f);
        }
    }

    // ------------------------------------------------------------- 死亡/清理

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        if (bossId == null || !event.getEntity().getUniqueId().equals(bossId)) {
            return;
        }
        LivingEntity boss = event.getEntity();
        World world = boss.getWorld();
        Location loc = boss.getLocation();
        Player killer = boss.getKiller();

        event.getDrops().clear();
        event.setDroppedExp(500);

        // 战利品雨
        world.dropItemNaturally(loc, Items.shard(Math.min(64, 16 + 8 * plugin.getMoonManager().getTier().level())));
        world.dropItemNaturally(loc, new ItemStack(Material.DIAMOND, 4 + random.nextInt(5)));
        world.dropItemNaturally(loc, new ItemStack(Material.NETHERITE_SCRAP, 1 + random.nextInt(2)));
        world.dropItemNaturally(loc, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
        // 必掉一件血月神器
        String[] artifacts = {Items.ID_BLADE, Items.ID_BOW, Items.ID_CHARM, Items.ID_TOTEM};
        world.dropItemNaturally(loc, Items.byId(artifacts[random.nextInt(artifacts.length)]));

        for (int i = 0; i < 3; i++) {
            Location fwLoc = loc.clone().add(random.nextDouble() * 4 - 2, 1, random.nextDouble() * 4 - 2);
            world.spawn(fwLoc, Firework.class, fw -> {
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .withColor(Color.RED, Color.MAROON)
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .trail(true)
                        .build());
                meta.setPower(1);
                fw.setFireworkMeta(meta);
            });
        }

        world.playSound(loc, Sound.ENTITY_WITHER_DEATH, 2f, 0.6f);
        Text.broadcast(world, Component.empty());
        if (killer != null) {
            plugin.getStatsManager().addBossKill(killer.getUniqueId());
            Text.broadcast(world, Text.msg("\u2620 " + killer.getName() + " 斩杀了血月领主 · 卡尔诺斯! 血月的宝藏倾泻而出!", NamedTextColor.GOLD));
        } else {
            Text.broadcast(world, Text.msg("\u2620 血月领主 · 卡尔诺斯 陨落了!", NamedTextColor.GOLD));
        }
        Text.broadcast(world, Component.empty());

        cleanup();
    }

    public void despawn() {
        if (bossId != null) {
            Entity e = Bukkit.getEntity(bossId);
            if (e != null && e.isValid()) {
                e.getWorld().spawnParticle(Particle.POOF, e.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.05);
                e.remove();
            }
        }
        cleanup();
    }

    private void cleanup() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        bossId = null;
    }
}
