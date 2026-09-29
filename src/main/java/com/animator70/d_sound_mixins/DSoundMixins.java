package com.animator70.d_sound_mixins;

// Forge 类
import net.minecraftforge.fml.common.Mod;

/**
 * Dynamic Surroundings Mixins —— 一个针对 Dynamic Surroundings (Expanded) 的运行时补丁模组
 *
 * 解决的问题：匠魂 3（Tinkers' Construct 3）等模组的工具/武器继承了原版
 * {@code TieredItem} 而非 {@code SwordItem}，被 Dynamic Surroundings 误判为
 * "工具(TOOL)"，
 * 从而丢失剑鸣音效。
 *
 * 本模组通过 Mixin（见 {@code mixin.ItemLibraryMixin}）在
 * {@code ItemLibrary#resolveClassType} 的 instanceof 判断之前，依据物品标签
 * （如 {@code #minecraft:swords}）优先分类来修复。
 * 
 * Mixin 配置通过 {@code META-INF/MANIFEST.MF} 的 {@code MixinConfigs} 属性自动加载，
 * 因此本类无需任何初始化逻辑。
 * 
 */
@Mod(DSoundMixins.MODID)
public class DSoundMixins {
    // 模组 ID
    public static final String MODID = "d_sound_mixins";
}
