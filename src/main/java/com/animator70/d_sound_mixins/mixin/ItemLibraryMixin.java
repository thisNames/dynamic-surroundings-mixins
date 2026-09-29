package com.animator70.d_sound_mixins.mixin;

// Minecraft 类
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

// Dynamic Surroundings 类
import org.orecruncher.dsurround.config.ItemClassType;
import org.orecruncher.dsurround.config.libraries.ITagLibrary;
import org.orecruncher.dsurround.config.libraries.impl.ItemLibrary;
import org.orecruncher.dsurround.tags.ItemEffectTags;

// SpongeMixin 类
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 修复 {@code ItemLibrary#resolveClassType} 对非原版物品类的误判。
 *
 * 原方法仅用 {@code instanceof} 判断原版物品类（SwordItem、AxeItem、TieredItem 等）
 * 而匠魂 3 等模组的工具/武器继承了原版 {@code TieredItem}（而非 {@code SwordItem}）
 * 因此会被误判为 {@code TOOL}，丢失剑鸣音效。
 * 
 * 本 Mixin 在方法开头（HEAD）依据物品标签（映射到 {@code #minecraft:swords}、
 * {@code #c:swords} 等）优先分类：命中即返回对应类型，未命中则回退到原版 instanceof 逻辑
 * 
 */
@Mixin(value = ItemLibrary.class, remap = false)
public abstract class ItemLibraryMixin {
    // 原 {@code ItemLibrary} 的标签库字段，用于判断物品是否属于某个效果标签
    @Shadow(remap = false)
    private ITagLibrary tagLibrary;

    /**
     * 在 {@code resolveClassType} 开头按标签优先分类
     * 命中 {@code ItemEffectTags} 中的标签（或原版镐子标签）时直接覆盖返回值
     * 未命中则不做任何事，让原方法继续走 {@code instanceof} 判断
     */
    @Inject(method = "resolveClassType", at = @At("HEAD"), cancellable = true, remap = false)
    private void dsurround$classifyByTags(ItemStack stack, CallbackInfoReturnable<ItemClassType> cir) {
        // 物品类型
        ItemClassType resolved = null;

        // 优先判断标签逻辑（前置拦截原模组的继承判断逻辑、如果未命中才走原模组逻辑）
        if (this.tagLibrary.is(ItemEffectTags.AXES, stack)) {
            // 斧
            resolved = ItemClassType.AXE;

        } else if (this.tagLibrary.is(ItemEffectTags.BOWS, stack)) {
            // 弓
            resolved = ItemClassType.BOW;

        } else if (this.tagLibrary.is(ItemEffectTags.CROSSBOWS, stack)) {

            // 驽
            resolved = ItemClassType.CROSSBOW;
        } else if (this.tagLibrary.is(ItemEffectTags.SHIELDS, stack)) {

            // 盾
            resolved = ItemClassType.SHIELD;
        } else if (this.tagLibrary.is(ItemEffectTags.SWORDS, stack)) {

            // 剑
            resolved = ItemClassType.SWORD;
        } else if (this.tagLibrary.is(ItemEffectTags.TOOLS, stack) || stack.is(ItemTags.PICKAXES)) {

            // 标签
            // ItemEffectTags.TOOLS 覆盖锄/铲/鱼竿；镐子未在 effects/tools.json 中引用
            // 这里直接检查原版 #minecraft:pickaxes 标签以覆盖匠魂镐子
            resolved = ItemClassType.TOOL;
        }

        // 非空判断
        if (resolved != null) {
            cir.setReturnValue(resolved);
        }
    }
}
