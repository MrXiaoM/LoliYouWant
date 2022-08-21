package top.mrxiaom.loliyouwant

import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.console.plugin.id
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.utils.info
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.*

object LoliYouWant : KotlinPlugin(
        JvmPluginDescription(
                id = "top.mrxiaom.loliyouwant",
                name = "Loli You Want",
                version = "0.1.0",
        ) {
            author("MrXiaoM")
        }
) {
    private val r18 = listOf("dick","penis","pupils","sex","pussy","vaginal","nipples")
    private val PERM_RANDOM = PermissionId(id, "random")
    private val cooldown = mutableMapOf<Long, Long>()
    override fun onEnable() {
        val permRandom = PermissionService.INSTANCE.register(PERM_RANDOM, "随机发图权限")
        reloadConfig()
        globalEventChannel(coroutineContext).subscribeAlways<GroupMessageEvent> {
            if (LoliConfig.at && this.message.filterIsInstance<At>().none { it.target == bot.id }) return@subscribeAlways
            if (!LoliConfig.enableGroups.contains(group.id) && !permRandom.testPermission(group.permitteeId)) return@subscribeAlways
            if (!LoliConfig.keywords.contains(message.filterIsInstance<PlainText>().joinToString { it.content }.trimStart().trimEnd())) return@subscribeAlways
            val quote = QuoteReply(source)
            val at = At(sender)
            val replacement = mutableMapOf("quote" to quote, "at" to at)
            val cd = cooldown.getOrDefault(group.id, 0)
            if (cd >= System.currentTimeMillis()) {
                group.sendMessage(LoliConfig.replyCooldown.replace(replacement))
                return@subscribeAlways
            }
            cooldown[group.id] = System.currentTimeMillis() + LoliConfig.cooldown * 1000
            val loli = Lolibooru.random(1).firstOrNull()
            if (loli == null || r18(loli.tags.lowercase(Locale.getDefault()))) {
                group.sendMessage(LoliConfig.replyFail.replace(replacement))
                return@subscribeAlways
            }
            val url = when(LoliConfig.quality)
            {
                "FILE" -> loli.url
                "PREVIEW" -> loli.urlPreview
                else -> loli.urlSample
            }.replace(" ", "%20")
            replacement.putAll(mapOf(
                "url" to PlainText(url),
                "tags" to PlainText(loli.tags),
                "pic" to PrepareUploadImage.url(group, url, LoliConfig.imageFailDownload
                ) { input ->
                    if (!LoliConfig.download) return@url input
                    val file = resolveDataFile(url.substringAfterLast('/'))
                    file.writeBytes(input.readBytes())
                    return@url FileInputStream(file)
                }
            ))
            group.sendMessage(LoliConfig.replySuccess.replace(replacement))
        }
        logger.info { "Plugin loaded" }
    }
    fun r18(tags: String): Boolean {
        for (keyword in r18){
            if (tags.contains(keyword)) return true
        }
        return false
    }

    fun reloadConfig() {
        LoliConfig.reload()
        Lolibooru.baseUrl = LoliConfig.apiBaseUrl
    }
}