package top.mrxiaom.loliyouwant.config.entity

import kotlinx.serialization.Serializable
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.MessageSource
import net.mamoe.yamlkt.Comment
import top.mrxiaom.loliyouwant.utils.EconomyHolder

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
    @Comment("获取失败时重试次数")
    val retryTimes: Int = 5,
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
    ): Boolean = EconomyHolder.costMoney(
        group,
        user,
        source,
        costMoney,
        costMoneyGlobal,
        costMoneyCurrency,
        costMoneyNotEnough
    )
}
