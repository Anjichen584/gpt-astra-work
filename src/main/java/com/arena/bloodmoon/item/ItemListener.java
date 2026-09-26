package com.arena.bloodmoon.item;

import com.arena.bloodmoon.BloodMoonManager;
import com.arena.bloodmoon.BloodMoonPlugin;
import com.arena.bloodmoon.util.Text;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 自定义物品的全部特效。
 */
public class ItemListener implements Listener {

    private static final Set<EntityType> UNDEAD = EnumSet.of(
            EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.HUSK, EntityType.DROWNED,
            EntityType.SKELETON, EntityType.STRAY, EntityType.BOGGED, EntityType.WITHER_SKELETON,
            EntityType.PHANTOM, EntityType.ZOGLIN, EntityType.ZOMBIFIED_PIGLIN, EntityType.WITHER,
            EntityType.SKELETON_HORSE, EntityType.ZOMBIE_HORSE);

    private final BloodMoonPlugin plugin;
    private final NamespacedKey arrowKey;
    private final Map<UUID, Long> hornCooldown = new HashMap<>();

    public ItemListener(BloodMoonPlugin plugin) {
        this.plugin = plugin;
        this.arrowKey = new NamespacedKey(plugin, "bm_arrow");

        // 守夜人护符被动: 每 5 秒刷新
        Bukkit.getScheduler().runTaskTimer(plugin, this::charmPassive, 100L, 100L);
    }

    private void charmPassive() {
        BloodMoonManager mgr = plugin.getMoonManager();
        if (!mgr.isActive()) {
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!mgr.isBloodMoonWorld(p.getWorld())) {
                continue;
            }
            if (hasItem(p, Items.ID_CHARM)) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 140, 0, true, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 140, 0, true, false));
            }
        }
    }

    private boolean hasItem(Player p, String id) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && Items.is(item, id)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------- 血月之刃

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBladeHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!Items.is(hand, Items.ID_BLADE)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        // 猎魔增伤
        if (plugin.getMoonManager().isActive()
                && plugin.getMoonManager().isBloodMoonWorld(player.getWorld())
                && UNDEAD.contains(victim.getType())) {
            event.setDamage(event.getDamage() * 1.3);
        }
        // 嗜血回复
        double heal = event.getFinalDamage() * 0.15;
        AttributeInstance maxHp = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHp != null && heal > 0) {
            player.setHealth(Math.min(maxHp.getValue(), player.getHealth() + heal));
        }
        victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 8,
                0.3, 0.4, 0.3, new Particle.DustOptions(Color.fromRGB(200, 0, 20), 1.3f));
    }

    // ------------------------------------------------------------- 猩红战弓

    @EventHandler(ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        ItemStack bow = event.getBow();
        if (bow == null || !Items.is(bow, Items.ID_BOW)) {
            return;
        }
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) {
            return;
        }
        arrow.getPersistentDataContainer().set(arrowKey, PersistentDataType.BYTE, (byte) 1);
        arrow.setVelocity(arrow.getVelocity().multiply(1.15));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isOnGround() || ticks++ > 60) {
                    cancel();
                    return;
                }
                arrow.getWorld().spawnParticle(Particle.DUST, arrow.getLocation(), 2,
                        0.05, 0.05, 0.05, new Particle.DustOptions(Color.fromRGB(180, 0, 20), 1.0f));
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onArrowHit(ProjectileHitEvent event) {
        if (!event.getEntity().getPersistentDataContainer().has(arrowKey, PersistentDataType.BYTE)) {
            return;
        }
        if (event.getHitEntity() instanceof LivingEntity victim && victim instanceof Enemy) {
            victim.setFireTicks(Math.max(victim.getFireTicks(), 80));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0, true, true));
            victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 15,
                    0.3, 0.5, 0.3, new Particle.DustOptions(Color.fromRGB(200, 0, 20), 1.5f));
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.7f, 1.4f);
        }
    }

    // ------------------------------------------------------------- 血月图腾

    @EventHandler(ignoreCancelled = true)
    public void onResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        EquipmentSlot hand = event.getHand();
        if (hand == null) {
            return;
        }
        ItemStack item = player.getInventory().getItem(hand);
        if (item == null || !Items.is(item, Items.ID_TOTEM)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                    player.getLocation().add(0, 1, 0), 80, 1.5, 1.5, 1.5, 0.4);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.2f, 0.7f);
            for (Entity e : player.getNearbyEntities(8, 4, 8)) {
                if (e instanceof Monster monster) {
                    Vector away = monster.getLocation().toVector().subtract(player.getLocation().toVector());
                    if (away.lengthSquared() < 0.01) {
                        away = new Vector(0, 0.2, 0);
                    }
                    monster.setVelocity(away.normalize().multiply(1.6).setY(0.7));
                    monster.damage(8.0, player);
                }
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 400, 1));
            player.sendMessage(Text.msg("血月图腾炸裂, 血爆击退了周围的魔物!", NamedTextColor.RED));
        });
    }

    // ------------------------------------------------------------- 右键物品

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        String id = Items.idOf(item);
        if (id == null) {
            return;
        }

        switch (id) {
            case Items.ID_CLOT -> {
                event.setCancelled(true);
                item.setAmount(item.getAmount() - 1);
                AttributeInstance maxHp = player.getAttribute(Attribute.MAX_HEALTH);
                if (maxHp != null) {
                    player.setHealth(Math.min(maxHp.getValue(), player.getHealth() + 8.0));
                }
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0));
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.5, 0), 6, 0.4, 0.4, 0.4);
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1f, 0.8f);
            }
            case Items.ID_HORN -> {
                event.setCancelled(true);
                long now = System.currentTimeMillis();
                long last = hornCooldown.getOrDefault(player.getUniqueId(), 0L);
                if (now - last < 60_000L) {
                    long left = (60_000L - (now - last)) / 1000L + 1;
                    player.sendMessage(Text.msg("号角尚在回响... 还需 " + left + " 秒。", NamedTextColor.GRAY));
                    return;
                }
                hornCooldown.put(player.getUniqueId(), now);
                int revealed = 0;
                for (Entity e : player.getNearbyEntities(30, 20, 30)) {
                    if (e instanceof Monster monster) {
                        monster.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0, true, false));
                        revealed++;
                    }
                }
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_1, 2f, 1f);
                player.sendMessage(Text.msg("号角长鸣! " + revealed + " 只魔物无所遁形。", NamedTextColor.YELLOW));
            }
            default -> {
            }
        }
    }
}
