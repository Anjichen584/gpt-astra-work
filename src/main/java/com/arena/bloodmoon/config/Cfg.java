package com.arena.bloodmoon.config;

import com.arena.bloodmoon.BloodMoonPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;

/**
 * 配置统一入口。
 * <p>所有"怪物显示（高亮 / 头顶名称 / 显示范围）"与"刷怪量"相关参数都集中在这里读取，
 * 代码中不再散落硬编码数值；<code>/bm reload</code> 后立即生效（每次读取实时配置）。
 */
public final class Cfg {

    private Cfg() {
    }

    private static FileConfiguration c() {
        return BloodMoonPlugin.get().getConfig();
    }

    /* ==================================================== 显示：高亮发光 */

    /** 精英怪是否发光高亮。 */
    public static boolean glowElite() {
        return c().getBoolean("display.glow.elite", true);
    }

    /** 领主是否发光高亮。 */
    public static boolean glowBoss() {
        return c().getBoolean("display.glow.boss", true);
    }

    /** 高亮描边颜色（队伍颜色，取不到时回退 DARK_RED）。 */
    public static NamedTextColor glowColor() {
        return parseColor(c().getString("display.glow.color", "DARK_RED"), NamedTextColor.DARK_RED);
    }

    /* ==================================================== 显示：头顶名称 */

    /** 精英怪是否显示头顶名称。 */
    public static boolean nameElite() {
        return c().getBoolean("display.name.elite", true);
    }

    /** 领主是否显示头顶名称。 */
    public static boolean nameBoss() {
        return c().getBoolean("display.name.boss", true);
    }

    /** 名称是否常显（false = 仅准星指向时显示，原版行为）。 */
    public static boolean nameAlwaysVisible() {
        return c().getBoolean("display.name.always-visible", true);
    }

    /**
     * 名称显示范围（格）。
     * <p>&lt;= 0 表示不做范围限制（交给客户端默认约 32 格）。
     */
    public static double nameViewRange() {
        return c().getDouble("display.name.view-range", 32.0D);
    }

    /** 名称范围检查任务的间隔（秒），低频调度，避免 TPS 损耗。 */
    public static int nameRangeCheckSeconds() {
        return Math.max(1, c().getInt("display.name.range-check-seconds", 2));
    }

    /** 精英名称格式，占位符 {name} = 精英类型名。 */
    public static String nameEliteFormat() {
        return c().getString("display.name.elite-format", "&4&l\u2620 {name} \u2620");
    }

    /** 领主名称格式，占位符 {name} = 领主名。 */
    public static String nameBossFormat() {
        return c().getString("display.name.boss-format", "&4&l\u2620 {name} \u2620");
    }

    /** 领主名字本体。 */
    public static String bossDisplayName() {
        return c().getString("display.name.boss-name", "血月领主 \u00b7 卡尔诺斯");
    }

    /** 是否显示怪物血量条（名称后追加 HP）。 */
    public static boolean nameShowHealth() {
        return c().getBoolean("display.name.show-health", false);
    }

    /* ==================================================== 刷怪量：尸潮压力 */

    public static boolean pressureEnable() {
        // 兼容旧键 mobs.spawn-pressure
        return c().getBoolean("spawns.pressure.enable", c().getBoolean("mobs.spawn-pressure", true));
    }

    /** 压力刷怪的执行间隔（秒）。 */
    public static int pressureIntervalSeconds() {
        return Math.max(5, c().getInt("spawns.pressure.interval-seconds", 30));
    }

    /** 玩家周围敌对生物数量上限，超过则跳过本次加压。 */
    public static int pressureCap() {
        return c().getInt("spawns.pressure.cap", c().getInt("mobs.pressure-cap", 24));
    }

    /** 每名玩家本轮被加压的概率。 */
    public static double pressureChancePerPlayer() {
        return c().getDouble("spawns.pressure.chance-per-player", 0.4D);
    }

    /** 每波刷怪量 = base + per-tier × 阶层等级。 */
    public static int pressurePackBase() {
        return c().getInt("spawns.pressure.pack-base", 2);
    }

    public static int pressurePackPerTier() {
        return c().getInt("spawns.pressure.pack-per-tier", 1);
    }

    /** 一次加压最多生成多少只（硬上限，防止配置写飞）。 */
    public static int pressurePackMax() {
        return Math.max(1, c().getInt("spawns.pressure.pack-max", 12));
    }

    public static int pressureRadiusMin() {
        return c().getInt("spawns.pressure.radius-min", 12);
    }

    public static int pressureRadiusMax() {
        return c().getInt("spawns.pressure.radius-max", 22);
    }

    /** 统计附近怪物数量时的检测半径（水平 / 垂直）。 */
    public static int pressureScanRadius() {
        return c().getInt("spawns.pressure.scan-radius", 28);
    }

    public static int pressureScanHeight() {
        return c().getInt("spawns.pressure.scan-height", 16);
    }

    /* ==================================================== 刷怪量：尸潮事件 */

    /** 尸潮事件刷怪量 = base + per-tier × 阶层等级。 */
    public static int hordeBase() {
        return c().getInt("spawns.horde.base", 5);
    }

    public static int hordePerTier() {
        return c().getInt("spawns.horde.per-tier", 2);
    }

    public static int hordeMax() {
        return Math.max(1, c().getInt("spawns.horde.max", 30));
    }

    public static int hordeRadiusMin() {
        return c().getInt("spawns.horde.radius-min", 8);
    }

    public static int hordeRadiusMax() {
        return c().getInt("spawns.horde.radius-max", 16);
    }

    /* ==================================================== 刷怪量：自然刷怪接管 */

    /** 精英替换概率 = 阶层精英概率 × 该倍率。 */
    public static double eliteChanceMultiplier() {
        return c().getDouble("spawns.elite.chance-multiplier", 1.0D);
    }

    /** 自然刷怪时附带增援小队的概率。 */
    public static double reinforcementChance() {
        return c().getDouble("spawns.reinforcement.chance", 0.35D);
    }

    /** 增援小队额外数量上限（0 = 使用阶层 packs 默认值）。 */
    public static int reinforcementMax() {
        return c().getInt("spawns.reinforcement.max", 0);
    }

    public static int reinforcementRadiusMin() {
        return c().getInt("spawns.reinforcement.radius-min", 2);
    }

    public static int reinforcementRadiusMax() {
        return c().getInt("spawns.reinforcement.radius-max", 5);
    }

    /* ==================================================== 工具 */

    private static NamedTextColor parseColor(String name, NamedTextColor fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        NamedTextColor color = NamedTextColor.NAMES.value(name.trim().toLowerCase(Locale.ROOT));
        return color == null ? fallback : color;
    }
}
