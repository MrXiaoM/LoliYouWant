package top.mrxiaom.loliyouwant

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.MemberCommandSender
import net.mamoe.mirai.console.permission.Permission

class LoliCommand(private val perm: Permission) : CompositeCommand(
    owner = LoliYouWant,
    primaryName = "LoliYouWant",
    secondaryNames = arrayOf("loli", "lyw"),
    parentPermission = perm
) {
    fun CommandSender.reload() {
        LoliYouWant.reloadConfig()
    }
}