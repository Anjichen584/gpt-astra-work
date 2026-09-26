package com.arena.bloodmoon.mob;

import com.arena.bloodmoon.tier.Tier;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

/**
 * 血月怪物属性强化 + 随机装备。
 */
public final class MobBuffer {

    private static NamespacedKey mark;

    private MobBuffer() {
    }

    public static void init(Plugin plugin) {
        mark = new NamespacedKey(plugin, "bm_mob");
    }

    public static void mark(Entity entity) {
        entity.getPersistentDataContainer().set(mark, PersistentDataType.BYTE, (byte) 1);
    }

    public static boolean isMarked(Entity entity) {
        return entity.getPersistentDataContainer().has(mark, PersistentDataType.BYTE);
    }

    /** 按阶层放大血量/攻击/移速。 */
    public static void buff(LivingEntity mob, Tier tier) {
        mark(mob);

        AttributeInstance hp = mob.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null) {
            hp.setBaseValue(hp.getBaseValue() * tier.healthMult());
            mob.setHealth(hp.getValue());
        }
        AttributeInstance dmg = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dmg != null) {
            dmg.setBaseValue(dmg.getBaseValue() * tier.damageMult());
        }
        AttributeInstance speed = mob.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * tier.speedMult());
        }
        AttributeInstance follow = mob.getAttribute(Attribute.FOLLOW_RANGE);
        if (follow != null && follow.getBaseValue() < 40.0) {
            follow.setBaseValue(40.0);
        }
        if (tier.level() >= 4) {
            mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 0, true, false));
        }
    }

    /** 给僵尸/骷髅类随机穿装备。 */
    public static void gear(LivingEntity mob, Tier tier, double baseChance, Random random) {
        boolean zombieLike = mob instanceof Zombie;
        boolean skeletonLike = mob instanceof AbstractSkeleton;
        if (!zombieLike && !skeletonLike) {
            return;
        }
        EntityEquipment eq = mob.getEquipment();
        if (eq == null) {
            return;
        }
        double chance = Math.min(0.9, baseChance + tier.level() * 0.08);
        if (random.nextDouble() >= chance) {
            return;
        }

        String prefix = switch (tier.level()) {
            case 1, 2 -> random.nextBoolean() ? "CHAINMAIL" : "GOLDEN";
            case 3 -> "IRON";
            default -> random.nextDouble() < 0.4 ? "DIAMOND" : "IRON";
        };

        if (random.nextDouble() < 0.8) {
            eq.setHelmet(enchanted(Material.valueOf(prefix + "_HELMET"), tier, random));
            eq.setHelmetDropChance(0.02f);
        }
        if (random.nextDouble() < 0.5) {
            eq.setChestplate(enchanted(Material.valueOf(prefix + "_CHESTPLATE"), tier, random));
            eq.setChestplateDropChance(0.02f);
        }
        if (random.nextDouble() < 0.35) {
            eq.setBoots(enchanted(Material.valueOf(prefix + "_BOOTS"), tier, random));
            eq.setBootsDropChance(0.02f);
        }

        if (zombieLike && random.nextDouble() < 0.55) {
            Material weapon = switch (tier.level()) {
                case 1, 2 -> random.nextBoolean() ? Material.STONE_SWORD : Material.STONE_AXE;
                case 3 -> Material.IRON_SWORD;
                default -> random.nextDouble() < 0.35 ? Material.DIAMOND_SWORD : Material.IRON_AXE;
            };
            ItemStack item = new ItemStack(weapon);
            if (tier.level() >= 3 && random.nextDouble() < 0.5) {
                item.addUnsafeEnchantment(Enchantment.SHARPNESS, 1 + random.nextInt(tier.level()));
            }
            eq.setItemInMainHand(item);
            eq.setItemInMainHandDropChance(0.02f);
        }
        if (skeletonLike && tier.level() >= 3) {
            ItemStack bow = new ItemStack(Material.BOW);
            bow.addUnsafeEnchantment(Enchantment.POWER, Math.max(1, tier.level() - 2));
            if (tier.level() >= 4 && random.nextDouble() < 0.4) {
                bow.addUnsafeEnchantment(Enchantment.FLAME, 1);
            }
            eq.setItemInMainHand(bow);
            eq.setItemInMainHandDropChance(0.02f);
        }
    }

    private static ItemStack enchanted(Material material, Tier tier, Random random) {
        ItemStack item = new ItemStack(material);
        if (tier.level() >= 2 && random.nextDouble() < 0.5) {
            item.addUnsafeEnchantment(Enchantment.PROTECTION, 1 + random.nextInt(Math.max(1, tier.level() - 1)));
        }
        if (tier.level() >= 4 && random.nextDouble() < 0.25) {
            item.addUnsafeEnchantment(Enchantment.THORNS, 1);
        }
        return item;
    }
}
