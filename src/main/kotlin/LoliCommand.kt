package top.mrxiaom.loliyouwant

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.permission.Permission

class LoliCommand(perm: Permission) : SimpleCommand(
    owner = LoliYouWant,
    primaryName = "LoliYouWant",
    secondaryNames = arrayOf("loli", "luw"),
    parentPermission = perm
) {
    @Handler
    suspend fun CommandSender.handle(operation: String) {
        if (operation.equals("reload", true)) {
            LoliYouWant.reloadConfig()
            sendMessage(LoliConfig.msgReload)
        }
        if (operation.equals("keywords", false)) {
            sendMessage("关键词列表 (${LoliConfig.keywords.size}):\n" + LoliConfig.keywords.map {
                "   ${it.key} (获取 ${it.value.count} 张图) 标签: " + (if (it.value.tags.isEmpty()) "(无)" else it.value.tags.joinToString(
                    " "
                ))
            }.joinToString("\n"))
        }
    }
}