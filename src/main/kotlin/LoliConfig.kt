package top.mrxiaom.loliyouwant

import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.ValueName
import net.mamoe.mirai.console.data.value

object LoliConfig : ReadOnlyPluginConfig("config") {

    @ValueName("api-base-url")
    @ValueDescription("Lolibooru 地址，结尾要带 /\n若无法访问，请尝试自行搭建反向代理站点")
    val apiBaseUrl by value("https://lolibooru.moe/")

    @ValueDescription("返回图片的画质\nPREVIEW - 低画质\nSAMPLE - 中等画质\nFILE - 原画质")
    val quality by value("SAMPLE")

    @ValueName("enable-groups")
    @ValueDescription("启用本插件的群聊列表\n除了修改配置外，你也可以给予权限")
    val enableGroups by value(listOf<Long>())

    @ValueName("strict-mode")
    @ValueDescription("严格模式\n开启后将过滤掉评级为 q (衣物较少) 的图片")
    val strictMode by value(true)

    @ValueDescription("是否需要 @ 机器人来触发随机发图")
    val at by value(false)

    @ValueDescription("随机发图的关键词")
    val keywords by value(listOf("来只萝莉"))

    @ValueName("reply-success")
    @ValueDescription("获取成功的回复信息")
    val replySuccess by value("\$pic\n图片地址: \$url\n标签: \$tags")
    @ValueName("image-fail-download")
    @ValueDescription("图片下载失败时的代替文字")
    val imageFailDownload by value("「图片下载失败」")
    @ValueName("reply-fail")
    @ValueDescription("获取成功的回复信息")
    val replyFail by value("\$quote获取失败，稍后再试吧")

    @ValueName("reply-fetching")
    @ValueDescription("正在获取的回复信息")
    val replyFetching by value("\$quote正在获取中，请稍等")

    @ValueName("recall-fetching-message")
    @ValueDescription("图片获取完成后撤回正在获取的回复信息")
    val recallFetchingMessage by value(true)

    @ValueName("msg-reload")
    val msgReload by value("配置文件已重载")

    @ValueDescription("冷却时间 (各群独立，单位是秒)")
    val cooldown by value(30)

    @ValueName("fail-cooldown")
    @ValueDescription("获取图片失败时重置的冷却时间 (各群独立，单位是秒)")
    val failCooldown by value(3)

    @ValueName("reply-cooldown")
    @ValueDescription("冷却提示\n\$cd 冷却剩余时间")
    val replyCooldown by value("\$quote太快了，再等等吧")

    @ValueDescription("是否顺便保存图片到本地 (data 文件夹)")
    val download by value(false)
}