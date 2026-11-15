# Aeronautics Armor vs Projectile

为 CBC 火炮弹丸加入穿甲等级与方块防护等级判定。

## 计算规则

- 穿甲等级和防护等级为 1–6 级。未登记的弹丸默认 1 级；带引信的大炮弹（`FuzedBigCannonProjectile`）默认 3 级。
- 可选兼容 CBC More Shells（cbcmoreshells）：标准炮的伤害乘数达到 1.5 时额外 +1 级。该兼容按类名反射探测，未安装或版本不匹配时自动跳过，不产生依赖。
- 显式登记的方块等级优先于配置等级；未登记方块使用 CBC armor provider 的 `toughness` 映射为 1–6 级。
- 未登记方块按 `tier2Hardness` 至 `tier6Hardness` 配置阈值映射等级；这些键名为兼容旧配置保留，实际比较的是 CBC `toughness`。默认阈值为 4.0、8.0、2147483647.0、2147483647.0、2147483647.0。
- 每低于防护等级 1 级，有效伤害减半；穿甲等级相等或更高时不衰减。
- 进入穿透判定的原始伤害统一乘以全局伤害系数（默认 0.1，可配置 `globalDamageScale`），再应用等级差倍率；该系数作用于炮弹命中、机炮命中和爆炸判定。
- 击穿概率为 `clamp(有效伤害 / CBC toughness, 0, 1)`。toughness 是概率计算使用的方块耐久；其值小于或等于 0 时，正伤害必定击穿。
- 命中或爆炸判定失败时方块保持完整，不累积方块损伤。爆炸原始伤害按爆炸半径随目标距离衰减。
- 高爆弹的爆炸也使用弹丸实体类型登记的穿甲等级。

## 配置

常见配置文件为 `config/aeronautics_armor_vs_projectile-common.toml`：

```toml
projectilePierceLevels = [
    "createbigcannons:solid_shot=4"
]
blockProtectionLevels = [
    "minecraft:obsidian=5"
]
tier2Hardness = 4.0
tier3Hardness = 8.0
tier4Hardness = 2147483647.0
tier5Hardness = 2147483647.0
tier6Hardness = 2147483647.0
globalDamageScale = 0.1
debugLogging = false
```

等级配置格式为 `namespace:id=level`，等级必须是 1–6。无效或未知的注册 ID 会在日志中报告并忽略。
阈值必须从 2 级到 6 级保持非递减；匹配时优先选择最高的满足阈值等级。等级映射和概率计算使用同一 CBC `toughness` 值。开启 `debugLogging` 可记录每次判定的等级、有效伤害、耐久、概率及结果。
开启后，`latest.log`/`debug.log` 会输出原始伤害、等级差、等级伤害倍率、有效伤害公式、CBC toughness、穿透概率公式（含百分比）、随机值和最终结果。开发环境的 `run/client/config/aeronautics_armor_vs_projectile-common.toml` 默认开启该日志；其他实例需设置 `debugLogging = true`。

## Java API

其他模组可直接按实体类型和方块注册等级：

```java
ArmorPenetrationRegistry.registerProjectile(projectileEntityType, 4);
ArmorPenetrationRegistry.registerBlock(block, 3);
```

Java API 登记优先于配置登记。未登记弹丸为 1 级；未登记方块按 CBC toughness 自动映射。也可通过 `getPierceLevel`、`getProtectionLevel` 和 `getDurability` 查询当前等级与韧性。

属性也可直接覆盖：

```java
ArmorPenetrationRegistry.setProjectileMassMultiplier(projectileEntityType, 1.5F);
ArmorPenetrationRegistry.setProjectileBonusMomentumMultiplier(projectileEntityType, 1.2F);
ArmorPenetrationRegistry.setBlockToughness(block, 18.0);
ArmorPenetrationRegistry.setBlockHardness(block, 12.0);
```
注：projectileEntityType为string

## 查看方块信息

瞄准方块并持有 Create Big Cannons 的方块装甲检查工具时，goggles 提示会显示该方块的防护等级和 toughness（耐久）。

## KubeJS

安装 KubeJS 后可在 startup 脚本中登记等级，或按模板创建新的方块/炮弹。新对象要在注册期创建，使用 `armor_clone` 或 `projectile_clone` 类型：

```js
StartupEvents.registry('block', event => {
  event.create('reinforced_steel', 'armor_clone')
    .template('minecraft:iron_block')
    .name('Reinforced Steel')
    .protectionLevel(4)
    .inheritTexture()

  event.create('heavy_solid_shot', 'projectile_clone')
    .template('createbigcannons:solid_shot')
    .name('Heavy Solid Shot')
    .penetrationLevel(4)
    .texture('my_mod:block/heavy_solid_shot')
})
```

`.inheritTexture()` 沿用模板方块模型；`.texture('namespace:block/path')` 使用指定材质覆盖模板模型的常见材质槽。`armor_clone` 会复制模板方块的材质属性，但创建的是普通方块，不会复制模板特有的交互/方块实体逻辑。`projectile_clone` 目前要求 CBC 的 inert projectile block 模板（例如实心弹），并沿用模板弹丸实体类型；每个新炮弹实体仍使用克隆对象自己的穿甲等级和模型。

也可按注册 ID 直接设置已存在对象的等级：

```js
AAVP.registerProjectile('createbigcannons:solid_shot', 4)
AAVP.registerBlock('minecraft:obsidian', 5)
```

直接登记调用与 Java API 一样，在游戏运行期生效；Java/KubeJS 显式登记优先于配置文件中的等级。

### 编辑炮弹动量与方块属性

`AAVP` 可直接调整已有炮弹的动量参数、方块韧性与硬度，以及全局伤害系数，无需新建对象。

炮弹动量公式为：

$$
\text{momentum} = \text{mass} \times \text{incidentVel} \times \text{bonusMomentum}
$$

其中 `mass` 和 `bonusMomentum` 可按炮弹实体类型缩放：

```js
// mass 倍率（默认 1.0）：直接缩放炮弹质量，影响动量与穿透
AAVP.setProjectileMassMultiplier('createbigcannons:solid_shot', 1.5)

// bonusMomentum 倍率（默认 1.0）：缩放 CBC 的速度加成系数
AAVP.setProjectileBonusMomentumMultiplier('createbigcannons:solid_shot', 1.2)

// 方块韧性覆盖（默认使用 CBC toughness），影响防护等级映射与击穿概率
AAVP.setBlockToughness('minecraft:iron_block', 18.0)

// 方块硬度覆盖（默认使用 CBC hardness），影响 CBC 硬度惩罚与碎裂判定
AAVP.setBlockHardness('minecraft:iron_block', 12.0)

// 全局伤害系数（默认 0.1），作用于命中与爆炸的原始伤害
AAVP.setGlobalDamageScale(0.15)
```

查询与重置：

```js
AAVP.getProjectileMassMultiplier('createbigcannons:solid_shot')          // 1.5
AAVP.getProjectileBonusMomentumMultiplier('createbigcannons:solid_shot') // 1.2
AAVP.hasBlockToughnessOverride('minecraft:iron_block')                   // true
AAVP.hasBlockHardnessOverride('minecraft:iron_block')                    // true
AAVP.getGlobalDamageScale()                                              // 0.15
AAVP.getProjectileLevel('createbigcannons:solid_shot')                   // 已登记的穿甲等级
AAVP.getBlockLevel('minecraft:iron_block')                               // 已登记的防护等级
AAVP.resetProjectileMassMultiplier('createbigcannons:solid_shot')
AAVP.resetProjectileBonusMomentumMultiplier('createbigcannons:solid_shot')
AAVP.resetBlockToughness('minecraft:iron_block')
AAVP.resetBlockHardness('minecraft:iron_block')
```

参数会校验：倍率、韧性、硬度、全局伤害系数必须为有限非负数；未知 ID 会直接报错。`mass`/`bonusMomentum` 缩放对大炮弹和机炮都生效，且不影响 `massLost` 等 CBC 内部质量统计。韧性覆盖优先于 CBC 数据，但显式登记的防护等级仍优先于韧性映射。
