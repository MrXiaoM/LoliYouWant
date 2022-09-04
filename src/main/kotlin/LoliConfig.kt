package top.mrxiaom.loliyouwant

import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText

object LoliConfig : ReadOnlyPluginConfig("config") {
    private const val DEFAULT_KEYWORD_YAML = "..来只萝莉:\n" +
            "....# 指定该关键词包括的 Tag\n" +
            "....tags: []\n" +
            "....# 获取图片的最大数量\n" +
            "....# 接口每次只能申请40张图片，过滤器会过滤掉一部分，故该数量仅供限制最多发送数量\n" +
            "....# 若数量大于等于2，将通过合并转发发送\n" +
            "....count: 1\n" +
            "....# 单张图片的连接超时时间 (秒)\n" +
            "....timeout: 60\n" +
            "....# 是否需要 @ 机器人来触发随机发图\n" +
            "....at: false\n" +
            "....# 返回图片的画质\n" +
            "....# PREVIEW - 低画质\n" +
            "....# SAMPLE - 中等画质\n" +
            "....# FILE - 原画质\n" +
            "....quality: SAMPLE\n" +
            "....# 获取成功的回复信息\n" +
            "....# \$at @发送者\n" +
            "....# \$quote 回复发送者\n" +
            "....# \$id 图片ID\n" +
            "....# \$previewUrl 图片直链 (预览画质)\n" +
            "....# \$sampleUrl 图片直链 (中等画质)\n" +
            "....# \$fileUrl 图片直链 (原画质)\n" +
            "....# \$url 图片直链 (发送的图所选画质)\n" +
            "....# \$tags 图片标签\n" +
            "....# \$rating 图片分级，q (Questionable) 或者 s (Safe)\n" +
            "....# \$pic 下载的图片，下载失败时用 image-fail-download 的值代替\n" +
            "....reply-success: \"\$pic\\n图片地址: https://lolibooru.moe/post/show/\$id\\n标签: \$tags\"\n" +
            "....# 图片下载失败时的代替文字\n" +
            "....image-fail-download: 「图片下载失败」\n" +
            "....# 获取成功的回复信息\n" +
            "....# \$at @发送者\n" +
            "....# \$quote 回复发送者\n" +
            "....reply-fail: '\$quote获取失败，稍后再试吧'\n" +
            "....# 正在获取的回复信息\n" +
            "....# \$at @发送者\n" +
            "....# \$quote 回复发送者\n" +
            "....reply-fetching: '\$quote正在获取中，请稍等'\n" +
            "....# 图片获取完成后撤回正在获取的回复信息\n" +
            "....recall-fetching-message: true\n" +
            "....# 是否顺便保存图片到本地 (data 文件夹)\n" +
            "....download: false\n" +
            "....# 重写图片保存路径，该路径相对于 data/top.mrxiaom.loliyouwant/\n" +
            "....override-download-path: ''"

    @Serializable
    class Keyword(
        val tags: List<String>,
        val count: Int,
        val timeout: Int,
        val at: Boolean,
        val quality: String,
        val replySuccess: String,
        val imageFailDownload: String,
        val replyFail: String,
        val replyFetching: String,
        val recallFetchingMessage: Boolean,
        val download: Boolean,
        val overrideDownloadPath: String,
    )

    @ValueName("api-base-url")
    @ValueDescription("Lolibooru 地址，结尾要带 /\n若无法访问，请尝试自行搭建反向代理站点")
    val apiBaseUrl by value("https://lolibooru.moe/")

    @ValueName("enable-groups")
    @ValueDescription("启用本插件的群聊列表\n除了修改配置外，你也可以给予权限")
    val enableGroups by value(listOf<Long>())

    @ValueName("strict-mode")
    @ValueDescription("严格模式\n开启后将过滤掉评级为 q (衣物较少) 的图片")
    val strictMode by value(true)

    @ValueName("hidden-tags")
    @ValueDescription(
        "隐藏 tag 不合适的图片。\n" +
                "默认配置的 tag 将会过滤一些 r18 漏网之鱼和部分 r16 图片，减少可能违规的图片有助于机器人防封。\n" +
                "其中 3dcg 为 3D 渲染的图片，即使是插件作者个人也欣赏不来，故添加。" +
                "\n更新插件并不会更新这个列表，如有更新该列表的需求，请到本插件帖子中寻找。"
    )
    val hiddenTags by value(
        listOf(
            "panties",
            "pantsu",
            "underwear",
            "navel",
            "breast",
            "bikini",
            "stomach",
            "topless",
            "bottomless",
            "groin",
            "butt",
            "3dcg"
        )
    )

    @ValueName("keywords")
    @ValueDescription(
        "随机发图的关键词，可自由添加\n" +
                "因无法为自定义数据类型添加注释，故将默认配置及注释放到这里\n" +
                "配置文件系统的注释自动吃缩进，请自觉把 . 看为空格\n" +
                DEFAULT_KEYWORD_YAML
    )
    val keywords by value(
        mapOf(
            "来只萝莉" to Keyword(
                listOf(),
                1,
                60,
                false,
                "SAMPLE",
                "\$pic\n图片地址: https://lolibooru.moe/post/show/\$id\n标签: \$tags",
                "「图片下载失败」",
                "\$quote获取失败，稍后再试吧",
                "\$quote正在获取中，请稍等",
                true,
                false,
                ""
            )
        )
    )

    @ValueName("reply-cooldown")
    @ValueDescription(
        "冷却提示\n" +
                "\$at @发送者\n" +
                "\$quote 回复发送者\n" +
                "\$cd 冷却剩余时间"
    )
    val replyCooldown by value("\$quote太快了，再等等吧 (\$cd)")

    @ValueName("msg-reload")
    val msgReload by value("配置文件已重载")

    @ValueName("cooldown")
    @ValueDescription("冷却时间 (各群独立，单位是秒)")
    val cooldown by value(30)

    @ValueName("fail-cooldown")
    @ValueDescription("获取图片失败时重置的冷却时间 (各群独立，单位是秒)")
    val failCooldown by value(3)

    fun resolveKeyword(message: MessageChain, at: Boolean): Keyword? {
        val key = message.filterIsInstance<PlainText>().joinToString { it.content }.trimStart().trimEnd()
        val keyword = keywords[key] ?: return null
        return if (keyword.at && !at) null else keyword
    }

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