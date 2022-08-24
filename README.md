# LoliYouWant

> 「你想要的插件」系列作品

[![](https://shields.io/github/downloads/MrXiaoM/LoliYouWant/total)](https://github.com/MrXiaoM/LoliYouWant/releases) [![](https://img.shields.io/badge/mirai--console-2.11-blue)](https://github.com/mamoe/mirai) [![](https://img.shields.io/badge/MiraiForum-post-yellow)](https://mirai.mamoe.net/topic/1515)

刑，真是太刑了。

## 简介

论坛又一大涩图插件 (bushi)。本插件使用 [Lolibooru](https://lolibooru.moe) 的 API，~~全是萝莉图，萝莉控狂喜~~。

由于该站点在大陆内部分地区无法访问 (我在我家的机子和手机都可以访问，但在学校，该站点被 SNI 阻断了)，请自备代理或者使用本地反向代理来绕过 SNI 阻断，在这方面我不会提供任何帮助。

其他功能详见配置文件。

## 安装

到 [Releases](https://github.com/MrXiaoM/LoliYouWant/releases) 下载插件并放入 plugins 文件夹进行安装

> 2.11 或以上下载 LoliYouWant-*.mirai2.jar
>
> 2.11 以下下载 LoliYouWant-legacy-*.mirai.jar

安装完毕后，编辑配置文件作出你想要的修改。在控制台执行 `/luw reload` 重载配置即可~

配置文件内有详细的注释，详见 [源码](src/main/kotlin/LoliConfig.kt)  
> 配置文件路径是  
> (2.11 或以上) `./config/top.mrxiaom.loliyouwant/config.yml`  
> (2.11 以下) `./config/Loli You Want/config.yml`
>

## 命令

| 命令          | 解释     |
|-------------|--------|
| /luw reload | 重载配置文件 |

## 权限
| 权限                                      | 解释         |
|-----------------------------------------|------------|
| top.mrxiaom.loliyouwant:random          | 允许使用随机图片功能 |
| top.mrxiaom.loliyouwant:reload          | 允许重载插件配置文件 |
| top.mrxiaom.loliyouwant:bypass.cooldown | 无视冷却时间     |

> 提示：可通过以下命令给权限  
> `/perm permit g群号 权限`  
> `/perm permit g群号.群友QQ号 权限`  
> `/perm permit m群友QQ号 权限`

## 用法

发送关键词 `来只萝莉` 即可，剩下的详见配置文件。

## 编译

```
./gradlew buildPlugin buildPluginLegacy
```
