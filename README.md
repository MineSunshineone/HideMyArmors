# HideMyArmors

向其它玩家隐藏我的盔甲!

这是从我服务器的基础功能中分离出来的插件，写这个功能起因是有玩家觉得服务器的盔甲太花了，会挡住自己的皮肤，他想别人能看他的皮肤又不想失去盔甲保护。所以这个功能就出现了。

支持 Folia / Paper 1.21 及后续版本，运行和构建需要 Java 21+（26.x 服务端按其要求使用 Java 25）。需要前置 [ProtocolLib](https://github.com/dmulloy2/ProtocolLib/releases)：1.21–1.21.8 可以使用 5.4.0，1.21.9 及之后的版本请使用支持对应服务端版本的 5.5.0 开发版。

`/hma hide <head|chest|legs|feet|all>` 和 `/hma show <槽位>` 设置会保存在玩家 PDC 中，重连、重启后保留；`/hma status` 查看状态。权限隐藏与命令设置同时生效，移除命令设置不会覆盖权限隐藏。插件只处理四个盔甲槽位，主副手不隐藏，也不清除物品信息；旧的主副手隐藏权限不再生效。

你可以做一个菜单让玩家开启或关闭盔甲隐藏功能，使用你的权限管理插件给予或移除权限即可。

可用权限如下，所有权限的默认状态为关
```
hidemyarmors.hide.armors 隐藏盔甲物品
  hidemyarmors.hide.head (子权限)隐藏头盔物品
  hidemyarmors.hide.chest (子权限)隐藏胸甲物品
  hidemyarmors.hide.legs (子权限)隐藏护腿物品
  hidemyarmors.hide.feet (子权限)隐藏靴子物品
```
由于插件原理为修改服务器的实体装备包，切换隐藏设置后，需要重进游戏、切换世界或重新穿戴装备，触发新的装备包后生效。权限状态每 20 tick 在玩家自己的实体线程刷新；发包线程只读取缓存，不跨区域读取玩家状态。

出于原版限制，玩家自己的装备是由玩家的物品栏决定的，如果向自己隐藏装备，那自己也无法在背包看到装备栏的物品了。所以本插件**只能让其它玩家看不到你的装备**，不能让你自己看不到你的装备。

除此之外，本插件还提供了一个`擦除实体装备物品nbt标签`的功能，默认关闭，如有需要可到配置文件开启。重载命令为 `/hidemyarmors`

该功能可以防止作弊端查看其他玩家装备的 lore、附魔等信息，物品类型、皮革套装颜色、CustomModelData、item model、附魔光泽和头颅皮肤依然保留。1.21.5+ 的 CustomModelData 浮点、字符串、布尔和颜色数据会完整保留；该功能兼容 MagicCosmetic 魔法时装标记，时装信息将不会被擦除。

构建与单元测试：`./gradlew build`，成品为 `build/libs/HideMyArmors-1.0.4.jar`。

本地兼容测试记录见 [TESTING.md](TESTING.md)。目前验证过 Luminol 1.21.8 和 Shiroha 26.2（均启用 Folia 区域调度），后续版本仍依赖 ProtocolLib 和 NBT-API 对对应协议的支持。
