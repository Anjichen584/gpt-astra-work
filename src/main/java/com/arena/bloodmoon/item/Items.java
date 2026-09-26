package com.arena.bloodmoon.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 血月自定义物品工厂。物品通过 PDC 标签识别。
 */
public final class Items {

    public static final String ID_SHARD = "shard";
    public static final String ID_BLADE = "blade";
    public static final String ID_BOW = "crimson_bow";
    public static final String ID_CHARM = "charm";
    public static final String ID_TOTEM = "totem";
    public static final String ID_CLOT = "clot";
    public static final String ID_HORN = "horn";

    private static NamespacedKey itemKey;

    private Items() {
    }

    public static void init(Plugin plugin) {
        itemKey = new NamespacedKey(plugin, "bm_item");
    }

    public static String idOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
    }

    public static boolean is(ItemStack item, String id) {
        return id.equals(idOf(item));
    }

    // ------------------------------------------------------------- 物品定义

    /** 血月残晶 —— 货币。 */
    public static ItemStack shard(int amount) {
        ItemStack item = base(Material.ECHO_SHARD, ID_SHARD,
                "血月残晶", NamedTextColor.RED,
                List.of("血月魔物凝结出的赤红结晶,", "是血月黑市唯一认可的货币。", "", "\u25b6 /bm shop 打开血月黑市"));
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return glint(item);
    }

    /** 血月之刃 —— 吸血 + 对亡灵增伤。 */
    public static ItemStack blade() {
        ItemStack item = base(Material.NETHERITE_SWORD, ID_BLADE,
                "血月之刃", NamedTextColor.DARK_RED,
                List.of("以血月残晶淬炼的魔剑。", "", "\u25c6 嗜血: 攻击回复 15% 伤害的生命", "\u25c6 猎魔: 血月期间对亡灵 +30% 伤害"));
        item.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
        item.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
        item.addUnsafeEnchantment(Enchantment.MENDING, 1);
        return item;
    }

    /** 猩红战弓 —— 血羽箭。 */
    public static ItemStack crimsonBow() {
        ItemStack item = base(Material.BOW, ID_BOW,
                "猩红战弓", NamedTextColor.RED,
                List.of("弓弦以血月魔物的筋腱织成。", "", "\u25c6 血羽箭: 箭矢携带血雾轨迹", "\u25c6 灼魂: 命中魔物点燃并施加凋零"));
        item.addUnsafeEnchantment(Enchantment.POWER, 4);
        item.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
        return item;
    }

    /** 守夜人护符 —— 血月期间被动减伤。 */
    public static ItemStack charm() {
        return glint(base(Material.NETHER_STAR, ID_CHARM,
                "守夜人护符", NamedTextColor.GOLD,
                List.of("古老守夜人结社的信物。", "", "\u25c6 只需放在背包中:", "\u25c6 血月期间获得 抗性提升 + 防火")));
    }

    /** 血月图腾 —— 复活时爆发冲击。 */
    public static ItemStack totem() {
        return base(Material.TOTEM_OF_UNDYING, ID_TOTEM,
                "血月图腾", NamedTextColor.DARK_RED,
                List.of("以禁忌之血浸染的不死图腾。", "", "\u25c6 触发时释放血爆:", "\u25c6 击退并重创周围魔物, 自身获得回复"));
    }

    /** 血浆凝块 —— 应急治疗。 */
    public static ItemStack clot(int amount) {
        ItemStack item = base(Material.GLISTERING_MELON_SLICE, ID_CLOT,
                "血浆凝块", NamedTextColor.RED,
                List.of("凝固的血月精华, 散发着铁锈味。", "", "\u25b6 右键食用: 恢复 4\u2764 并获得再生"));
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return item;
    }

    /** 猎人号角 —— 显形周围魔物。 */
    public static ItemStack horn() {
        return base(Material.GOAT_HORN, ID_HORN,
                "猎人号角", NamedTextColor.YELLOW,
                List.of("守夜人猎队的示警号角。", "", "\u25b6 右键吹响: 30 格内魔物发光显形", "\u25b6 冷却: 60 秒"));
    }

    public static ItemStack byId(String id) {
        return switch (id) {
            case ID_SHARD -> shard(1);
            case ID_BLADE -> blade();
            case ID_BOW -> crimsonBow();
            case ID_CHARM -> charm();
            case ID_TOTEM -> totem();
            case ID_CLOT -> clot(1);
            case ID_HORN -> horn();
            default -> null;
        };
    }

    // ------------------------------------------------------------- 工具

    private static ItemStack base(Material material, String id, String name,
                                  NamedTextColor color, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(Component.text(line, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack glint(ItemStack item) {
        item.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }
}
