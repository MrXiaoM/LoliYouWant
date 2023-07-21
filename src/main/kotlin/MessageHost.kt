package top.mrxiaom.loliyouwant

import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.permission.PermitteeId
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.*
import java.io.File
import java.io.FileInputStream
import kotlin.random.Random

object MessageHost : SimpleListenerHost() {

    @EventHandler
    suspend fun onGroupMessage(event: GroupMessageEvent) {
        // 权限
        if (!LoliConfig.enableGroups.contains(event.group.id) && !anyHasPerm(
                LoliYouWant.PERM_RANDOM,
                event.group,
                event.sender
            )
        ) return

        // 捕捉关键词
        val at = event.message.filterIsInstance<At>().any { it.target == event.bot.id }
        val keyword = LoliConfig.resolveKeyword(event.message, at) ?: return

        val replacement = mutableMapOf("quote" to QuoteReply(event.source), "at" to At(event.sender))

        // 冷却
        if (!anyHasPerm(LoliYouWant.PERM_BYPASS_COOLDOWN, event.group, event.sender)) {
            val cd = LoliYouWant.cooldown.getOrDefault(event.group.id, 0)
            if (cd >= System.currentTimeMillis()) {
                replacement["cd"] = PlainText(((cd - System.currentTimeMillis()) / 1000L).toString())
                event.group.sendMessage(LoliConfig.replyCooldown.replace(replacement))
                return
            }

            // 无权限时才设置冷却
            LoliYouWant.cooldown[event.group.id] = System.currentTimeMillis() + LoliConfig.cooldown * 1000
        }

        // 经济系统
        if (!keyword.costMoney(event.group, event.sender, event.source)) return

        // 获取图片并发送
        val result =
            if (keyword.count > 1) sendLoliPictureCollection(event.group, keyword, replacement) else sendLoliPicture(
                event.group,
                keyword,
                replacement
            )
        if (keyword.recallFetchingMessage) result.second.recallIgnoreError()
        if (!result.first) {
            event.group.sendMessage(keyword.replyFail.replace(replacement))
            LoliYouWant.cooldown[event.group.id] = System.currentTimeMillis() + LoliConfig.failCooldown * 1000
        }
    }

    @EventHandler
    suspend fun onFriendMessage(event: FriendMessageEvent) {
        // 权限
        if (!anyHasPerm(LoliYouWant.PERM_RANDOM, event.sender)) return

        // 捕捉关键词
        val at = event.message.filterIsInstance<At>().any { it.target == event.bot.id }
        val keyword = LoliConfig.resolveKeyword(event.message, at) ?: return

        val replacement = mutableMapOf<String, SingleMessage>("quote" to QuoteReply(event.source))

        // 冷却
        if (!anyHasPerm(LoliYouWant.PERM_BYPASS_COOLDOWN, event.sender)) {
            val cd = LoliYouWant.cooldownFriend.getOrDefault(event.sender.id, 0)
            if (cd >= System.currentTimeMillis()) {
                replacement["cd"] = PlainText(((cd - System.currentTimeMillis()) / 1000L).toString())
                event.sender.sendMessage(LoliConfig.replyCooldown.replace(replacement))
                return
            }

            // 无权限时才设置冷却
            LoliYouWant.cooldownFriend[event.sender.id] = System.currentTimeMillis() + LoliConfig.cooldown * 1000
        }

        // 经济系统
        if (!keyword.costMoney(null, event.sender, event.source)) return

        // 获取图片并发送
        val result =
            if (keyword.count > 1) sendLoliPictureCollection(event.friend, keyword, replacement) else sendLoliPicture(
                event.sender,
                keyword,
                replacement
            )
        if (keyword.recallFetchingMessage) result.second.recallIgnoreError()
        if (!result.first) {
            event.sender.sendMessage(keyword.replyFail.replace(replacement))
            LoliYouWant.cooldownFriend[event.friend.id] = System.currentTimeMillis() + LoliConfig.failCooldown * 1000
        }
    }

    private suspend fun sendLoliPicture(
        contact: Contact,
        keyword: LoliConfig.Keyword,
        replacement: MutableMap<String, SingleMessage>
    ): Pair<Boolean, MessageReceipt<Contact>> {
        val receipt = contact.sendMessage(keyword.replyFetching.replace(replacement))

        val tags = keyword.tags.resolveTagsParams()
        val loli = LoliYouWant.searchLolis(Lolibooru.get(10, Random.nextInt(1, 11), tags)).randomOrNull()
            ?: return Pair(false, receipt)

        replacement.putAll(loli.toReplacement(contact, keyword))

        contact.sendMessage(keyword.replySuccess.replace(replacement))
        return Pair(true, receipt)
    }

    private suspend fun sendLoliPictureCollection(
        contact: Contact,
        keyword: LoliConfig.Keyword,
        defReplacement: MutableMap<String, SingleMessage>
    ): Pair<Boolean, MessageReceipt<Contact>> {
        val receipt = contact.sendMessage(keyword.replyFetching.replace(defReplacement))

        val tags = keyword.tags.resolveTagsParams()
        val lolies = LoliYouWant.searchLolis(Lolibooru.get(40, Random.nextInt(1, 11), tags)).chunked(keyword.count)[0]
        if (lolies.isEmpty()) return Pair(false, receipt)

        val forward = ForwardMessageBuilder(contact.bot.asFriend)

        for (loli in lolies) {
            val replacement = defReplacement.toMutableMap()
            replacement.putAll(loli.toReplacement(contact, keyword))

            forward.add(
                contact.bot,
                keyword.replySuccess.replace(replacement),
                (System.currentTimeMillis() / 1000).toInt()
            )
        }
        contact.sendMessage(forward.build())
        return Pair(true, receipt)
    }
}

fun Loli.toReplacement(contact: Contact, keyword: LoliConfig.Keyword? = null): Map<String, SingleMessage> {
    val picUrl = browserLikeUrlEncode(when (keyword?.quality ?: LoliConfig.quality) {
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
            keyword?.imageFailDownload ?: LoliConfig.imageFailDownload,
            keyword?.timeout ?: LoliConfig.timeout
        ) { input ->
            if (!(keyword?.download ?: LoliConfig.download)) return@url input
            val folder =
                LoliYouWant.resolveDataFile((keyword?.overrideDownloadPath ?: LoliConfig.overrideDownloadPath)
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

fun anyHasPerm(p: Permission, vararg users: Contact): Boolean = users.any {
    p.testPermission(it.permitteeIdOrNull ?: return@any false)
}
