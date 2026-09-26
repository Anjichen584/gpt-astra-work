package com.arena.bloodmoon;

import com.arena.bloodmoon.boss.BossManager;
import com.arena.bloodmoon.command.BloodMoonCommand;
import com.arena.bloodmoon.event.EventDirector;
import com.arena.bloodmoon.item.ItemListener;
import com.arena.bloodmoon.item.Items;
import com.arena.bloodmoon.item.Recipes;
import com.arena.bloodmoon.listener.CombatListener;
import com.arena.bloodmoon.listener.MobAbilities;
import com.arena.bloodmoon.listener.MobSpawnListener;
import com.arena.bloodmoon.listener.PlayerListener;
import com.arena.bloodmoon.mob.EliteFactory;
import com.arena.bloodmoon.mob.MobBuffer;
import com.arena.bloodmoon.shop.ShopGUI;
import com.arena.bloodmoon.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 血月 BloodMoon - 主插件类。
 */
public final class BloodMoonPlugin extends JavaPlugin {

    private static BloodMoonPlugin instance;

    private BloodMoonManager moonManager;
    private StatsManager statsManager;
    private EventDirector eventDirector;
    private BossManager bossManager;
    private ShopGUI shopGUI;
    private MobAbilities mobAbilities;
    private CombatListener combatListener;

    public static BloodMoonPlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        Items.init(this);
        MobBuffer.init(this);
        EliteFactory.init(this);

        statsManager = new StatsManager(this);
        bossManager = new BossManager(this);
        eventDirector = new EventDirector(this);
        moonManager = new BloodMoonManager(this);
        shopGUI = new ShopGUI(this);
        mobAbilities = new MobAbilities(this);
        combatListener = new CombatListener(this);

        Recipes.register(this);

        Bukkit.getPluginManager().registerEvents(new MobSpawnListener(this), this);
        Bukkit.getPluginManager().registerEvents(mobAbilities, this);
        Bukkit.getPluginManager().registerEvents(combatListener, this);
        Bukkit.getPluginManager().registerEvents(new ItemListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(shopGUI, this);
        Bukkit.getPluginManager().registerEvents(bossManager, this);
        Bukkit.getPluginManager().registerEvents(eventDirector, this);

        BloodMoonCommand cmd = new BloodMoonCommand(this);
        PluginCommand pc = getCommand("bloodmoon");
        if (pc != null) {
            pc.setExecutor(cmd);
            pc.setTabCompleter(cmd);
        }

        moonManager.startTicking();
        mobAbilities.startTicking();
        statsManager.startAutoSave();

        getLogger().info("血月已在天际酝酿... BloodMoon 已启用。");
    }

    @Override
    public void onDisable() {
        if (moonManager != null) {
            moonManager.shutdown();
        }
        if (statsManager != null) {
            statsManager.save();
        }
        getLogger().info("血月退散，黎明将至。");
    }

    public BloodMoonManager getMoonManager() {
        return moonManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public EventDirector getEventDirector() {
        return eventDirector;
    }

    public BossManager getBossManager() {
        return bossManager;
    }

    public ShopGUI getShopGUI() {
        return shopGUI;
    }

    public CombatListener getCombatListener() {
        return combatListener;
    }
}
