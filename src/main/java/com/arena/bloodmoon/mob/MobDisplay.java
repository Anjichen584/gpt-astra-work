package com.arena.bloodmoon.mob;

import com.arena.bloodmoon.BloodMoonPlugin;
import com.arena.bloodmoon.config.Cfg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 怪物显示统一管理：头顶名称、发光高亮、名称显示范围。
 * <p>全部开关与数值来自 config.yml（display.* 段），
 * 名称范围用低频任务维护（默认 2 秒一次，只遍历已登记的血月怪，不扫全图实体）。
 */
public final class MobDisplay {

    private static final String TEAM_NAME = "bm_glow";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    /** 已登记的血月怪 -> 其名称组件（范围显隐时复用，避免重复构造）。 */
    private static final Map<UUID, Component> TRACKED = new ConcurrentHashMap<>();

    private static BukkitTask rangeTask;

    private MobDisplay() {
    }

    /* ============================================ 名称 / 高亮 应用 */

    /**
     * 按配置给怪物套用名称与高亮。
     *
     * @param mob      目标
     * @param rawName  名称本体（未套格式）
     * @param format   config 里的格式模板，含 {name}
     * @param showName 该类怪物是否允许显示名称
     * @param glow     该类怪物是否允许高亮
     */
    public static void apply(LivingEntity mob, String rawName, String format, boolean showName, boolean glow) {
        if (showName) {
            Component name = LEGACY.deserialize(format.replace("{name}", rawName));
            mob.customName(name);
            mob.setCustomNameVisible(Cfg.nameAlwaysVisible());
            TRACKED.put(mob.getUniqueId(), name);
        } else {
            mob.customName(null);
            mob.setCustomNameVisible(false);
            TRACKED.remove(mob.getUniqueId());
        }

        if (glow) {
            glowTeam().addEntry(mob.getUniqueId().toString());
            mob.setGlowing(true);
        } else {
            mob.setGlowing(false);
            glowTeam().removeEntry(mob.getUniqueId().toString());
        }
    }

    /** 高亮队伍（颜色来自 config，reload 后自动刷新颜色）。 */
    private static Team glowTeam() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(TEAM_NAME);
        if (team == null) {
            team = board.registerNewTeam(TEAM_NAME);
        }
        team.color(Cfg.glowColor());
        return team;
    }

    /* ============================================ 名称显示范围 */

    /** 启动名称范围维护任务（低频，间隔来自 config）。 */
    public static void startRangeTask(BloodMoonPlugin plugin) {
        stopRangeTask();
        long period = Cfg.nameRangeCheckSeconds() * 20L;
        rangeTask = Bukkit.getScheduler().runTaskTimer(plugin, MobDisplay::tickRange, period, period);
    }

    public static void stopRangeTask() {
        if (rangeTask != null) {
            rangeTask.cancel();
            rangeTask = null;
        }
    }

    /** reload 后按新配置重建任务并立即刷新一次。 */
    public static void reload(BloodMoonPlugin plugin) {
        startRangeTask(plugin);
        tickRange();
    }

    private static void tickRange() {
        if (TRACKED.isEmpty()) {
            return;
        }
        double range = Cfg.nameViewRange();
        boolean always = Cfg.nameAlwaysVisible();
        boolean limited = range > 0.0D;
        double rangeSq = range * range;

        for (Map.Entry<UUID, Component> entry : TRACKED.entrySet()) {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity mob) || !mob.isValid()) {
                TRACKED.remove(entry.getKey()); // 死亡/卸载，清理防止泄漏
                continue;
            }
            if (!always) {
                mob.setCustomNameVisible(false);
                continue;
            }
            if (!limited) {
                mob.setCustomNameVisible(true);
                refreshHealthSuffix(mob, entry.getValue());
                continue;
            }
            mob.setCustomNameVisible(hasPlayerWithin(mob, rangeSq));
            refreshHealthSuffix(mob, entry.getValue());
        }
    }

    private static boolean hasPlayerWithin(LivingEntity mob, double rangeSq) {
        Location loc = mob.getLocation();
        for (Player player : mob.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(loc) <= rangeSq) {
                return true;
            }
        }
        return false;
    }

    /** 可选：名称后追加血量（display.name.show-health）。 */
    private static void refreshHealthSuffix(LivingEntity mob, Component base) {
        if (!Cfg.nameShowHealth()) {
            return;
        }
        AttributeInstance max = mob.getAttribute(Attribute.MAX_HEALTH);
        int cur = (int) Math.ceil(mob.getHealth());
        int hp = max == null ? cur : (int) Math.ceil(max.getValue());
        mob.customName(base.append(LEGACY.deserialize(" &c" + cur + "&7/&c" + hp + " &4\u2764")));
    }

    /* ============================================ 清理 */

    public static void untrack(UUID uuid) {
        TRACKED.remove(uuid);
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(TEAM_NAME);
        if (team != null) {
            team.removeEntry(uuid.toString());
        }
    }

    public static void clear() {
        TRACKED.clear();
    }

    public static Set<UUID> tracked() {
        return TRACKED.keySet();
    }
}
