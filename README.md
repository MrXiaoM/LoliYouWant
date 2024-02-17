# LoliYouWant

> 「你想要的插件」系列作品

[![](https://shields.io/github/downloads/MrXiaoM/LoliYouWant/total)](https://github.com/MrXiaoM/LoliYouWant/releases) [![](https://img.shields.io/badge/mirai--console-2.15.0-blue)](https://github.com/mamoe/mirai) [![](https://img.shields.io/badge/MiraiForum-post-yellow)](https://mirai.mamoe.net/topic/1515)

刑，真是太刑了。

## 简介

论坛又一大涩图插件 (bushi)。本插件使用 [Lolibooru](https://lolibooru.moe) 的 API，~~全是萝莉图，萝莉控狂喜~~。

由于该站点在大陆内部分地区无法访问 (我在我家的机子和手机都可以访问，但在学校，该站点被 SNI 阻断了)，请自备代理或者使用本地反向代理来绕过 SNI 阻断，在这方面我不会提供任何帮助。

其他功能详见配置文件。

0.3.0 新增搜索功能，用户输入的关键词将会使用[腾讯交互翻译](https://yi.qq.com/zh-CN/index)进行 中译日、日译英 然后在 Lolibooru 查找相关 Tags，再获取图片。

腾讯交互翻译在日文人名这方面翻译得相当不准确，如果是直接中译英，则结果是逐字翻译成英文，故先中译日再日译英。  
目前测试`和泉纱雾`会以英文习惯把姓名倒过来 (此问题已经过特殊处理解决)，`绪山真寻`则是根本翻译不出来。这些错误导致翻译结果并不符合tag命名规则，在之后的版本会经过特殊处理尽量规避。

## 安装

到 [Releases](https://github.com/MrXiaoM/LoliYouWant/releases) 下载插件并放入 plugins 文件夹进行安装

安装完毕后，编辑配置文件作出你想要的修改。在控制台执行 `/luwadmin reload` 重载配置即可~

配置文件内有详细的注释，详见 [源码](src/main/kotlin/LoliConfig.kt)  
> 配置文件路径是  
> `./config/top.mrxiaom.loliyouwant/config.yml`

0.3.0 配置文件大改，如果你是从 0.2.x 升级到 0.3.0 的，请在升级前**备份配置文件**，升级后修改新加的选项。  
新版本将一些配置移动了位置，比如 `/loli get` 命令的各种回复消息、超时时间、是否保存图片等通通移到了 `command` 块。  
`keywords` 配置没有改动。

## 命令

| 命令                        | 解释                                                                       |
|---------------------------|--------------------------------------------------------------------------|
| /loli get <[数量] tags>     | 根据tags获取N张图片，不输入数量则默认为1张，多个tag用空格分开，tag只能为英文，tag中的空格用下划线代替，不支持模糊搜索       |
| /loli search <[数量] 搜索关键词> | 根据关键词搜索到最符合、图片数量最多的 tags，并根据tags获取N张图片，不输入数量则默认为1张。关键词最好用英文，中文会自动翻译但不准确  |
| /loliadmin reload         | 重载配置文件                                                                   |
| /loliadmin keywords       | 查看已载入的关键词配置                                                              |

## 权限

| 权限                                               | 解释                                 |
|--------------------------------------------------|------------------------------------|
| top.mrxiaom.loliyouwant:command.loliyouwant      | 允许使用 /loli 命令                      |
| top.mrxiaom.loliyouwant:command.loliyouwantadmin | 允许使用 /loliadmin 命令                 |
| top.mrxiaom.loliyouwant:random                   | 允许使用关键词随机图片功能                      |
| top.mrxiaom.loliyouwant:search                   | 允许使用关键词搜索功能 (不是 /loli search 命令权限) |
| top.mrxiaom.loliyouwant:bypass.cooldown          | 无视冷却时间                             |

random 和 search 权限，即可以给群，也可以给群员，也可以给好友。如果把权限给到群，群内所有人均可使用关键词

> 提示：可通过以下命令给权限  
> ```
> /perm permit g群号 权限  
> /perm permit g群号.群友QQ号 权限  
> /perm permit m群友QQ号 权限  
> /perm permit QQ号 权限
> ```
> 
## 用法

给予 random 权限后，发送 `@机器人 来只萝莉` 即可，空格可不加，剩下的详见配置文件。

给予 search 权限后，发送 `@机器人 来点绪山真寻` 即可搜索图片。在配置文件找不到关键词时，以 `来点` 开头将会触发搜索。

## 编译

```
./gradlew buildPlugin
```

## 捐助

前往 [爱发电](https://afdian.net/a/mrxiaom) 捐助我。
