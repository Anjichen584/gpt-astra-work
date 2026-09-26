package com.arena.bloodmoon.stats;

import com.arena.bloodmoon.BloodMoonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 战绩持久化：击杀 / 精英击杀 / 领主击杀 / 幸存夜数 / 获得残晶 / 阵亡数。
 */
public class StatsManager {

    public static class PlayerStats {
        public int kills;
        public int eliteKills;
        public int bossKills;
        public int nightsSurvived;
        public int shardsEarned;
        public int deaths;
    }

    private final BloodMoonPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerStats> stats = new HashMap<>();
    private int moonCount;

    public StatsManager(BloodMoonPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        load();
    }

    public void startAutoSave() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::save, 20L * 300, 20L * 300);
    }

    // ------------------------------------------------------------- 读写

    private void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        moonCount = yml.getInt("moon-count", 0);
        var section = yml.getConfigurationSection("players");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                PlayerStats ps = new PlayerStats();
                ps.kills = section.getInt(key + ".kills");
                ps.eliteKills = section.getInt(key + ".elite-kills");
                ps.bossKills = section.getInt(key + ".boss-kills");
                ps.nightsSurvived = section.getInt(key + ".nights-survived");
                ps.shardsEarned = section.getInt(key + ".shards-earned");
                ps.deaths = section.getInt(key + ".deaths");
                stats.put(id, ps);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("moon-count", moonCount);
        for (Map.Entry<UUID, PlayerStats> entry : stats.entrySet()) {
            String base = "players." + entry.getKey();
            PlayerStats ps = entry.getValue();
            yml.set(base + ".kills", ps.kills);
            yml.set(base + ".elite-kills", ps.eliteKills);
            yml.set(base + ".boss-kills", ps.bossKills);
            yml.set(base + ".nights-survived", ps.nightsSurvived);
            yml.set(base + ".shards-earned", ps.shardsEarned);
            yml.set(base + ".deaths", ps.deaths);
        }
        try {
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("无法保存 stats.yml: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------- 更新

    public PlayerStats of(UUID id) {
        return stats.computeIfAbsent(id, k -> new PlayerStats());
    }

    public void addKill(UUID id) {
        of(id).kills++;
    }

    public void addEliteKill(UUID id) {
        of(id).eliteKills++;
    }

    public void addBossKill(UUID id) {
        of(id).bossKills++;
    }

    public void addNightSurvived(UUID id) {
        of(id).nightsSurvived++;
    }

    public void addShards(UUID id, int amount) {
        of(id).shardsEarned += amount;
    }

    public void addDeath(UUID id) {
        of(id).deaths++;
    }

    public int incrementMoonCount() {
        return ++moonCount;
    }

    public int getMoonCount() {
        return moonCount;
    }

    // ------------------------------------------------------------- 排行

    public List<Map.Entry<String, Integer>> top(String category, int limit) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>();
        for (Map.Entry<UUID, PlayerStats> entry : stats.entrySet()) {
            int value = switch (category) {
                case "kills" -> entry.getValue().kills;
                case "elites" -> entry.getValue().eliteKills;
                case "boss" -> entry.getValue().bossKills;
                case "nights" -> entry.getValue().nightsSurvived;
                case "shards" -> entry.getValue().shardsEarned;
                default -> entry.getValue().kills;
            };
            if (value <= 0) {
                continue;
            }
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            String name = op.getName() != null ? op.getName() : entry.getKey().toString().substring(0, 8);
            list.add(Map.entry(name, value));
        }
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return list.size() > limit ? list.subList(0, limit) : list;
    }
}
