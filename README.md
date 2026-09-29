# Dynamic Surroundings Mixins

一个针对【Dynamic Surroundings (Expanded)】[Gitbub](https://github.com/astryxion/Dynamic-Surroundings)、[CurseForge](https://www.curseforge.com/minecraft/mc-mods/dynamic-surroundings-expanded) 的 **Mixin 补丁模组**，用于修复它在 Minecraft Forge 1.20.1 上无法正确识别部分模组工具/武器音效的问题。

> 本模组**不修改** Dynamic Surroundings 的任何源码，而是通过 Mixin 在运行时注入补丁。

---

## 做了什么

修复「匠魂 3（Tinkers' Construct 3）」等模组的工具/武器**没有切换音效、剑类没有剑鸣音效**的问题。

具体来说：让 Dynamic Surroundings 能够根据**物品标签**（如 `#minecraft:swords`）正确地把物品归类为「剑 / 斧 / 工具…」，而不是只靠继承关系判断。

---

## 为什么会有这个问题（根本原因）

Dynamic Surroundings 的 `ItemLibrary#resolveClassType()` 判定物品类型时，**只用了 `instanceof` 判断原版物品类**：

```java
if (item instanceof SwordItem) return SWORD;          // 剑 → 剑鸣音效
if (item instanceof TieredItem) return TOOL;          // 工具 → 工具音效
...
return NONE;
```

它**完全没有使用物品标签**（`#minecraft:swords` 等）。

而匠魂 3 的剑的继承关系是：

```
ModifiableSwordItem → ModifiableItem → TieredItem（原版类）
```

它**不是** `SwordItem`，而是 `TieredItem`。因此：

| 模组的剑 | 继承的类 | 判定结果 | 音效 |
|---|---|---|---|
| 冰火传说 / 暮色森林 | `SwordItem` | `SWORD` | 有剑鸣音效 |
| 匠魂 3 | `TieredItem` | `TOOL` | 有工具音效，没有剑鸣 |

所以匠魂的剑「看起来没声音」，其实是**被误判成了工具**。

> 讽刺的是，Dynamic Surroundings 自己早就定义好了标签（`ItemEffectTags.SWORDS` 等常量，以及映射到 `#minecraft:swords` / `#c:swords` 的 `effects/swords.json`），**只是忘了在 `resolveClassType` 里用它们**。

---

## 修复

本模组通过 Mixin 注入到 `ItemLibrary#resolveClassType` 的**方法开头（HEAD）**，在 `instanceof` 判断**之前**，按标签优先分类：

```java
@Mixin(value = ItemLibrary.class, remap = false)
public abstract class ItemLibraryMixin {

    @Shadow(remap = false)
    private ITagLibrary tagLibrary;

    @Inject(method = "resolveClassType", at = @At("HEAD"), cancellable = true, remap = false)
    private void dsurround$classifyByTags(ItemStack stack, CallbackInfoReturnable<ItemClassType> cir) {
        ItemClassType resolved = null;
        if      (tagLibrary.is(ItemEffectTags.AXES, stack))       resolved = AXE;
        else if (tagLibrary.is(ItemEffectTags.BOWS, stack))       resolved = BOW;
        else if (tagLibrary.is(ItemEffectTags.CROSSBOWS, stack))  resolved = CROSSBOW;
        else if (tagLibrary.is(ItemEffectTags.SHIELDS, stack))    resolved = SHIELD;
        else if (tagLibrary.is(ItemEffectTags.SWORDS, stack))     resolved = SWORD;   // 关键：剑优先于 TieredItem
        else if (tagLibrary.is(ItemEffectTags.TOOLS, stack)
              || stack.is(ItemTags.PICKAXES))                     resolved = TOOL;

        if (resolved != null) {
            cir.setReturnValue(resolved);   // 命中标签 → 直接接管返回值
        }
        // 没命中 → 什么都不做，原 instanceof 逻辑照常执行
    }
}
```

工作方式（**前置拦截**）：

1. 方法被调用时，先执行上面的标签判断；
2. **命中标签** → `setReturnValue` 直接返回对应类型，原 `instanceof` 逻辑跳过；
3. **没命中标签** → 什么都不做，原 `instanceof` 逻辑原样执行。

因此：

- 对原版物品，标签判断结果和原 `instanceof` 一致，**行为不变**；
- 对匠魂这类「继承 `TieredItem` 却有 `#minecraft:swords` 标签」的物品，被正确归类为剑；
- 原模组的其它功能（书、药水、环境音、脚步声等）**完全不受影响**。

### 额外补丁

原模组的 `effects/tools.json` 漏掉了 `#minecraft:pickaxes`，导致镐子无法被识别为工具。本模组用 `stack.is(ItemTags.PICKAXES)` 直接检查原版标签补齐。

---

## 特性

- 修复匠魂 3 等模组的工具/武器切换音效与剑鸣音效
- Mixin 补丁，**不修改** Dynamic Surroundings 源码
- 对原版物品行为零影响
- 可配合数据包/资源包：给物品加 `#minecraft:swords` 等标签即可扩展分类
- 纯客户端模组（`clientSideOnly`）

---

## 前置依赖

| 依赖 | 必需 | 说明 |
|---|---|---|
| [Dynamic Surroundings (Expanded)](https://modrinth.com/mod/dynamicsurroundingsfabric) 1.20.1 | 必须 | 被修补的目标模组 |
| [Architectury API](https://modrinth.com/mod/architectury-api) 9.2.14+ | 必须 | Dynamic Surroundings 的前置 |
| Tinkers' Construct 3 + Mantle | 可选（验证用） | 本模组修复的目标场景，不装也不影响 |

---

## 构建

在 `libs/` 目录放置以下 jar（详见 `build.gradle`）：

```
libs/
├── dynamicsurroundingsforge-1.20.1-1.3.1.jar
├── architectury-9.2.14-forge.jar
├── Mantle-1.20.1-1.11.117.jar          （可选，验证用）
└── TConstruct-1.20.1-3.11.2.166.jar     （可选，验证用）
```

然后：

```bash
gradlew build       # 构建
gradlew runClient   # 开发环境运行
```

---

## 许可证

LGPL
