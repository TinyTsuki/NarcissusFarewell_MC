# Narcissus Farewell (水仙辞)

一个 Minecraft Forge 传送指令 Mod。

## 目录

- [Narcissus Farewell](#narcissus_farewell)
    - [目录](#目录)
    - [释义](#释义)
    - [介绍](#介绍)
    - [特性](#特性)
    - [配置说明](#配置说明)
    - [指令说明](#指令说明)
    - [注意事项](#注意事项)
    - [许可证](#许可证)

## 释义

- 水仙：象征着思念、吉祥和团圆、新的开始、美好的希望。
- 辞：告别、不接受，请求离去。
- 水仙辞：思念(home)，团圆(tpa、tph)，新的开始(back)，<del>不辞而别(tpx、tpr)</del>。
- <del>喝下水仙浓茶，然后与世长辞(feed)。</del>

## 介绍

本项目适用于Minecraft Forge服务器，实现传送至玩家/回家/返回等指令。
该Mod服务器必装，客户端可选。

## 特性

- **指定维度传送**：在进行大部分传送时可指定维度，想去哪儿就去哪儿。
- **传送至结构/群系**：可以传送至附近自然生成的结构或生物群系，麻麻再也不用担心我找不到村庄、要塞啦。
- **传送至玩家**：可以请求传送自己至某玩家，或请求将某玩家传送至自己。
- **传送至出生点**：可以传送至世界出生点或者自己的出生点。
- **自定义传送点**：可以设置自定义传送点，并使用指令/快捷键传送至该点。
- **向上、向下传送**：可以传送至上方、下方最近处的可站立的方块。
- **顶部、底部传送**：可以传送至上方最高、下方最低处的可站立的方块。
- **传送至视线方向尽头**：可以向所视方向传送直至视线被方块阻挡或到达预设的最大值处。
- **传送至服务器预设点**：可以设置服务器默认传送点，使玩家可以任意传送。
- **返回至上次离开地点**：可以返回上次传送离开的地点。
- **传送载具与跟随者**：在传送时可以连同载具与跟随的实体一起传送，麻麻再也不用担心旺财走丢啦。
- **安全的传送**：在进行任何传送时可选择安全传送，避免将玩家传送至虚空、方块、熔岩中。
- **快捷键**：在任何时间任何地点，只需按下设定的按键即可快速执行回家、返回、同意请求、拒绝请求指令。
- **虚拟权限系统**：即使不开启作弊模式，仅通过修改配置文件即可给予玩家使用某个传送指令的权限。
- **很烂的翻译**：文本描述可能存在歧义，或其表达方式不够清晰<del>（不仅仅是英文）</del>。
- **很烂的代码**：烂代码 + 疏忽的测试 = 一堆难闻的臭虫。

## TODO

- **添加指令**：屏蔽指定玩家/全部玩家的tpa/tph
- **添加指令**：自动同意/拒绝指定玩家/全部玩家的tpa/tph
- **添加配置**：传送后置代价
- **添加配置**：传送前置与后置提示
- **添加配置**：周围有怪物时无法传送
- **添加配置**：在末地时若未击败末影龙则无法传送
- **添加配置**：在指定维度无法使用某个传送指令
- **添加配置**：传送时粒子效果

## 配置说明

本地配置文件路径 `world/serverconfig/narcissus_farewell-server.toml`，其他的信息不再赘述，请参考 [默认配置文件](narcissus_farewell-server.toml) 中的注释。


## 指令说明

- **dim**：获取当前所处世界的维度ID
- **tpx**：传送至指定坐标或玩家。  
  **参数列表**：
  1. `<玩家> [<是否安全传送>]`
  2. `<坐标> [<是否安全传送>] [<维度>]`

- **tpst**：传送至指定结构或生物群系（默认安全的）。  
  **参数列表**：
  1. `<结构> [<搜索距离>] [<维度>]`
  2. `<生物群系> [<搜索距离>] [<维度>]`

- **tpa**：请求将自己传送至某个玩家，若不指定目标玩家将使用上次请求时指定的玩家。  
  **参数列表**：
  1. `[<玩家>] [<是否安全传送>]`

- **tpay**：接受将某个玩家传送至自己 的请求，若不指定参数将接受最近一次请求。  
  **参数列表**：
  1. `[<玩家>]`
  2. `[<最近的第几次请求>]`
  3. `[<请求ID>]`

- **tpan**：拒绝将某个玩家传送至自己 的请求，若不指定参数将拒绝最近一次请求。  
  **参数列表**：
  1. `[<玩家>]`
  2. `[<最近的第几次请求>]`
  3. `[<请求ID>]`

- **tph**：请求将某个玩家传送至自己，若不指定目标玩家将使用上次请求时指定的玩家。  
  **参数列表**：
  1. `[<玩家>] [<是否安全传送>]`

- **tphy**：接受将自己传送至某个玩家 的请求，若不指定参数将接受最近一次请求。  
  **参数列表**：
  1. `[<玩家>]`
  2. `[<最近的第几次请求>]`
  3. `[<请求ID>]`

- **tphn**：拒绝将自己传送至某个玩家 的请求，若不指定参数将拒绝最近一次请求。  
  **参数列表**：
  1. `[<玩家>]`
  2. `[<最近的第几次请求>]`
  3. `[<请求ID>]`

- **tpr**：传送至随机位置。  
  **参数列表**：
  1. `[<随机范围>] [<是否安全传送>] [<维度>]`

- **tpsp**：传送至玩家自己的重生点。  
  **参数列表**：
  1. `[<是否安全传送>]`

- **tpws**：传送至世界重生点。  
  **参数列表**：
  1. `[<是否安全传送>]`

- **tpt**：传送至上方最远处可站立的方块。  
  **参数列表**：
  1. `[<是否安全传送>]`

- **tpb**：传送至下方最远处可站立的方块。  
  **参数列表**：
  1. `[<是否安全传送>]`

- **tpu**：传送至上方最近处可站立的方块。  
  **参数列表**：
  1. `[<是否安全传送>]`

- **tpd**：传送至下方最近处可站立的方块。  
  **参数列表**：
  1. `[<是否安全传送>]`

- **tpv**：传送至视线方向尽头。  
  **参数列表**：
  1. `[<是否安全传送>] [<最远距离>]`

- **home**：传送至预先设定的私人传送点，每个不同维度都可设置一个默认传送点。  
  若不指定传送点名称：
  - 私人传送点仅一个时将使用该传送点；
  - 私人传送点存在多个，但默认传送点仅有一个时将使用该默认传送点；
  - 默认传送点存在多个时将使用当前维度下的默认传送点。  
    **参数列表**：
  1. `<传送点名称> [<是否安全传送>] [<维度>]`
  2. `[<是否安全传送>] [<维度>]`

- **sethome**：将玩家当前坐标加入私人传送点列表，若不指定名称将使用默认名称『home』。  
  **参数列表**：
  1. `<传送点名称> [<是否默认传送点>]`

- **delhome**：删除已设定的私人传送点，当前维度ID可使用指令查询。  
  **参数列表**：
  1. `<传送点名称> [<维度>]`

- **stage**：传送至预先设定的驿站（公共传送点），若不指定名称将传送至最近的驿站。  
  **参数列表**：
  1. `<驿站名称> [<是否安全传送>] [<维度>]`

- **setstage**：将当前或指定坐标加入驿站（公共传送点）。  
  **参数列表**：
  1. `<驿站名称> [<坐标>] [<维度>]`

- **delstage**：删除已设定的驿站（公共传送点），当前维度ID可使用指令查询。  
  **参数列表**：
  1. `<驿站名称> [<维度>]`

- **back**：返回至上次传送离开的坐标或死亡坐标。  
  **参数列表**：
  1. `[<是否安全传送>] [<传送类型>] [<维度>]`


## 注意事项

- **版本迁移**：将加载了Minecraft 1.12.2版本的该mod的存档升级到高版本Minecraft时可能会因为数据不兼容导致的各种问题。

## 许可证

MIT License

---

如有任何问题或建议，欢迎提交 Issues 或 Pull requests。
