package com.arena.bloodmoon.listener;

import com.arena.bloodmoon.BloodMoonManager;
import com.arena.bloodmoon.BloodMoonPlugin;
import com.arena.bloodmoon.util.Text;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * 玩家相关：血月禁睡、进服提示。
 */
public class PlayerListener implements Listener {

    private final BloodMoonPlugin plugin;

    public PlayerListener(BloodMoonPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBed(PlayerBedEnterEvent event) {
        BloodMoonManager mgr = plugin.getMoonManager();
        if (!mgr.isActive() || !mgr.isBloodMoonWorld(event.getPlayer().getWorld())) {
            return;
        }
        if (!plugin.getConfig().getBoolean("night.disable-sleep", true)) {
            return;
        }
        event.setCancelled(true);
        Player p = event.getPlayer();
        p.sendMessage(Text.msg("血月之夜, 无人可以入眠...", NamedTextColor.DARK_RED));
        p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.7f, 0.5f);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        BloodMoonManager mgr = plugin.getMoonManager();
        Player p = event.getPlayer();
        if (mgr.isActive() && mgr.isBloodMoonWorld(p.getWorld())) {
            mgr.addParticipant(p.getUniqueId());
            if (mgr.getBar() != null) {
                mgr.getBar().addPlayer(p);
            }
            p.sendMessage(Text.msg("你在血月之夜降临此世... 小心行事。", NamedTextColor.RED));
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.6f);
        }
    }
}
