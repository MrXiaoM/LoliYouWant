package top.mrxiaom.loliyouwant

import net.mamoe.mirai.console.command.CommandSender.Companion.toCommandSender
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.permission.PermitteeId
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.console.util.cast
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.*
import top.mrxiaom.loliyouwant.api.Loli
import top.mrxiaom.loliyouwant.api.Lolibooru
import top.mrxiaom.loliyouwant.api.browserLikeUrlEncode
import top.mrxiaom.loliyouwant.api.urlDecode
import top.mrxiaom.loliyouwant.commands.LoliCommand.search
import top.mrxiaom.loliyouwant.config.LoliConfig
import top.mrxiaom.loliyouwant.config.entity.Keyword
import top.mrxiaom.loliyouwant.utils.PrepareUploadImage
import top.mrxiaom.loliyouwant.utils.replace
import java.io.File
import java.io.FileInputStream
import kotlin.random.Random

object MessageHost : SimpleListenerHost() {

    @EventHandler
    suspend fun onGroupMessage(event: GroupMessageEvent) {
        val configPermission = LoliConfig.enableGroups.contains(event.group.id)

        // 权限 关键词
        if (configPermission || LoliYouWant.PERM_RANDOM.anyHasPerm(
            event.group, event.sender
        )) {
            // 捕捉关键词
            val at = event.message.filterIsInstance<At>().any { it.target == event.bot.id }
            val keyword = LoliConfig.resolveKeyword(event.message, at)
            if (keyword != null) {
                executeKeyword(event, keyword)
                return
            }
        }
        // 权限 搜索
        if (configPermission || LoliYouWant.PERM_SEARCH.anyHasPerm(
            event.group, event.sender
        )) {
            val at = event.message.filterIsInstance<At>().any { it.target == event.bot.id }
            if (LoliConfig.commandSearch.at && !at) return

            val key = event.message.filterIsInstance<PlainText>().joinToString { it.content }.trim()
            if (key.startsWith(LoliConfig.commandSearch.keywordPrefix)) {
                executeSearch(event, key.removePrefix(LoliConfig.commandSearch.keywordPrefix))
                return
            }
        }
    }

    @EventHandler
    suspend fun onFriendMessage(event: FriendMessageEvent) {
        // 权限 关键词
        if (LoliYouWant.PERM_RANDOM.anyHasPerm(event.sender)) {
            // 捕捉关键词
            val at = event.message.filterIsInstance<At>().any { it.target == event.bot.id }
            val keyword = LoliConfig.resolveKeyword(event.message, at)
            if (keyword != null) {
                executeKeyword(event, keyword)
                return
            }
        }
        // 权限 搜索
        if (LoliYouWant.PERM_SEARCH.anyHasPerm(event.sender)) {
            val at = event.message.filterIsInstance<At>().any { it.target == event.bot.id }
            if (LoliConfig.commandSearch.at && !at) return

            val key = event.message.filterIsInstance<PlainText>().joinToString { it.content }.trim()
            if (key.startsWith(LoliConfig.commandSearch.keywordPrefix)) {
                executeSearch(event, key.removePrefix(LoliConfig.commandSearch.keywordPrefix))
                return
            }
        }
    }

    private suspend fun executeSearch(
        event: MessageEvent,
        keyword: String
    ) = event.toCommandSender().search(*keyword.trim().split(" ").toTypedArray())

    private suspend fun executeKeyword(
        event: MessageEvent,
        keyword: Keyword
    ) {
        val replacement = mutableMapOf<String, SingleMessage>("quote" to QuoteReply(event.source))

        val cooldown = if (event.sender is Member) LoliYouWant.cooldown else LoliYouWant.cooldownFriend
        // 冷却
        if (!LoliYouWant.PERM_BYPASS_COOLDOWN.anyHasPerm(event.subject, event.sender)) {
            val cd = cooldown.getOrDefault(event.subject.id, 0)
            if (cd >= System.currentTimeMillis()) {
                replacement["cd"] = PlainText(((cd - System.currentTimeMillis()) / 1000L).toString())
                event.sender.sendMessage(LoliConfig.replyCooldown.replace(replacement))
                return
            }

            // 无权限时才设置冷却
            cooldown[event.subject.id] = System.currentTimeMillis() + LoliConfig.cooldown * 1000
        }

        // 经济系统
        if (!keyword.costMoney(event.subject.takeIf { it is Group }?.cast(), event.sender, event.source)) return

        // 获取图片并发送
        val result = fetchLoli(event.subject, keyword, replacement)

        if (keyword.recallFetchingMessage) result.second.recallIgnoreError()
        if (!result.first) {
            event.sender.sendMessage(keyword.replyFail.replace(replacement))
            cooldown[event.subject.id] = System.currentTimeMillis() + LoliConfig.failCooldown * 1000
        }
    }

    private suspend fun fetchLoli(
        contact: Contact,
        keyword: Keyword,
        defReplacement: MutableMap<String, SingleMessage>
    ): Pair<Boolean, MessageReceipt<Contact>> {
        val receipt = contact.sendMessage(keyword.replyFetching.replace(defReplacement))

        val tags = keyword.tags.resolveTagsParams()
        val lolies = arrayListOf<Loli>()
        var count = keyword.retryTimes
        while (count > 0) {
            for (loli in LoliYouWant.searchLolis(
                Lolibooru.get(40, Random.nextInt(1, 11), tags)
            )) {
                if (lolies.size >= keyword.count) break
                lolies.add(loli)
            }
            if (lolies.size >= keyword.count) break
            count--
        }
        if (lolies.isEmpty()) return Pair(false, receipt)
        if (keyword.count > 1) {
            val forward = ForwardMessageBuilder(contact.bot.asFriend)
            for (loli in lolies) {
                val replacement = defReplacement.plus(toReplacementCommand(loli, contact, keyword))
                forward.add(
                    contact.bot,
                    keyword.replySuccess.replace(replacement),
                    (System.currentTimeMillis() / 1000).toInt()
                )
            }
            contact.sendMessage(forward.build())
        } else {
            contact.sendMessage(
                keyword.replySuccess.replace(defReplacement.plus(
                    toReplacementCommand(lolies[0], contact, null)
                ))
            )
        }
        return Pair(true, receipt)
    }

    fun toReplacementCommand(loli: Loli, contact: Contact, keyword: Keyword? = null): Map<String, SingleMessage> {
        return loli.toReplacement0(
            contact,
            LoliConfig.command.quality,
            LoliConfig.command.imageFailDownload,
            LoliConfig.command.timeout,
            LoliConfig.command.download,
            LoliConfig.command.overrideDownloadPath,
            keyword
        )
    }
    fun toReplacementSearch(loli: Loli, contact: Contact, keyword: Keyword? = null): Map<String, SingleMessage> {
        return loli.toReplacement0(
            contact,
            LoliConfig.commandSearch.quality,
            LoliConfig.commandSearch.imageFailDownload,
            LoliConfig.commandSearch.timeout,
            LoliConfig.commandSearch.download,
            LoliConfig.commandSearch.overrideDownloadPath,
            keyword
        )
    }
}

private fun Loli.toReplacement0(
    contact: Contact,
    quality: String,
    imageFailDownload: String,
    timeout: Int,
    download: Boolean,
    overrideDownloadPath: String,
    keyword: Keyword? = null
): Map<String, SingleMessage> {
    val picUrl = browserLikeUrlEncode(when (keyword?.quality ?: quality) {
        "FILE" -> url
        "PREVIEW" -> urlPreview
        else -> urlSample
    })
    return mapOf(
        "id" to PlainText(id.toString()),
        "previewUrl" to PlainText(browserLikeUrlEncode(urlPreview)),
        "sampleUrl" to PlainText(browserLikeUrlEncode(urlSample)),
        "fileUrl" to PlainText(browserLikeUrlEncode(url)),
        "url" to PlainText(picUrl),
        "tags" to PlainText(tags),
        "rating" to PlainText(rating),
        "pic" to PrepareUploadImage.url(
            contact, picUrl,
            keyword?.imageFailDownload ?: imageFailDownload,
            keyword?.timeout ?: timeout
        ) { input ->
            if (!(keyword?.download ?: download)) return@url input
            val folder =
                LoliYouWant.resolveDataFile((keyword?.overrideDownloadPath ?: overrideDownloadPath)
                    .replace("\\", "/").removeSurrounding("/"))
            if (!folder.exists()) folder.mkdirs()
            val file = File(folder, urlDecode(picUrl).substringAfterLast('/'))

            file.writeBytes(input.readBytes())
            return@url FileInputStream(file)
        }
    )
}

fun List<String>.resolveTagsParams() = mutableListOf("order:random", "-rating:e", "-video").also { paramTags ->
    if (LoliConfig.doesAddTagsToParams) paramTags.addAll(LoliYouWant.blacklistTags.map { "-$it" })
    paramTags.addAll(filter { !paramTags.contains("-$it") && !it.contains("rating:") && !it.contains("order:") })
}.joinToString(" ")

val Contact.permitteeIdOrNull: PermitteeId?
    get() = when (this) {
        is User -> this.permitteeId
        is Group -> this.permitteeId
        else -> null
    }

fun Permission.anyHasPerm(vararg users: Contact): Boolean = users.any {
    testPermission(it.permitteeIdOrNull ?: return@any false)
}
