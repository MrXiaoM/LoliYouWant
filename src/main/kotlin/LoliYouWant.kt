package top.mrxiaom.loliyouwant

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.console.plugin.id
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.utils.info
import java.io.FileInputStream

object LoliYouWant : KotlinPlugin(
    JvmPluginDescription(
        id = "top.mrxiaom.loliyouwant",
        name = "Loli You Want",
        version = "0.1.0",
    ) {
        author("MrXiaoM")
    }
) {
    private val r18Tags = listOf("sex", "penis", "pussy", "cum", "nude", "vaginal", "testicles")
    private val blacklistTags = mutableListOf<String>()
    private val PERM_RANDOM = PermissionId(id, "random")
    private val PERM_BYPASS_COOLDOWN = PermissionId(id, "bypass.cooldown")
    private val PERM_RELOAD = PermissionId(id, "reload")
    private val cooldown = mutableMapOf<Long, Long>()
    private val cooldownFriend = mutableMapOf<Long, Long>()
    override fun onEnable() {
        val permRandom = PermissionService.INSTANCE.register(PERM_RANDOM, "随机发图权限")
        val permBypassCooldown = PermissionService.INSTANCE.register(PERM_BYPASS_COOLDOWN, "绕过冷却时间")

        reloadConfig()
        LoliCommand(PermissionService.INSTANCE.register(PERM_RELOAD, "重载配置文件")).register()

        globalEventChannel(coroutineContext).subscribeAlways<GroupMessageEvent> {
            if (LoliConfig.at && this.message.filterIsInstance<At>()
                    .none { it.target == bot.id }
            ) return@subscribeAlways
            if (!LoliConfig.enableGroups.contains(group.id) && !permRandom.testPermission(group.permitteeId) && !permRandom.testPermission(
                    sender.permitteeId
                )
            ) return@subscribeAlways
            if (!LoliConfig.keywords.contains(message.filterIsInstance<PlainText>().joinToString { it.content }
                    .trimStart().trimEnd())) return@subscribeAlways
            val replacement = mutableMapOf("quote" to QuoteReply(source), "at" to At(sender))
            if (!permBypassCooldown.testPermission(group.permitteeId) && !permBypassCooldown.testPermission(sender.permitteeId)) {
                val cd = cooldown.getOrDefault(group.id, 0)
                if (cd >= System.currentTimeMillis()) {
                    replacement["cd"] = PlainText(((cd - System.currentTimeMillis()) / 1000L).toString())
                    group.sendMessage(LoliConfig.replyCooldown.replace(replacement))
                    return@subscribeAlways
                }
            }
            cooldown[group.id] = System.currentTimeMillis() + LoliConfig.cooldown * 1000
            val receipt = group.sendMessage(LoliConfig.replyFetching.replace(replacement))
            val loli = searchLolis(Lolibooru.get(10, 1, "order%3Arandom%20-rating:e")).randomOrNull()
            if (loli == null) {
                group.sendMessage(LoliConfig.replyFail.replace(replacement))
                cooldown[group.id] = System.currentTimeMillis() + LoliConfig.failCooldown * 1000
                receipt.recallIgnoreError()
                return@subscribeAlways
            }
            val url = when (LoliConfig.quality) {
                "FILE" -> loli.url
                "PREVIEW" -> loli.urlPreview
                else -> loli.urlSample
            }.replace(" ", "%20")
            replacement.putAll(mapOf(
                "id" to PlainText(loli.id.toString()),
                "previewUrl" to PlainText(loli.urlPreview.replace(" ", "%20")),
                "sampleUrl" to PlainText(loli.urlSample.replace(" ", "%20")),
                "fileUrl" to PlainText(loli.url.replace(" ", "%20")),
                "url" to PlainText(url.replace(" ", "%20")),
                "tags" to PlainText(loli.tags),
                "rating" to PlainText(loli.rating),
                "pic" to PrepareUploadImage.url(
                    group, url, LoliConfig.imageFailDownload
                ) { input ->
                    if (!LoliConfig.download) return@url input
                    val file = resolveDataFile(url.substringAfterLast('/').replace("%20", " "))
                    file.writeBytes(input.readBytes())
                    return@url FileInputStream(file)
                }
            ))
            group.sendMessage(LoliConfig.replySuccess.replace(replacement))
            if (LoliConfig.recallFetchingMessage) receipt.recallIgnoreError()
        }

        globalEventChannel(coroutineContext).subscribeAlways<FriendMessageEvent> {
            if (!permRandom.testPermission(sender.permitteeId)) return@subscribeAlways
            if (!LoliConfig.keywords.contains(message.filterIsInstance<PlainText>().joinToString { it.content }
                    .trimStart().trimEnd())) return@subscribeAlways
            val replacement = mutableMapOf("quote" to QuoteReply(source), "at" to At(sender))
            if (!permBypassCooldown.testPermission(sender.permitteeId)) {
                val cd = cooldownFriend.getOrDefault(sender.id, 0)
                if (cd >= System.currentTimeMillis()) {
                    replacement["cd"] = PlainText(((cd - System.currentTimeMillis()) / 1000L).toString())
                    sender.sendMessage(LoliConfig.replyCooldown.replace(replacement))
                    return@subscribeAlways
                }
            }
            cooldownFriend[sender.id] = System.currentTimeMillis() + LoliConfig.cooldown * 1000
            val receipt = sender.sendMessage(LoliConfig.replyFetching.replace(replacement))
            val loli = searchLolis(Lolibooru.get(10, 1, "order%3Arandom%20-rating:e")).randomOrNull()
            if (loli == null) {
                sender.sendMessage(LoliConfig.replyFail.replace(replacement))
                cooldownFriend[sender.id] = System.currentTimeMillis() + LoliConfig.failCooldown * 1000
                receipt.recallIgnoreError()
                return@subscribeAlways
            }
            val url = when (LoliConfig.quality) {
                "FILE" -> loli.url
                "PREVIEW" -> loli.urlPreview
                else -> loli.urlSample
            }.replace(" ", "%20")
            replacement.putAll(mapOf(
                "id" to PlainText(loli.id.toString()),
                "previewUrl" to PlainText(loli.urlPreview.replace(" ", "%20")),
                "sampleUrl" to PlainText(loli.urlSample.replace(" ", "%20")),
                "fileUrl" to PlainText(loli.url.replace(" ", "%20")),
                "url" to PlainText(url.replace(" ", "%20")),
                "tags" to PlainText(loli.tags),
                "rating" to PlainText(loli.rating),
                "pic" to PrepareUploadImage.url(
                    sender, url, LoliConfig.imageFailDownload
                ) { input ->
                    if (!LoliConfig.download) return@url input
                    val file = resolveDataFile(url.substringAfterLast('/').replace("%20", " "))
                    file.writeBytes(input.readBytes())
                    return@url FileInputStream(file)
                }
            ))
            sender.sendMessage(LoliConfig.replySuccess.replace(replacement))
            if (LoliConfig.recallFetchingMessage) receipt.recallIgnoreError()
        }
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
        for (tag in blacklistTags) {
            if (loli.tags.contains(tag)) return false
        }
        return true
    }

    fun reloadConfig() {
        LoliConfig.reload()
        LoliConfig.save()
        Lolibooru.baseUrl = LoliConfig.apiBaseUrl
        blacklistTags.clear()
        blacklistTags.addAll(r18Tags)
        blacklistTags.addAll(LoliConfig.hiddenTags)
    }
}

suspend fun MessageReceipt<Contact>.recallIgnoreError() {
    try {
        this.recall()
    } catch (_: Throwable) {
    }
}