package com.arena.bloodmoon.listener;

import com.arena.bloodmoon.BloodMoonManager;
import com.arena.bloodmoon.BloodMoonPlugin;
import com.arena.bloodmoon.mob.EliteFactory;
import com.arena.bloodmoon.mob.MobBuffer;
import com.arena.bloodmoon.tier.Tier;
import com.arena.bloodmoon.util.Spawns;
import org.bukkit.Location;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

/**
 * 血月期间接管自然刷怪：强化、精英替换、组队增援。
 */
public class MobSpawnListener implements Listener {

    private static final Set<CreatureSpawnEvent.SpawnReason> AFFECTED = EnumSet.of(
            CreatureSpawnEvent.SpawnReason.NATURAL,
            CreatureSpawnEvent.SpawnReason.SPAWNER,
            CreatureSpawnEvent.SpawnReason.REINFORCEMENTS,
            CreatureSpawnEvent.SpawnReason.PATROL
    );

    private final BloodMoonPlugin plugin;
    private final Random random = new Random();

    public MobSpawnListener(BloodMoonPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        BloodMoonManager mgr = plugin.getMoonManager();
        if (!mgr.isActive() || !mgr.isBloodMoonWorld(event.getLocation().getWorld())) {
            return;
        }
        if (!(event.getEntity() instanceof Enemy)) {
            return;
        }
        LivingEntity mob = event.getEntity();
        if (MobBuffer.isMarked(mob)) {
            return; // 插件自己生成的, 已经处理过
        }
        if (!AFFECTED.contains(event.getSpawnReason())) {
            return;
        }

        Tier tier = mgr.getTier();
        boolean natural = event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL;

        // 精英替换
        if (natural && random.nextDouble() < tier.eliteChance()) {
            event.setCancelled(true);
            EliteFactory.spawnElite(EliteFactory.EliteType.random(random), event.getLocation(), tier);
            return;
        }

        MobBuffer.buff(mob, tier);
        MobBuffer.gear(mob, tier, plugin.getConfig().getDouble("mobs.gear-chance", 0.25), random);

        // 高阶血月: 自然刷怪概率携带增援小队
        if (natural && tier.packs() > 1 && random.nextDouble() < 0.35) {
            for (int i = 1; i < tier.packs(); i++) {
                Location spot = Spawns.findSpot(event.getLocation(), random, 2, 5);
                if (spot != null) {
                    EliteFactory.spawnBuffedBasic(mob.getType(), spot, tier);
                }
            }
        }
    }
}
