package com.animator70.d_sound_mixins.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.orecruncher.dsurround.effects.entity.EntityEffectInfo;
import org.orecruncher.dsurround.effects.entity.ItemSwingEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 修复副手攻击（匠魂匕首、副手攻击模组）时没有挥剑/剑鸣音效的问题
 *
 * 原 {@code ItemSwingEffect#tick} 取挥动物品时写死了主手
 * {@code entity.getItemInHand(InteractionHand.MAIN_HAND)}
 * 
 * 这里在 {@code currentItem} 局部变量被赋值（swinging 分支）时，把它替换为按实际挥动的手
 * {@code entity.swingingArm} 取到的物品
 * 
 * 主手攻击时 {@code swingingArm == MAIN_HAND}，
 * 行为与原版一致；副手攻击时 {@code swingingArm == OFF_HAND}，正确取到副手武器
 * 
 * 用 {@code @ModifyVariable} 而非 {@code @Redirect} 的原因：{@code @Redirect} 的
 * target 指向 Minecraft 方法 {@code getItemInHand}，而 ForgeGradle 6 的 Mixin AP 无法
 * 为其生成混淆映射（警告 "Unable to locate method mapping"），会导致生产环境失效
 * 
 * {@code @ModifyVariable} 只操作 mod 方法 {@code tick} 的局部变量，不涉及 Minecraft
 * 方法 target，从而规避该问题
 */
@Mixin(value = ItemSwingEffect.class, remap = false)
public abstract class ItemSwingEffectMixin {
    // ordinal 按 handler 返回类型（ItemStack）来数 STORE 指令：0 = swinging 分支
    // 的 getItemInHand 赋值，1 = else 分支的 getUseItem 赋值
    @ModifyVariable(method = "tick", at = @At(value = "STORE", ordinal = 0), remap = false)
    private ItemStack dsurround$useSwingingArm(ItemStack currentItem, EntityEffectInfo info) {
        LivingEntity entity = info.getEntity();

        if (entity.swinging) {
            return entity.getItemInHand(entity.swingingArm);
        }

        return currentItem;
    }
}
