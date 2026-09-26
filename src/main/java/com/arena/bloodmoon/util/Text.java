package com.arena.bloodmoon.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * 文本 / 广播工具。
 */
public final class Text {

    private Text() {
    }

    public static Component prefix() {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text("血月", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY));
    }

    public static Component msg(String body) {
        return prefix().append(Component.text(body, NamedTextColor.GRAY));
    }

    public static Component msg(String body, NamedTextColor color) {
        return prefix().append(Component.text(body, color));
    }

    /** 向某个世界的所有玩家 + 控制台广播。 */
    public static void broadcast(World world, Component component) {
        for (Player p : world.getPlayers()) {
            p.sendMessage(component);
        }
        Bukkit.getConsoleSender().sendMessage(component);
    }

    public static void title(Player p, String main, NamedTextColor mainColor,
                             String sub, NamedTextColor subColor) {
        p.showTitle(Title.title(
                Component.text(main, mainColor, TextDecoration.BOLD),
                Component.text(sub, subColor),
                Title.Times.times(Duration.ofMillis(400), Duration.ofMillis(2800), Duration.ofMillis(800))));
    }
}
