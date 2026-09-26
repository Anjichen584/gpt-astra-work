package com.arena.bloodmoon.listener;

import com.arena.bloodmoon.BloodMoonManager;
import com.arena.bloodmoon.BloodMoonPlugin;
import com.arena.bloodmoon.item.Items;
import com.arena.bloodmoon.mob.EliteFactory;
import com.arena.bloodmoon.mob.MobBuffer;
import com.arena.bloodmoon.tier.Tier;
import com.arena.bloodmoon.util.Text;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 战斗结算：血晶掉落、经验加成、击杀统计、连杀播报、第一滴血、阵亡记录。
 */
public class CombatListener implements Listener {

    private final BloodMoonPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, Integer> streaks = new HashMap<>();

    public CombatListener(BloodMoonPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead instanceof Player) {
            return; // 玩家死亡由下面的方法处理
        }
        if (!MobBuffer.isMarked(dead)) {
            return;
        }
        if (plugin.getBossManager().isBoss(dead)) {
            return; // 领主掉落由 BossManager 处理
        }

        BloodMoonManager mgr = plugin.getMoonManager();
        Tier tier = mgr.getTier();

        // 经验加成
        event.setDroppedExp((int) Math.ceil(event.getDroppedExp() * tier.xpMult()));

        EliteFactory.EliteType eliteType = EliteFactory.getType(dead);
        Player killer = dead.getKiller();

        // 血晶掉落
        int shards = 0;
        if (eliteType != null) {
            shards = 2 + random.nextInt(1 + tier.level());
            dead.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                    dead.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.15);
            dead.getWorld().playSound(dead.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.5f);
            // 精英稀有掉落
            if (random.nextDouble() < 0.10) {
                event.getDrops().add(Items.clot(1));
            }
            if (random.nextDouble() < 0.05) {
                event.getDrops().add(Items.horn());
            }
        } else if (random.nextDouble() < tier.shardChance()) {
            shards = 1 + (random.nextDouble() < 0.2 ? 1 : 0);
        }
        if (shards > 0) {
            event.getDrops().add(Items.shard(shards));
        }

        if (killer == null) {
            return;
        }

        plugin.getStatsManager().addKill(killer.getUniqueId());
        if (eliteType != null) {
            plugin.getStatsManager().addEliteKill(killer.getUniqueId());
        }

        if (mgr.isActive() && mgr.isBloodMoonWorld(dead.getWorld())) {
            if (mgr.claimFirstBlood()) {
                Text.broadcast(dead.getWorld(),
                        Text.msg(killer.getName() + " 染下了今夜的第一滴血!", NamedTextColor.RED));
            }
            int streak = streaks.merge(killer.getUniqueId(), 1, Integer::sum);
            switch (streak) {
                case 10 -> Text.title(killer, "杀意渐浓", NamedTextColor.RED, "血月连杀 x10", NamedTextColor.GRAY);
                case 25 -> Text.title(killer, "杀戮如麻", NamedTextColor.DARK_RED, "血月连杀 x25", NamedTextColor.GRAY);
                case 50 -> {
                    Text.title(killer, "\u2620 血月化身 \u2620", NamedTextColor.DARK_PURPLE, "血月连杀 x50", NamedTextColor.RED);
                    Text.broadcast(dead.getWorld(),
                            Text.msg(killer.getName() + " 在血月下连杀 50 只魔物, 宛如血月化身!", NamedTextColor.DARK_PURPLE));
                }
                default -> {
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        BloodMoonManager mgr = plugin.getMoonManager();
        streaks.remove(player.getUniqueId());
        plugin.getStatsManager().addDeath(player.getUniqueId());
        if (mgr.isActive() && mgr.isBloodMoonWorld(player.getWorld())) {
            mgr.markFallen(player.getUniqueId());
            Text.broadcast(player.getWorld(),
                    Text.msg(player.getName() + " 倒在了血月之下...", NamedTextColor.DARK_RED));
        }
    }

    public void resetStreaks() {
        streaks.clear();
    }
}
