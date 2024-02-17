package top.mrxiaom.loliyouwant.config

import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageSource
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.yamlkt.Comment
import top.mrxiaom.loliyouwant.LoliYouWant
import top.mrxiaom.loliyouwant.config.entity.CommandEconomy
import top.mrxiaom.loliyouwant.config.entity.CommandProperties
import top.mrxiaom.loliyouwant.config.entity.CommandSearchProperties
import top.mrxiaom.loliyouwant.config.entity.Keyword
import top.mrxiaom.loliyouwant.utils.EconomyHolder

object LoliConfig : ReadOnlyPluginConfig("config") {

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
        开启后将过滤掉评级为 questionable (分级模糊, 可疑) 的图片
    """)
    val strictMode by value(true)

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
            "pantie",
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
            "3dcg",
            "comic",
            "bum",
            "backside",
            "fanny",
            "bunny",
            "bunny_girl",
            "less",
            "bottom",
            "kiss",
            "swimsuit",
            "sukusui",
            "towel",
            "naked_towel",
            "denim",
            "armpits",
            "ribs",
            "bare_shoulders",
            "translation_request",
            "no_bra",
            "blush",
            "ass",
            "absurdres",
            "head_out_of_frame",
            "spats",
            "short",
            "short_under_skrit",
            "short_skrit",
            "thighhighs"
        )
    )

    @ValueName("hidden-when-tags-less-than")
    @ValueDescription("""
        屏蔽tag数量少于不等于这个数字的图片
        部分老旧的图片分级不明确, 无法根据tag进行进一步分级
    """)
    val hiddenTagsCount by value(26)

    @ValueName("command-retry-times")
    @ValueDescription("使用命令获取失败时的重试次数")
    val commandRetryTimes by value(5)

    @ValueName("command")
    @ValueDescription("命令 /loli get 的相关配置")
    val command by value(CommandProperties())

    @ValueName("command-economy")
    @ValueDescription("使用 /loli get 命令获取图片时的经济消耗设置")
    val commandEconomy by value(CommandEconomy())

    @ValueName("command-search")
    @ValueDescription("命令 /loli search 的相关配置")
    val commandSearch by value(CommandSearchProperties())

    @ValueName("command-economy-search")
    @ValueDescription("使用 /loli search 命令获取图片时的经济消耗设置")
    val commandEconomySearch by value(CommandEconomy())

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
