package top.mrxiaom.loliyouwant

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.plugin.id
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.registerTo
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.utils.info
import top.mrxiaom.loliyouwant.api.EconomyHolder
import top.mrxiaom.loliyouwant.api.Loli
import top.mrxiaom.loliyouwant.api.Lolibooru
import top.mrxiaom.loliyouwant.commands.LoliAdminCommand
import top.mrxiaom.loliyouwant.commands.LoliCommand

object LoliYouWant : KotlinPlugin(
    JvmPluginDescription(
        id = "top.mrxiaom.loliyouwant",
        name = "Loli You Want",
        version = BuildConstants.VERSION,
    ) {
        author("MrXiaoM")

        dependsOn("xyz.cssxsh.mirai.plugin.mirai-economy-core", true)
    }
) {
    private val r18Tags = listOf(
        "sex",
        "penis",
        "pussy",
        "cum",
        "nude",
        "vaginal",
        "testicles",
        "nipple",
        "papilla",
        "teat",
        "thele",
        "vulva"
    )
    internal val blacklistTags = mutableListOf<String>()
    lateinit var PERM_RANDOM: Permission
    lateinit var PERM_BYPASS_COOLDOWN: Permission
    internal val cooldown = mutableMapOf<Long, Long>()
    internal val cooldownFriend = mutableMapOf<Long, Long>()
    override fun onEnable() {
        PERM_RANDOM = PermissionService.INSTANCE.register(PermissionId(id, "random"), "关键词随机发图权限")
        PERM_BYPASS_COOLDOWN = PermissionService.INSTANCE.register(PermissionId(id, "bypass.cooldown"), "绕过冷却时间")

        logger.info(when (EconomyHolder.hasEconomyCorePlugin) {
            true -> "已安装经济插件"
            false -> "未安装经济插件"
        })

        reloadConfig()
        LoliCommand.register()
        LoliAdminCommand.register()

        MessageHost.registerTo(globalEventChannel(coroutineContext))

        logger.info { "Plugin loaded" }
    }

    fun searchLolis(loliList: List<Loli>): List<Loli> {
        return loliList
            // 为你的账号安全着想，请不要移除评级为 e 的图片过滤
            // 要涩涩就自己上源站看去
            .filter { it.rating != "e" }
            .filter { checkTags(it) }
            .filter { if (!LoliConfig.strictMode) it.rating != "q" else true }
    }

    fun checkTags(loli: Loli): Boolean {
        if (loli.tags.split(" ").size < LoliConfig.hiddenTagsCount) return false
        if (blacklistTags.any { loli.tags.contains(it) }) return false
        return true
    }

    fun reloadConfig() {
        LoliConfig.reload()
        LoliConfig.save()
        Lolibooru.baseUrl = LoliConfig.apiBaseUrl
        blacklistTags.clear()
        blacklistTags.addAll(r18Tags)
        blacklistTags.addAll(LoliConfig.hiddenTags.map { it.trimStart().trimEnd().replace(" ", "_") })
    }
}

suspend fun MessageReceipt<Contact>.recallIgnoreError() {
    try {
        this.recall()
    } catch (_: Throwable) {
    }
}
