package com.arena.bloodmoon.item;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

/**
 * 血晶合成配方。
 */
public final class Recipes {

    private Recipes() {
    }

    public static void register(Plugin plugin) {
        RecipeChoice shard = new RecipeChoice.ExactChoice(Items.shard(1));

        // 血月之刃: 8 血晶 + 下界合金剑
        ShapedRecipe blade = new ShapedRecipe(new NamespacedKey(plugin, "blade"), Items.blade());
        blade.shape("SSS", "SBS", "SSS");
        blade.setIngredient('S', shard);
        blade.setIngredient('B', new RecipeChoice.MaterialChoice(Material.NETHERITE_SWORD));
        Bukkit.addRecipe(blade);

        // 猩红战弓: 4 血晶 + 弓
        ShapedRecipe bow = new ShapedRecipe(new NamespacedKey(plugin, "crimson_bow"), Items.crimsonBow());
        bow.shape(" S ", "SBS", " S ");
        bow.setIngredient('S', shard);
        bow.setIngredient('B', new RecipeChoice.MaterialChoice(Material.BOW));
        Bukkit.addRecipe(bow);

        // 守夜人护符: 8 血晶 + 金块
        ShapedRecipe charm = new ShapedRecipe(new NamespacedKey(plugin, "charm"), Items.charm());
        charm.shape("SSS", "SGS", "SSS");
        charm.setIngredient('S', shard);
        charm.setIngredient('G', new RecipeChoice.MaterialChoice(Material.GOLD_BLOCK));
        Bukkit.addRecipe(charm);

        // 血月图腾: 4 血晶 + 不死图腾
        ShapedRecipe totem = new ShapedRecipe(new NamespacedKey(plugin, "totem"), Items.totem());
        totem.shape(" S ", "STS", " S ");
        totem.setIngredient('S', shard);
        totem.setIngredient('T', new RecipeChoice.MaterialChoice(Material.TOTEM_OF_UNDYING));
        Bukkit.addRecipe(totem);

        // 血浆凝块 x2: 1 血晶 + 闪烁西瓜片
        ShapedRecipe clot = new ShapedRecipe(new NamespacedKey(plugin, "clot"), Items.clot(2));
        clot.shape("S", "M");
        clot.setIngredient('S', shard);
        clot.setIngredient('M', new RecipeChoice.MaterialChoice(Material.GLISTERING_MELON_SLICE));
        Bukkit.addRecipe(clot);

        // 猎人号角: 3 血晶 + 山羊角
        ShapedRecipe horn = new ShapedRecipe(new NamespacedKey(plugin, "horn"), Items.horn());
        horn.shape("SSS", " H ");
        horn.setIngredient('S', shard);
        horn.setIngredient('H', new RecipeChoice.MaterialChoice(Material.GOAT_HORN));
        Bukkit.addRecipe(horn);
    }
}
