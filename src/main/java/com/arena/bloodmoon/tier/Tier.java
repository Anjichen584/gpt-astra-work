package com.arena.bloodmoon.tier;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * 血月阶层：等级越高，怪越凶，掉落越肥。
 */
public enum Tier {

    CRIMSON(1, "猩红之月", NamedTextColor.RED, 1.3, 1.2, 1.05, 0.25, 0.06, 1.5, 1),
    BLOODIED(2, "泣血之月", NamedTextColor.RED, 1.6, 1.4, 1.08, 0.32, 0.10, 2.0, 1),
    SLAUGHTER(3, "屠戮之月", NamedTextColor.DARK_RED, 2.0, 1.7, 1.12, 0.40, 0.15, 2.5, 2),
    NIGHTMARE(4, "梦魇之月", NamedTextColor.DARK_RED, 2.5, 2.0, 1.16, 0.50, 0.20, 3.0, 2),
    APOCALYPSE(5, "天启之月", NamedTextColor.DARK_PURPLE, 3.2, 2.5, 1.20, 0.60, 0.28, 4.0, 3);

    private final int level;
    private final String display;
    private final NamedTextColor color;
    private final double healthMult;
    private final double damageMult;
    private final double speedMult;
    private final double shardChance;
    private final double eliteChance;
    private final double xpMult;
    private final int packs;

    Tier(int level, String display, NamedTextColor color,
         double healthMult, double damageMult, double speedMult,
         double shardChance, double eliteChance, double xpMult, int packs) {
        this.level = level;
        this.display = display;
        this.color = color;
        this.healthMult = healthMult;
        this.damageMult = damageMult;
        this.speedMult = speedMult;
        this.shardChance = shardChance;
        this.eliteChance = eliteChance;
        this.xpMult = xpMult;
        this.packs = packs;
    }

    public int level() {
        return level;
    }

    public String display() {
        return display;
    }

    public NamedTextColor color() {
        return color;
    }

    public double healthMult() {
        return healthMult;
    }

    public double damageMult() {
        return damageMult;
    }

    public double speedMult() {
        return speedMult;
    }

    public double shardChance() {
        return shardChance;
    }

    public double eliteChance() {
        return eliteChance;
    }

    public double xpMult() {
        return xpMult;
    }

    public int packs() {
        return packs;
    }

    public static Tier ofLevel(int lv) {
        for (Tier t : values()) {
            if (t.level == lv) {
                return t;
            }
        }
        return lv < 1 ? CRIMSON : APOCALYPSE;
    }
}
