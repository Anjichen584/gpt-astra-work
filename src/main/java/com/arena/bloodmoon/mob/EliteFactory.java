package com.arena.bloodmoon.mob;

import com.arena.bloodmoon.tier.Tier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Witch;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 精英魔物工厂 —— 全部使用原版实体"自配造型":
 * 体型(SCALE)/装备/染色皮甲/坐骑组合/红色发光描边/名牌/药水效果。
 */
public final class EliteFactory {

    /** 精英类型。 */
    public enum EliteType {
        BUTCHER("血月屠夫"),
        HUNTRESS("猩红猎手"),
        PLAGUEBEARER("瘟疫使者"),
        BONE_RIDER("骸骨骑士"),
        BOOMFIEND("爆裂魔灵"),
        SOUL_RIPPER("噬魂魔翼"),
        CRUSHER("血月碾压兽"),
        SOULBINDER("缚魂术士"),
        TIDE_WRAITH("溺渊拖行者"),
        ASH_WALKER("焦骨浪人"),
        JOCKEY("血月小丑骑士");

        private final String display;

        EliteType(String display) {
            this.display = display;
        }

        public String display() {
            return display;
        }

        public static EliteType random(Random random) {
            EliteType[] all = values();
            return all[random.nextInt(all.length)];
        }
    }

    private static final Map<UUID, EliteType> REGISTRY = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();
    private static NamespacedKey eliteKey;

    private EliteFactory() {
    }

    public static void init(Plugin plugin) {
        eliteKey = new NamespacedKey(plugin, "bm_elite");
    }

    public static Map<UUID, EliteType> registry() {
        return REGISTRY;
    }

    public static EliteType getType(Entity entity) {
        EliteType cached = REGISTRY.get(entity.getUniqueId());
        if (cached != null) {
            return cached;
        }
        String stored = entity.getPersistentDataContainer().get(eliteKey, PersistentDataType.STRING);
        if (stored == null) {
            return null;
        }
        try {
            return EliteType.valueOf(stored);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** 生成一只普通"血月强化怪"(用于尸潮/组队增援)。 */
    public static LivingEntity spawnBuffedBasic(EntityType type, Location loc, Tier tier) {
        World world = loc.getWorld();
        if (world == null || !type.isSpawnable()) {
            return null;
        }
        Entity entity = world.spawnEntity(loc, type);
        if (entity instanceof LivingEntity living) {
            MobBuffer.buff(living, tier);
            MobBuffer.gear(living, tier, 0.25, RANDOM);
            return living;
        }
        return null;
    }

    /** 生成指定精英。返回主实体。 */
    public static LivingEntity spawnElite(EliteType type, Location loc, Tier tier) {
        World world = loc.getWorld();
        if (world == null) {
            return null;
        }
        LivingEntity elite = switch (type) {
            case BUTCHER -> spawnButcher(world, loc, tier);
            case HUNTRESS -> spawnHuntress(world, loc, tier);
            case PLAGUEBEARER -> spawnPlaguebearer(world, loc, tier);
            case BONE_RIDER -> spawnBoneRider(world, loc, tier);
            case BOOMFIEND -> spawnBoomfiend(world, loc, tier);
            case SOUL_RIPPER -> spawnSoulRipper(world, loc, tier);
            case CRUSHER -> spawnCrusher(world, loc, tier);
            case SOULBINDER -> spawnSoulbinder(world, loc, tier);
            case TIDE_WRAITH -> spawnTideWraith(world, loc, tier);
            case ASH_WALKER -> spawnAshWalker(world, loc, tier);
            case JOCKEY -> spawnJockey(world, loc, tier);
        };
        if (elite != null) {
            world.spawnParticle(Particle.CRIMSON_SPORE, loc.clone().add(0, 1, 0), 40, 0.6, 1.0, 0.6, 0.02);
            world.playSound(loc, Sound.ENTITY_VEX_CHARGE, 1f, 0.5f);
        }
        return elite;
    }

    // ------------------------------------------------------------- 各类精英

    private static LivingEntity spawnButcher(World world, Location loc, Tier tier) {
        Zombie z = world.spawn(loc, Zombie.class, mob -> {
            mob.setAdult();
            setScale(mob, 1.5);
            EntityEquipment eq = mob.getEquipment();
            ItemStack axe = new ItemStack(Material.IRON_AXE);
            axe.addUnsafeEnchantment(Enchantment.SHARPNESS, 2);
            eq.setItemInMainHand(axe);
            eq.setItemInMainHandDropChance(0.03f);
            eq.setHelmet(redLeather(Material.LEATHER_HELMET));
            eq.setHelmetDropChance(0f);
            AttributeInstance kb = mob.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            if (kb != null) {
                kb.setBaseValue(0.6);
            }
        });
        decorate(z, EliteType.BUTCHER, tier, 2.5);
        return z;
    }

    private static LivingEntity spawnHuntress(World world, Location loc, Tier tier) {
        Skeleton s = world.spawn(loc, Skeleton.class, mob -> {
            setScale(mob, 1.1);
            EntityEquipment eq = mob.getEquipment();
            ItemStack bow = new ItemStack(Material.BOW);
            bow.addUnsafeEnchantment(Enchantment.POWER, 3);
            bow.addUnsafeEnchantment(Enchantment.FLAME, 1);
            eq.setItemInMainHand(bow);
            eq.setItemInMainHandDropChance(0.03f);
            eq.setHelmet(redLeather(Material.LEATHER_HELMET));
            eq.setChestplate(redLeather(Material.LEATHER_CHESTPLATE));
            eq.setHelmetDropChance(0f);
            eq.setChestplateDropChance(0f);
            mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0, true, false));
        });
        decorate(s, EliteType.HUNTRESS, tier, 2.0);
        return s;
    }

    private static LivingEntity spawnPlaguebearer(World world, Location loc, Tier tier) {
        Witch w = world.spawn(loc, Witch.class, mob -> setScale(mob, 1.15));
        decorate(w, EliteType.PLAGUEBEARER, tier, 2.2);
        return w;
    }

    private static LivingEntity spawnBoneRider(World world, Location loc, Tier tier) {
        Spider spider = world.spawn(loc, Spider.class, mob -> setScale(mob, 1.3));
        MobBuffer.buff(spider, tier);
        Skeleton rider = world.spawn(loc, Skeleton.class, mob -> {
            EntityEquipment eq = mob.getEquipment();
            ItemStack bow = new ItemStack(Material.BOW);
            bow.addUnsafeEnchantment(Enchantment.POWER, 2);
            eq.setItemInMainHand(bow);
            eq.setItemInMainHandDropChance(0.03f);
            eq.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
            eq.setHelmetDropChance(0f);
        });
        decorate(rider, EliteType.BONE_RIDER, tier, 1.8);
        spider.addPassenger(rider);
        return rider;
    }

    private static LivingEntity spawnBoomfiend(World world, Location loc, Tier tier) {
        Creeper c = world.spawn(loc, Creeper.class, mob -> {
            mob.setPowered(true);
            mob.setMaxFuseTicks(20);
            mob.setExplosionRadius(4);
            mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1, true, false));
        });
        decorate(c, EliteType.BOOMFIEND, tier, 1.8);
        return c;
    }

    private static LivingEntity spawnSoulRipper(World world, Location loc, Tier tier) {
        Phantom p = world.spawn(loc.clone().add(0, 8, 0), Phantom.class, mob -> mob.setSize(4));
        decorate(p, EliteType.SOUL_RIPPER, tier, 2.0);
        return p;
    }

    private static LivingEntity spawnCrusher(World world, Location loc, Tier tier) {
        Ravager r = world.spawn(loc, Ravager.class, mob -> setScale(mob, 1.1));
        decorate(r, EliteType.CRUSHER, tier, 1.6);
        return r;
    }

    private static LivingEntity spawnSoulbinder(World world, Location loc, Tier tier) {
        Evoker e = world.spawn(loc, Evoker.class, mob -> setScale(mob, 1.1));
        decorate(e, EliteType.SOULBINDER, tier, 2.0);
        return e;
    }

    private static LivingEntity spawnTideWraith(World world, Location loc, Tier tier) {
        Drowned d = world.spawn(loc, Drowned.class, mob -> {
            setScale(mob, 1.2);
            EntityEquipment eq = mob.getEquipment();
            eq.setItemInMainHand(new ItemStack(Material.TRIDENT));
            eq.setItemInMainHandDropChance(0.05f);
            mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0, true, false));
        });
        decorate(d, EliteType.TIDE_WRAITH, tier, 2.0);
        return d;
    }

    private static LivingEntity spawnAshWalker(World world, Location loc, Tier tier) {
        WitherSkeleton w = world.spawn(loc, WitherSkeleton.class, mob -> {
            setScale(mob, 1.2);
            EntityEquipment eq = mob.getEquipment();
            ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
            sword.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 1);
            eq.setItemInMainHand(sword);
            eq.setItemInMainHandDropChance(0.02f);
            mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0, true, false));
        });
        decorate(w, EliteType.ASH_WALKER, tier, 2.0);
        return w;
    }

    private static LivingEntity spawnJockey(World world, Location loc, Tier tier) {
        Chicken chicken = world.spawn(loc, Chicken.class, mob ->
                mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 2, true, false)));
        Zombie zombie = world.spawn(loc, Zombie.class, mob -> {
            mob.setBaby();
            EntityEquipment eq = mob.getEquipment();
            eq.setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            eq.setItemInMainHandDropChance(0.03f);
            mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1, true, false));
        });
        decorate(zombie, EliteType.JOCKEY, tier, 1.6);
        chicken.addPassenger(zombie);
        return zombie;
    }

    // ------------------------------------------------------------- 公共装饰

    private static void decorate(LivingEntity mob, EliteType type, Tier tier, double hpMult) {
        MobBuffer.buff(mob, tier);
        AttributeInstance hp = mob.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null) {
            hp.setBaseValue(hp.getBaseValue() * hpMult);
            mob.setHealth(hp.getValue());
        }
        mob.customName(Component.text("\u2620 " + type.display() + " \u2620", NamedTextColor.DARK_RED, TextDecoration.BOLD));
        mob.setCustomNameVisible(true);
        mob.setPersistent(true);
        mob.setRemoveWhenFarAway(false);
        mob.getPersistentDataContainer().set(eliteKey, PersistentDataType.STRING, type.name());
        REGISTRY.put(mob.getUniqueId(), type);
        glowRed(mob);
    }

    private static void glowRed(LivingEntity mob) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam("bm_elite");
        if (team == null) {
            team = board.registerNewTeam("bm_elite");
            team.color(NamedTextColor.RED);
        }
        team.addEntry(mob.getUniqueId().toString());
        mob.setGlowing(true);
    }

    private static void setScale(LivingEntity mob, double scale) {
        AttributeInstance attr = mob.getAttribute(Attribute.SCALE);
        if (attr != null) {
            attr.setBaseValue(scale);
        }
    }

    private static ItemStack redLeather(Material material) {
        ItemStack item = new ItemStack(material);
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(Color.fromRGB(120, 0, 10));
            item.setItemMeta(meta);
        }
        return item;
    }
}
