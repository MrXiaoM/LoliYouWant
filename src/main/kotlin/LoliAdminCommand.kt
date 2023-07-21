package top.mrxiaom.loliyouwant

import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.UserMessageEvent
import net.mamoe.mirai.message.data.*
import kotlin.random.Random

object LoliAdminCommand: CompositeCommand(
    owner = LoliYouWant,
    primaryName = "LoliYouWantAdmin",
    secondaryNames = arrayOf("loliadmin"),
    description = "LoliYouWant 管理命令"
) {
    @SubCommand
    @Description("重载插件配置文件")
    suspend fun CommandSender.reload() {
        LoliYouWant.reloadConfig()
        sendMessage(LoliConfig.msgReload)
    }

    @SubCommand
    @Description("获取已载入的关键词列表")
    suspend fun CommandSender.keywords() {
        sendMessage("关键词列表 (${LoliConfig.keywords.size}):\n" + LoliConfig.keywords.map {
            "   ${it.key} (获取 ${it.value.count} 张图) 标签: " + (if (it.value.tags.isEmpty()) "(无)" else it.value.tags.joinToString(
                " "
            ))
        }.joinToString("\n"))
    }
}