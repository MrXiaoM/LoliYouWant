package top.mrxiaom.loliyouwant

import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.util.ConsoleExperimentalApi

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
    @ValueDescription("获取成功的回复信息\n" +
            "\$at @发送者\n" +
            "\$quote 回复发送者\n" +
            "\$id 图片ID\n" +
            "\$previewUrl 图片直链 (预览画质)\n" +
            "\$sampleUrl 图片直链 (中等画质)\n" +
            "\$fileUrl 图片直链 (原画质)\n" +
            "\$url 图片直链 (发送的图所选画质)\n" +
            "\$tags 图片标签\n" +
            "\$rating 图片分级，q (Questionable) 或者 s (Safe)\n" +
            "\$pic 下载的图片，下载失败时用 image-fail-download 的值代替\n")
    val replySuccess by value("\$pic\n图片地址: https://lolibooru.moe/post/show/\$id\n标签: \$tags")
    @ValueName("image-fail-download")
    @ValueDescription("图片下载失败时的代替文字")
    val imageFailDownload by value("「图片下载失败」")
    @ValueName("reply-fail")
    @ValueDescription("获取成功的回复信息\n" +
            "\$at @发送者\n" +
            "\$quote 回复发送者")
    val replyFail by value("\$quote获取失败，稍后再试吧")

    @ValueName("reply-fetching")
    @ValueDescription("正在获取的回复信息\n" +
            "\$at @发送者\n" +
            "\$quote 回复发送者")
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
    @ValueDescription("冷却提示\n" +
            "\$at @发送者\n" +
            "\$quote 回复发送者\n" +
            "\$cd 冷却剩余时间")
    val replyCooldown by value("\$quote太快了，再等等吧")

    @ValueDescription("是否顺便保存图片到本地 (data 文件夹)")
    val download by value(false)

    @OptIn(ConsoleExperimentalApi::class)
    private lateinit var owner_: PluginDataHolder
    @OptIn(ConsoleExperimentalApi::class)
    private lateinit var storage_: PluginDataStorage
    @OptIn(ConsoleExperimentalApi::class)
    override fun onInit(owner: PluginDataHolder, storage: PluginDataStorage) {
        owner_ = owner
        storage_ = storage
    }

    @OptIn(ConsoleExperimentalApi::class)
    private fun save() {
        // 为 ReadOnlyPluginData 增加保存方法，以便更新配置文件
        kotlin.runCatching {
            storage_.store(owner_, this)
        }.onFailure { e ->
            LoliYouWant.logger.error(e)
        }
    }
}