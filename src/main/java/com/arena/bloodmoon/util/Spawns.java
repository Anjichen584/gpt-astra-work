package com.arena.bloodmoon.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Random;

/**
 * 刷怪落点工具：在中心附近找一个"脚下实心、身位两格空气"的位置。
 */
public final class Spawns {

    private Spawns() {
    }

    public static Location findSpot(Location center, Random random, int minRadius, int maxRadius) {
        World world = center.getWorld();
        if (world == null) {
            return null;
        }
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int dist = minRadius + random.nextInt(Math.max(1, maxRadius - minRadius + 1));
            int x = center.getBlockX() + (int) Math.round(Math.cos(angle) * dist);
            int z = center.getBlockZ() + (int) Math.round(Math.sin(angle) * dist);
            int baseY = center.getBlockY();
            for (int dy : new int[]{0, 1, 2, -1, -2, 3, -3, 4, -4}) {
                int y = baseY + dy;
                if (y < world.getMinHeight() + 1 || y > world.getMaxHeight() - 2) {
                    continue;
                }
                Block feet = world.getBlockAt(x, y, z);
                Block head = world.getBlockAt(x, y + 1, z);
                Block ground = world.getBlockAt(x, y - 1, z);
                if (feet.getType().isAir() && head.getType().isAir() && ground.getType().isSolid()) {
                    return new Location(world, x + 0.5, y, z + 0.5);
                }
            }
        }
        return null;
    }
}
