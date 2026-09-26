package com.arena.bloodmoon.command;

import com.arena.bloodmoon.BloodMoonManager;
import com.arena.bloodmoon.BloodMoonPlugin;
import com.arena.bloodmoon.mob.MobDisplay;
import com.arena.bloodmoon.mob.EliteFactory;
import com.arena.bloodmoon.stats.StatsManager;
import com.arena.bloodmoon.tier.Tier;
import com.arena.bloodmoon.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /bloodmoon 主命令。
 */
public class BloodMoonCommand implements org.bukkit.command.CommandExecutor, TabCompleter {

    private final BloodMoonPlugin plugin;

    public BloodMoonCommand(BloodMoonPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return help(sender);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "help" -> help(sender);
            case "status" -> status(sender);
            case "shop" -> shop(sender);
            case "stats" -> stats(sender);
            case "top" -> top(sender, args);
            case "start" -> admin(sender) && start(sender, args);
            case "stop" -> admin(sender) && stop(sender);
            case "boss" -> admin(sender) && boss(sender);
            case "spawn" -> admin(sender) && spawnElite(sender, args);
            case "give" -> admin(sender) && give(sender, args);
            case "reload" -> admin(sender) && reload(sender);
            default -> help(sender);
        };
    }

    private boolean admin(CommandSender sender) {
        if (!sender.hasPermission("bloodmoon.admin")) {
            sender.sendMessage(Text.msg("你没有权限执行血月管理命令。", NamedTextColor.RED));
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------- 子命令

    private boolean help(CommandSender sender) {
        sender.sendMessage(Component.text("========== 血月 BloodMoon ==========", NamedTextColor.DARK_RED));
        sender.sendMessage(Text.msg("/bm status - 查看血月状态"));
        sender.sendMessage(Text.msg("/bm shop - 打开血月黑市"));
        sender.sendMessage(Text.msg("/bm stats - 查看自己的血月战绩"));
        sender.sendMessage(Text.msg("/bm top <kills|elites|boss|nights|shards> - 排行榜"));
        if (sender.hasPermission("bloodmoon.admin")) {
            sender.sendMessage(Text.msg("/bm start [1-5] - 强制开启血月(可指定阶层)", NamedTextColor.GOLD));
            sender.sendMessage(Text.msg("/bm stop - 强制结束血月", NamedTextColor.GOLD));
            sender.sendMessage(Text.msg("/bm boss - 在脚下召唤血月领主", NamedTextColor.GOLD));
            sender.sendMessage(Text.msg("/bm spawn <精英类型> - 召唤指定精英", NamedTextColor.GOLD));
            sender.sendMessage(Text.msg("/bm give <数量> - 给自己血月残晶", NamedTextColor.GOLD));
            sender.sendMessage(Text.msg("/bm reload - 重载配置", NamedTextColor.GOLD));
        }
        return true;
    }

    private boolean status(CommandSender sender) {
        BloodMoonManager mgr = plugin.getMoonManager();
        World world = mgr.getPrimaryWorld();
        sender.sendMessage(Component.text("---------- 血月状态 ----------", NamedTextColor.DARK_RED));
        if (mgr.isActive()) {
            Tier tier = mgr.getTier();
            long time = world != null ? world.getTime() : 0;
            long remainTicks = Math.max(0, 23460L - time);
            long minutes = remainTicks / 1200L;
            sender.sendMessage(Text.msg("状态: 血月进行中!", NamedTextColor.RED));
            sender.sendMessage(Text.msg("阶层: " + tier.display() + " (第 " + tier.level() + " 阶)", tier.color()));
            sender.sendMessage(Text.msg("距黎明约: " + minutes + " 分钟"));
        } else {
            sender.sendMessage(Text.msg("状态: 平静... 暂无血月。", NamedTextColor.GRAY));
            double chance = plugin.getConfig().getDouble("trigger.chance", 0.25);
            sender.sendMessage(Text.msg("今晚基础触发概率: " + Math.round(chance * 100) + "%"));
            if (world != null && mgr.isFullMoon(world)) {
                sender.sendMessage(Text.msg("今夜是满月... 血月蠢蠢欲动。", NamedTextColor.RED));
            }
        }
        sender.sendMessage(Text.msg("历史血月次数: " + plugin.getStatsManager().getMoonCount()));
        return true;
    }

    private boolean shop(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.msg("只有玩家可以打开黑市。"));
            return true;
        }
        plugin.getShopGUI().open(player);
        return true;
    }

    private boolean stats(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.msg("只有玩家可以查看战绩。"));
            return true;
        }
        StatsManager.PlayerStats ps = plugin.getStatsManager().of(player.getUniqueId());
        sender.sendMessage(Component.text("---------- 血月战绩 ----------", NamedTextColor.DARK_RED));
        sender.sendMessage(Text.msg("血月击杀: " + ps.kills));
        sender.sendMessage(Text.msg("精英击杀: " + ps.eliteKills));
        sender.sendMessage(Text.msg("领主击杀: " + ps.bossKills));
        sender.sendMessage(Text.msg("幸存夜数: " + ps.nightsSurvived));
        sender.sendMessage(Text.msg("累计获得残晶: " + ps.shardsEarned));
        sender.sendMessage(Text.msg("血月阵亡: " + ps.deaths));
        return true;
    }

    private boolean top(CommandSender sender, String[] args) {
        String category = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "kills";
        String title = switch (category) {
            case "elites" -> "精英击杀";
            case "boss" -> "领主击杀";
            case "nights" -> "幸存夜数";
            case "shards" -> "残晶收入";
            default -> "血月击杀";
        };
        sender.sendMessage(Component.text("---------- 排行榜 · " + title + " ----------", NamedTextColor.DARK_RED));
        List<Map.Entry<String, Integer>> list = plugin.getStatsManager().top(category, 10);
        if (list.isEmpty()) {
            sender.sendMessage(Text.msg("暂无数据。血月尚未收割过灵魂..."));
        }
        int rank = 1;
        for (Map.Entry<String, Integer> entry : list) {
            NamedTextColor color = rank == 1 ? NamedTextColor.GOLD
                    : rank == 2 ? NamedTextColor.WHITE
                    : rank == 3 ? NamedTextColor.YELLOW : NamedTextColor.GRAY;
            sender.sendMessage(Component.text(" #" + rank + " " + entry.getKey() + " - " + entry.getValue(), color));
            rank++;
        }
        return true;
    }

    private boolean start(CommandSender sender, String[] args) {
        Tier tier;
        if (args.length > 1) {
            try {
                tier = Tier.ofLevel(Integer.parseInt(args[1]));
            } catch (NumberFormatException ex) {
                sender.sendMessage(Text.msg("阶层必须是 1-5 的数字。", NamedTextColor.RED));
                return true;
            }
        } else {
            tier = Tier.SLAUGHTER;
        }
        plugin.getMoonManager().forceStart(tier);
        sender.sendMessage(Text.msg("已强制召唤 " + tier.display() + "!", NamedTextColor.GOLD));
        return true;
    }

    private boolean stop(CommandSender sender) {
        if (!plugin.getMoonManager().isActive()) {
            sender.sendMessage(Text.msg("当前没有血月。"));
            return true;
        }
        plugin.getMoonManager().forceStop();
        sender.sendMessage(Text.msg("血月已被强制驱散。", NamedTextColor.GOLD));
        return true;
    }

    private boolean boss(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.msg("只有玩家可以召唤领主。"));
            return true;
        }
        if (plugin.getBossManager().isAlive()) {
            sender.sendMessage(Text.msg("血月领主已在世间游荡。", NamedTextColor.RED));
            return true;
        }
        plugin.getBossManager().spawn(player.getLocation().add(3, 0, 3), plugin.getMoonManager().getTier());
        return true;
    }

    private boolean spawnElite(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.msg("只有玩家可以召唤精英。"));
            return true;
        }
        if (args.length < 2) {
            StringBuilder sb = new StringBuilder();
            for (EliteFactory.EliteType t : EliteFactory.EliteType.values()) {
                sb.append(t.name().toLowerCase(Locale.ROOT)).append(" ");
            }
            sender.sendMessage(Text.msg("可用精英: " + sb));
            return true;
        }
        EliteFactory.EliteType type;
        try {
            type = EliteFactory.EliteType.valueOf(args[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(Text.msg("未知的精英类型: " + args[1], NamedTextColor.RED));
            return true;
        }
        EliteFactory.spawnElite(type, player.getLocation().add(2, 0, 2), plugin.getMoonManager().getTier());
        sender.sendMessage(Text.msg("已召唤 " + type.display() + "!", NamedTextColor.GOLD));
        return true;
    }

    private boolean give(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.msg("只有玩家可以领取残晶。"));
            return true;
        }
        int amount = 8;
        if (args.length > 1) {
            try {
                amount = Math.max(1, Math.min(640, Integer.parseInt(args[1])));
            } catch (NumberFormatException ignored) {
            }
        }
        plugin.getMoonManager().giveShards(player, amount);
        sender.sendMessage(Text.msg("已给予 " + amount + " 枚血月残晶。", NamedTextColor.GOLD));
        return true;
    }

    private boolean reload(CommandSender sender) {
        plugin.reloadConfig();
        // 重载后按新配置重建名称范围任务并立即刷新一次显示
        MobDisplay.reload(plugin);
        sender.sendMessage(Text.msg("配置已重载。", NamedTextColor.GOLD));
        return true;
    }

    // ------------------------------------------------------------- Tab 补全

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("help", "status", "shop", "stats", "top"));
            if (sender.hasPermission("bloodmoon.admin")) {
                subs.addAll(List.of("start", "stop", "boss", "spawn", "give", "reload"));
            }
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "start" -> out.addAll(List.of("1", "2", "3", "4", "5"));
                case "top" -> out.addAll(List.of("kills", "elites", "boss", "nights", "shards"));
                case "spawn" -> {
                    for (EliteFactory.EliteType t : EliteFactory.EliteType.values()) {
                        String name = t.name().toLowerCase(Locale.ROOT);
                        if (name.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                            out.add(name);
                        }
                    }
                }
                case "give" -> out.addAll(List.of("8", "16", "32", "64"));
                default -> {
                }
            }
        }
        return out;
    }
}
