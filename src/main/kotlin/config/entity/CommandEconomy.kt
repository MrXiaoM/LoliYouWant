package top.mrxiaom.loliyouwant.config.entity

import kotlinx.serialization.Serializable
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.MessageSource
import net.mamoe.yamlkt.Comment
import top.mrxiaom.loliyouwant.utils.EconomyHolder

@Serializable
data class CommandEconomy(
    @Comment("""
        执行命令所需金钱的货币类型
        留空为不花费金钱
        该功能需要安装 mirai-economy-core 插件生效
        """)
    val costMoneyCurrency: String = "mirai-coin",
    @Comment("""
        执行命令所需金钱单价
        最终价格为 {costMoney} * {图片张数} * {costMoneyPictureMultiplier}
        """)
    val costMoney: Double = 10.0,
    @Comment("""
        每张图片的价格乘数
        如果设置为0或负数，最终价格为 {costMoney}
        """)
    val costMoneyPictureMultiplier: Double = 1.0,
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
        source: MessageSource,
        count: Int
    ): Boolean {
        val money: Double = if (costMoneyPictureMultiplier > 0) {
            costMoney * count * costMoneyPictureMultiplier
        } else {
            costMoney
        }
        return EconomyHolder.costMoney(
            group,
            user,
            source,
            money,
            costMoneyGlobal,
            costMoneyCurrency,
            costMoneyNotEnough
        )
    }
}
