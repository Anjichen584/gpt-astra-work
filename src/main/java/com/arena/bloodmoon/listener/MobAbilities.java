package com.arena.bloodmoon.listener;

import com.arena.bloodmoon.BloodMoonPlugin;
import com.arena.bloodmoon.mob.EliteFactory;
import com.arena.bloodmoon.mob.EliteFactory.EliteType;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 精英魔物的主动技能 AI（周期驱动）+ 触发型技能。
 */
public class MobAbilities implements Listener {

    private final BloodMoonPlugin plugin;
    private final Random random = new Random();
    private BukkitTask task;

    public MobAbilities(BloodMoonPlugin plugin) {
        this.plugin = plugin;
    }

    public void startTicking() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 40L);
    }

    private void tick() {
        for (Map.Entry<UUID, EliteType> entry : EliteFactory.registry().entrySet()) {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living) || !living.isValid() || living.isDead()) {
                EliteFactory.registry().remove(entry.getKey());
                continue;
            }
            // 头顶血色微粒, 提示这是精英
            living.getWorld().spawnParticle(Particle.DUST,
                    living.getLocation().add(0, living.getHeight() + 0.4, 0), 3, 0.15, 0.1, 0.15,
                    new Particle.DustOptions(Color.fromRGB(180, 0, 20), 1.2f));

            if (!(living instanceof Mob mob)) {
                continue;
            }
            switch (entry.getValue()) {
                case BUTCHER -> butcherLeap(mob);
                case HUNTRESS -> huntressBlink(mob);
                case PLAGUEBEARER -> plagueCloud(mob);
                case CRUSHER -> crusherRoar(mob);
                case ASH_WALKER -> ashIgnite(mob);
                case TIDE_WRAITH -> tideDash(mob);
                case SOUL_RIPPER -> ripperShriek(mob);
                default -> {
                }
            }
        }
    }

    // ------------------------------------------------------------- 主动技能

    private LivingEntity targetOf(Mob mob, double maxDist) {
        LivingEntity target = mob.getTarget();
        if (target instanceof Player p && !p.isDead()
                && p.getWorld().equals(mob.getWorld())
                && p.getLocation().distanceSquared(mob.getLocation()) <= maxDist * maxDist) {
            return target;
        }
        return null;
    }

    private void butcherLeap(Mob mob) {
        LivingEntity target = targetOf(mob, 10);
        if (target == null || random.nextDouble() > 0.5) {
            return;
        }
        Vector dir = target.getLocation().toVector().subtract(mob.getLocation().toVector());
        if (dir.lengthSquared() < 4) {
            return;
        }
        mob.setVelocity(dir.normalize().multiply(1.15).setY(0.5));
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_HOGLIN_ANGRY, 1f, 0.6f);
        mob.getWorld().spawnParticle(Particle.CRIMSON_SPORE, mob.getLocation(), 15, 0.3, 0.2, 0.3, 0.01);
    }

    private void huntressBlink(Mob mob) {
        LivingEntity target = targetOf(mob, 24);
        if (target == null || random.nextDouble() > 0.3) {
            return;
        }
        Location dest = target.getLocation().clone().add(
                (random.nextDouble() - 0.5) * 8, 0, (random.nextDouble() - 0.5) * 8);
        dest.setY(target.getLocation().getY());
        mob.getWorld().spawnParticle(Particle.PORTAL, mob.getLocation().add(0, 1, 0), 30, 0.3, 0.6, 0.3, 0.5);
        mob.teleport(dest);
        mob.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.7f);
    }

    private void plagueCloud(Mob mob) {
        LivingEntity target = targetOf(mob, 16);
        if (target == null || random.nextDouble() > 0.35) {
            return;
        }
        Location loc = target.getLocation();
        mob.getWorld().spawn(loc, AreaEffectCloud.class, cloud -> {
            cloud.setRadius(3.0f);
            cloud.setDuration(100);
            cloud.setRadiusOnUse(-0.2f);
            cloud.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 80, 1), true);
            cloud.setParticle(Particle.DUST, new Particle.DustOptions(Color.fromRGB(70, 130, 20), 1.5f));
        });
        mob.getWorld().playSound(loc, Sound.ENTITY_WITCH_THROW, 1f, 0.6f);
    }

    private void crusherRoar(Mob mob) {
        boolean roared = false;
        for (Entity e : mob.getNearbyEntities(4.5, 3, 4.5)) {
            if (e instanceof Player p && !p.isDead()) {
                Vector away = p.getLocation().toVector().subtract(mob.getLocation().toVector());
                if (away.lengthSquared() < 0.01) {
                    away = new Vector(0, 0.1, 0);
                }
                p.setVelocity(away.normalize().multiply(1.4).setY(0.6));
                p.damage(5.0, mob);
                roared = true;
            }
        }
        if (roared) {
            mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.2f, 0.8f);
            mob.getWorld().spawnParticle(Particle.EXPLOSION, mob.getLocation().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0);
        }
    }

    private void ashIgnite(Mob mob) {
        LivingEntity target = targetOf(mob, 3.5);
        if (target != null) {
            target.setFireTicks(Math.max(target.getFireTicks(), 60));
            mob.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.02);
        }
    }

    private void tideDash(Mob mob) {
        LivingEntity target = targetOf(mob, 12);
        if (target == null || random.nextDouble() > 0.35) {
            return;
        }
        Vector dir = target.getLocation().toVector().subtract(mob.getLocation().toVector());
        if (dir.lengthSquared() < 1) {
            return;
        }
        mob.setVelocity(dir.normalize().multiply(1.3).setY(0.25));
        mob.getWorld().playSound(mob.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 0.8f);
    }

    private void ripperShriek(Mob mob) {
        if (random.nextDouble() > 0.25) {
            return;
        }
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, 1.4f, 0.6f);
        mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, true, false));
    }

    // ------------------------------------------------------------- 触发技能

    @EventHandler(ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        EliteType type = EliteFactory.getType(event.getEntity());
        if (type == null) {
            return;
        }
        if (event.getProjectile() instanceof AbstractArrow arrow) {
            if (type == EliteType.HUNTRESS || type == EliteType.BONE_RIDER) {
                arrow.setFireTicks(100);
                arrow.setVelocity(arrow.getVelocity().multiply(1.25));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        EliteType type = EliteFactory.getType(event.getDamager());
        if (type == null) {
            return;
        }
        switch (type) {
            case BUTCHER -> {
                // 撕裂: 短暂凋零 + 额外击退
                victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0, true, true));
                Vector kb = victim.getLocation().toVector().subtract(event.getDamager().getLocation().toVector());
                if (kb.lengthSquared() > 0.01) {
                    victim.setVelocity(victim.getVelocity().add(kb.normalize().multiply(0.6).setY(0.3)));
                }
            }
            case ASH_WALKER -> victim.setFireTicks(Math.max(victim.getFireTicks(), 80));
            case JOCKEY -> victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, true, true));
            default -> {
            }
        }
    }

    @EventHandler
    public void onEliteDeath(EntityDeathEvent event) {
        EliteFactory.registry().remove(event.getEntity().getUniqueId());
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
    }
}
