package top.mrxiaom.loliyouwant

import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.*
import net.mamoe.yamlkt.Comment

import top.mrxiaom.loliyouwant.EconomyHolder.CostResult.*

object LoliConfig : ReadOnlyPluginConfig("config") {
    @Serializable
    data class Keyword(
        @Comment("指定该关键词包括的 Tag")
        val tags: List<String> = listOf(),
        @Comment("""
        获取图片的最大数量
        接口每次只能申请40张图片, 过滤器会过滤掉一部分, 故该数量仅供限制最多发送数量
        若数量大于1, 将通过合并转发发送
        """)
        val count: Int = 1,
        @Comment("单张图片的连接超时时间 (秒)")
        val timeout: Int = 60,
        @Comment("是否需要 @ 机器人来触发随机发图")
        val at: Boolean = true,
        @Comment("""
        返回图片的画质
        PREVIEW - 低画质
        SAMPLE - 中等画质
        FILE - 原画质
        """)
        val quality: String = "SAMPLE",
        @Comment("""
        获取成功的回复信息
        ${'$'}at @发送者
        ${'$'}quote 回复发送者
        ${'$'}id 图片ID
        ${'$'}previewUrl 图片直链 (预览画质)
        ${'$'}sampleUrl 图片直链 (中等画质)
        ${'$'}fileUrl 图片直链 (原画质)
        ${'$'}url 图片直链 (发送的图所选画质)
        ${'$'}tags 图片标签
        ${'$'}rating 图片分级, q (Questionable) 或者 s (Safe)
        ${'$'}pic 下载的图片, 下载失败时用 image-fail-download 的值代替
        """)
        val replySuccess: String = "\$pic\n图片地址: https://lolibooru.moe/post/show/\$id\n标签: \$tags",
        @Comment("图片下载失败时的代替文字")
        val imageFailDownload: String = "「图片下载失败」",
        @Comment("""
        获取失败的回复信息
        ${'$'}at @发送者
        ${'$'}quote 回复发送者
        """)
        val replyFail: String = "\$quote获取失败, 稍后再试吧",
        @Comment("""
        正在获取的回复信息
        ${'$'}at @发送者
        ${'$'}quote 回复发送者
        """)
        val replyFetching: String = "\$quote正在获取中, 请稍等",
        @Comment("图片获取完成后撤回正在获取的回复信息")
        val recallFetchingMessage: Boolean = true,
        @Comment("是否顺便保存图片到本地 (data 文件夹)")
        val download: Boolean = false,
        @Comment("重写图片保存路径, 该路径相对于 data/top.mrxiaom.loliyouwant/")
        val overrideDownloadPath: String = "",
        @Comment("""
        执行命令所需金钱的货币类型
        留空为不花费金钱
        该功能需要安装 mirai-economy-core 插件生效
        """)
        val costMoneyCurrency: String = "mirai-coin",
        @Comment("执行命令所需金钱")
        val costMoney: Double = 10.0,
        @Comment("""
        是否从全局上下文扣除金钱
        若关闭该项, 将在用户执行命令所在群的上下文扣除金钱
        私聊执行命令将强制使用全局上下文
        """)
        val costMoneyGlobal: Boolean = false,
        @Comment("""
        执行命令金钱不足提醒
        ${'$'}at 为 @ 发送者
        ${'$'}quote 为回复发送者
        ${'$'}cost 为需要花费的金钱
        """)
        val costMoneyNotEnough: String ="\$quote你没有足够的 Mirai 币 (\$cost) 来执行该命令!"
    ) {
        suspend fun costMoney(
            group: Group?,
            user: User,
            source: MessageSource
        ): Boolean = when(
            if (costMoneyGlobal || group == null) EconomyHolder.costMoney(user, costMoneyCurrency, costMoney)
            else EconomyHolder.costMoney(group, user, costMoneyCurrency, costMoney)
        ) {
            NO_CURRENCY -> false.also { EconomyHolder.logger.warning("货币种类 `$costMoneyCurrency` 不存在") }
            NOT_ENOUGH -> false.also {
                (group ?: user).sendMessage(buildMessageChain {
                    if (costMoneyNotEnough.contains("\$quote")) add(QuoteReply(source))
                    addAll(Regex("\\\$at").split<SingleMessage>(
                        costMoneyNotEnough
                            .replace("\$cost", costMoney.toString())
                            .replace("\$quote", "")
                    ) { s, isMatched ->
                        if (isMatched) At(user.id) else PlainText(s)
                    })
                })
            }
            else -> true
        }
    }

    @ValueName("api-base-url")
    @ValueDescription("""
        Lolibooru 地址
        若无法访问, 请尝试自行搭建反向代理站点
        填写该项时, 结尾的 / 可以省略
        代理站点无法访问时将会尝试访问源站
    """)
    val apiBaseUrl by value("https://lolibooru.moe/")

    @ValueName("enable-groups")
    @ValueDescription("""
        启用本插件的群聊列表
        除了修改配置外, 你也可以给予权限
    """)
    val enableGroups by value(listOf<Long>())

    @ValueName("strict-mode")
    @ValueDescription("""
        严格模式
        开启后将过滤掉评级为 q (分级模糊, 可疑) 的图片
    """)
    val strictMode by value(true)

    @ValueName("max-search-count")
    @ValueDescription("""
        使用 /loli list 命令搜索图片时的最大图片数量
    """)
    val maxSearchCount by value(5)

    @ValueName("max-search-count-warn")
    @ValueDescription("""
        使用 /loli list 命令搜索图片超出最大图片数量时的警告
        ${'$'}at 为 @ 发送者
        ${'$'}quote 为回复发送者
        ${'$'}count 为 max-search-count 的值
    """)
    val maxSearchCountWarn by value("\$quote图片数量不能超过\$count")

    @ValueDescription("使用 /loli get 或 loli list 命令获取图片, 下载失败时的代替文字")
    val imageFailDownload by value("「图片下载失败」")
    @ValueDescription("使用 /loli get 或 loli list 命令获取图片, 获取成功的回复信息")
    val replySuccess by value("\$pic\n图片地址: https://lolibooru.moe/post/show/\$id")
    @ValueDescription("使用 /loli get 或 loli list 命令获取图片, 获取失败的回复信息")
    val replyFail by value("\$quote获取失败, 稍后再试吧")
    @ValueDescription("使用 /loli get 或 loli list 命令获取图片, 正在获取的回复信息")
    val replyFetching by value("\$quote正在获取中, 请稍等")
    @ValueDescription("使用 /loli get 或 loli list 命令获取图片, 获取完成后撤回正在获取的回复信息")
    val recallFetchingMessage by value(true)
    @ValueDescription("使用 /loli get 或 loli list 命令获取图片, 返回图片的画质")
    val quality by value("SAMPLE")
    @ValueDescription("使用 /loli get 或 loli list 命令获取图片, 单张图片的连接超时时间 (秒)")
    val timeout by value(60)
    @ValueDescription("使用 /loli get 或 loli list 命令获取图片, 是否顺便保存图片到本地 (data 文件夹)")
    val download by value(false)
    @ValueDescription("使用 /loli get 或 loli list 命令获取图片, 重写图片保存路径, 该路径相对于 data/top.mrxiaom.loliyouwant/")
    val overrideDownloadPath by value("command")

    @ValueName("does-add-tags-to-params")
    @ValueDescription("""
        是否将需要隐藏的 tag 添加到请求地址的参数中
        开启该选项, 有助于在获取多张图片的情况下让图片数量尽可能多
    """)
    val doesAddTagsToParams by value(true)

    @ValueName("hidden-tags")
    @ValueDescription("""
        隐藏 tag 不合适的图片。
        默认配置的 tag 将会过滤一些 r18 漏网之鱼和部分 r16 图片, 减少可能违规的图片有助于机器人防封。
        其中 3dcg 为 3D 渲染的图片, 即使是插件作者个人也欣赏不来, 故添加。
        更新插件并不会更新这个列表, 如有更新该列表的需求, 请到本插件帖子中寻找。
        https://mirai.mamoe.net/topic/1515
    """)
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

    @ValueName("hidden-when-tags-less-than")
    @ValueDescription("""
        屏蔽tag数量少于不等于这个数字的图片
        部分老旧的图片分级不明确, 无法根据tag进行进一步分级
    """)
    val hiddenTagsCount by value(26)

    @ValueName("keywords")
    @ValueDescription("随机发图的关键词, 可自由添加")
    val keywords by value(
        mapOf(
            "来只萝莉" to Keyword()
        )
    )

    @ValueName("reply-cooldown")
    @ValueDescription("""
        冷却提示
        ${'$'}at @发送者
        ${'$'}quote 回复发送者
        ${'$'}cd 冷却剩余时间
    """)
    val replyCooldown by value("\$quote太快了, 再等等吧 (\$cd)")

    @ValueName("msg-reload")
    val msgReload by value("配置文件已重载")

    @ValueName("cooldown")
    @ValueDescription("冷却时间 (各群独立, 单位是秒)")
    val cooldown by value(30)

    @ValueName("fail-cooldown")
    @ValueDescription("获取图片失败时重置的冷却时间 (各群独立, 单位是秒)")
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
        // 为 ReadOnlyPluginData 增加保存方法, 以便更新配置文件
        kotlin.runCatching {
            storage_.store(owner_, this)
        }.onFailure { e ->
            LoliYouWant.logger.error(e)
        }
    }
}

/**
 * 分隔字符串
 * @param input 需要分隔的字符串
 * @param transform 转换器, 返回 null 时不添加该项到结果
 */
fun <T> Regex.split(
    input: CharSequence,
    transform: (s: String, isMatched: Boolean) -> T?
): List<T> {
    val list = mutableListOf<T>()
    var index = 0
    for (result in findAll(input)) {
        val first = result.range.first
        val last = result.range.last
        if (first > index) {
            val value = transform(input.substring(index, first), false)
            if (value != null) list.add(value)
        }
        val value = transform(input.substring(first, last + 1), true)
        if (value != null) list.add(value)
        index = last + 1
    }
    if (index < input.length) {
        val value = transform(input.substring(index), false)
        if (value != null) list.add(value)
    }
    return list
}
