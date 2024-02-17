package top.mrxiaom.loliyouwant.config.entity

import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment


@Serializable
data class CommandProperties(
    @Comment("""
        获取图片时的最大图片数量限制
    """)
    var maxSearchCount: Int = 10,
    @Comment("""
        获取图片超出最大图片数量时的警告
        ${'$'}at 为 @ 发送者
        ${'$'}quote 为回复发送者
        ${'$'}count 为 maxSearchCount 的值
    """)
    var maxSearchCountWarn: String = "\$quote图片数量不能超过\$count",

    @Comment("获取图片时, 下载失败时的代替文字")
    var imageFailDownload: String = "「图片下载失败」",
    @Comment("获取图片时, 获取成功的回复信息")
    var replySuccess: String = "\$pic\n图片地址: https://lolibooru.moe/post/show/\$id",
    @Comment("获取图片时, 获取失败的回复信息")
    var replyFail: String = "\$quote获取失败, 稍后再试吧",
    @Comment("获取图片时, 正在获取的回复信息")
    var replyFetching: String = "\$quote正在获取中, 请稍等",

    @Comment("获取完成后撤回正在获取的回复信息")
    var recallFetchingMessage: Boolean = true,
    @Comment("返回图片的画质")
    var quality: String = "SAMPLE",
    @Comment("单张图片的连接超时时间 (秒)")
    var timeout: Int = 60,
    @Comment("是否顺便保存图片到本地 (data 文件夹)")
    var download: Boolean = false,
    @Comment("重写图片保存路径, 该路径相对于 data/top.mrxiaom.loliyouwant/")
    var overrideDownloadPath: String = "command"
)

@Serializable
data class CommandSearchProperties(

    @Comment("""
        关键词-当 keywords 配置中的所有关键词都不满足条件时，如果消息以本配置为开头，则触发 /loli search 命令
        将本配置设置为空关闭这个功能
    """)
    var keywordPrefix: String = "来点",
    @Comment("关键词-是否需要 @ 机器人来触发搜索")
    var at: Boolean = true,

    @Comment("""
        搜索图片时的最大图片数量限制
    """)
    var maxSearchCount: Int = 10,
    @Comment("""
        搜索图片超出最大图片数量时的警告
        ${'$'}at 为 @ 发送者
        ${'$'}quote 为回复发送者
        ${'$'}count 为 maxSearchCount 的值
    """)
    var maxSearchCountWarn: String = "\$quote图片数量不能超过\$count",

    @Comment("获取图片时, 下载失败时的代替文字")
    var imageFailDownload: String = "「图片下载失败」",
    @Comment("获取图片时, 获取失败的回复信息")
    var replyFail: String = "\$quote获取失败, 稍后再试吧",

    @Comment("获取图片, 获取成功的回复信息")
    var replySuccess: String = "\$pic\n图片地址: https://lolibooru.moe/post/show/\$id",
    @Comment("搜索 Tags 时, 正在获取的回复信息")
    var replySearching: String = "\$quote正在搜索中, 请稍等",
    @Comment("搜索 Tags 时, 翻译失败的回复信息")
    var replySearchTranslateFailed: String = "\$quote你的搜索关键词中存在中文，调用翻译接口失败，请重试",
    @Comment("搜索 Tags 时, 找不到任何 tags 的回复信息")
    var replySearchEmpty: String = "\$quote使用该关键词无法找到相关的 tags",
    @Comment("搜索到 Tags 之后开始获取图片时, 正在获取的回复信息")
    var replySearchFetching: String = "\$quote指定的 tags 为 \$tags。正在获取中，请稍等",
    @Comment("获取完成后撤回正在获取的回复信息")
    var recallFetchingMessage: Boolean = true,
    @Comment("返回图片的画质")
    var quality: String = "SAMPLE",
    @Comment("单张图片的连接超时时间 (秒)")
    var timeout: Int = 60,
    @Comment("是否顺便保存图片到本地 (data 文件夹)")
    var download: Boolean = false,
    @Comment("重写图片保存路径, 该路径相对于 data/top.mrxiaom.loliyouwant/")
    var overrideDownloadPath: String = "search"
)
