package com.arena.bloodmoon.shop;

import com.arena.bloodmoon.BloodMoonPlugin;
import com.arena.bloodmoon.item.Items;
import com.arena.bloodmoon.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 血月黑市 —— 用血月残晶购买装备。
 */
public class ShopGUI implements Listener {

    private record Offer(int price, Supplier<ItemStack> supplier) {
    }

    /** GUI 标识用 Holder。 */
    public static class ShopHolder implements InventoryHolder {
        private Inventory inventory;

        void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private final BloodMoonPlugin plugin;
    private final Map<Integer, Offer> offers = new HashMap<>();

    public ShopGUI(BloodMoonPlugin plugin) {
        this.plugin = plugin;
        offers.put(10, new Offer(64, Items::blade));
        offers.put(11, new Offer(40, Items::crimsonBow));
        offers.put(12, new Offer(48, Items::charm));
        offers.put(13, new Offer(32, Items::totem));
        offers.put(14, new Offer(8, () -> Items.clot(3)));
        offers.put(15, new Offer(24, Items::horn));
        offers.put(16, new Offer(20, () -> new ItemStack(Material.ENCHANTED_GOLDEN_APPLE)));
        offers.put(21, new Offer(10, () -> new ItemStack(Material.EXPERIENCE_BOTTLE, 16)));
        offers.put(23, new Offer(6, () -> new ItemStack(Material.ARROW, 64)));
    }

    public void open(Player player) {
        ShopHolder holder = new ShopHolder();
        Inventory inv = Bukkit.createInventory(holder, 27,
                Component.text("\u2620 血月黑市 \u2620", NamedTextColor.DARK_RED, TextDecoration.BOLD));
        holder.setInventory(inv);

        ItemStack pane = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        paneMeta.displayName(Component.text(" "));
        pane.setItemMeta(paneMeta);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, pane);
        }

        for (Map.Entry<Integer, Offer> entry : offers.entrySet()) {
            ItemStack display = entry.getValue().supplier().get().clone();
            ItemMeta meta = display.getItemMeta();
            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("\u25c8 价格: " + entry.getValue().price() + " 血月残晶", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("\u25b6 点击购买", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            display.setItemMeta(meta);
            inv.setItem(entry.getKey(), display);
        }

        // 余额指示
        ItemStack balance = Items.shard(1);
        ItemMeta bMeta = balance.getItemMeta();
        bMeta.displayName(Component.text("你的血月残晶: " + countShards(player), NamedTextColor.RED, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        bMeta.lore(List.of(Component.text("击杀血月魔物获取残晶", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        balance.setItemMeta(bMeta);
        inv.setItem(22, balance);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 0.7f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null
                || !(event.getClickedInventory().getHolder() instanceof ShopHolder)) {
            return;
        }
        Offer offer = offers.get(event.getSlot());
        if (offer == null) {
            return;
        }
        int have = countShards(player);
        if (have < offer.price()) {
            player.sendMessage(Text.msg("残晶不足! 需要 " + offer.price() + " 枚, 你只有 " + have + " 枚。", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
            return;
        }
        removeShards(player, offer.price());
        ItemStack bought = offer.supplier().get();
        var leftover = player.getInventory().addItem(bought);
        for (ItemStack stack : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), stack);
        }
        player.sendMessage(Text.msg("交易达成。血月黑市感谢你的惠顾...", NamedTextColor.GOLD));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
        open(player); // 刷新余额显示
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ShopHolder) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------- 货币操作

    public int countShards(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && Items.is(item, Items.ID_SHARD)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    public void removeShards(Player player, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || !Items.is(item, Items.ID_SHARD)) {
                continue;
            }
            int take = Math.min(item.getAmount(), remaining);
            remaining -= take;
            if (take >= item.getAmount()) {
                player.getInventory().setItem(i, null);
            } else {
                item.setAmount(item.getAmount() - take);
            }
        }
    }
}
