package top.mrxiaom.loliyouwant

import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.SingleMessage
import java.io.FileInputStream

object MessageHost : SimpleListenerHost() {
    @EventHandler
    suspend fun onGroupMessage(event: GroupMessageEvent) {
        if (LoliConfig.at && event.message.filterIsInstance<At>()
                .none { it.target == event.bot.id }
        ) return
        if (!LoliConfig.enableGroups.contains(event.group.id) && !LoliYouWant.PERM_RANDOM.testPermission(event.group.permitteeId) && !LoliYouWant.PERM_RANDOM.testPermission(
                event.sender.permitteeId
            )
        ) return
        if (!LoliConfig.keywords.contains(event.message.filterIsInstance<PlainText>().joinToString { it.content }
                .trimStart().trimEnd())) return
        val replacement = mutableMapOf("quote" to QuoteReply(event.source), "at" to At(event.sender))
        if (!LoliYouWant.PERM_BYPASS_COOLDOWN.testPermission(event.group.permitteeId) && !LoliYouWant.PERM_BYPASS_COOLDOWN.testPermission(
                event.sender.permitteeId
            )
        ) {
            val cd = LoliYouWant.cooldown.getOrDefault(event.group.id, 0)
            if (cd >= System.currentTimeMillis()) {
                replacement["cd"] = PlainText(((cd - System.currentTimeMillis()) / 1000L).toString())
                event.group.sendMessage(LoliConfig.replyCooldown.replace(replacement))
                return
            }
        }
        LoliYouWant.cooldown[event.group.id] = System.currentTimeMillis() + LoliConfig.cooldown * 1000
        val result = sendLoliPicture(event.group, replacement)
        if (LoliConfig.recallFetchingMessage) result.second.recallIgnoreError()
        if (!result.first) {
            event.group.sendMessage(LoliConfig.replyFail.replace(replacement))
            LoliYouWant.cooldown[event.group.id] = System.currentTimeMillis() + LoliConfig.failCooldown * 1000
        }
    }

    @EventHandler
    suspend fun onFriendMessage(event: FriendMessageEvent) {
        if (!LoliYouWant.PERM_RANDOM.testPermission(event.sender.permitteeId)) return
        if (!LoliConfig.keywords.contains(event.message.filterIsInstance<PlainText>().joinToString { it.content }
                .trimStart().trimEnd())) return
        val replacement = mutableMapOf<String, SingleMessage>("quote" to QuoteReply(event.source))
        if (!LoliYouWant.PERM_BYPASS_COOLDOWN.testPermission(event.sender.permitteeId)) {
            val cd = LoliYouWant.cooldownFriend.getOrDefault(event.sender.id, 0)
            if (cd >= System.currentTimeMillis()) {
                replacement["cd"] = PlainText(((cd - System.currentTimeMillis()) / 1000L).toString())
                event.sender.sendMessage(LoliConfig.replyCooldown.replace(replacement))
                return
            }
        }
        LoliYouWant.cooldownFriend[event.sender.id] = System.currentTimeMillis() + LoliConfig.cooldown * 1000
        val result = sendLoliPicture(event.sender, replacement)
        if (LoliConfig.recallFetchingMessage) result.second.recallIgnoreError()
        if (!result.first) {
            event.sender.sendMessage(LoliConfig.replyFail.replace(replacement))
            LoliYouWant.cooldownFriend[event.sender.id] = System.currentTimeMillis() + LoliConfig.failCooldown * 1000
        }
    }

    private suspend fun sendLoliPicture(
        contact: Contact,
        keyword: LoliConfig.Keyword,
        replacement: MutableMap<String, SingleMessage>
    ): Pair<Boolean, MessageReceipt<Contact>> {
        val receipt = contact.sendMessage(keyword.replyFetching.replace(replacement))

        val tags = keyword.tags.filter { !it.contains("rating:") }.joinToString(" ")
        val loli = LoliYouWant.searchLolis(Lolibooru.get(10, 1, "order:random -rating:e -video $tags")).randomOrNull()
            ?: return Pair(false, receipt)

        val url = when (keyword.quality) {
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
                contact, url, keyword.imageFailDownload
            ) { input ->
                if (!keyword.download) return@url input
                val path = keyword.overrideDownloadPath.replace("\\", "/").removeSurrounding("/") + "/"
                val file = LoliYouWant.resolveDataFile(path + url.substringAfterLast('/').replace("%20", " "))

                file.writeBytes(input.readBytes())
                return@url FileInputStream(file)
            }
        ))

        contact.sendMessage(keyword.replySuccess.replace(replacement))
        return Pair(true, receipt)
    }

    private suspend fun sendLoliPictureCollection(
        contact: Contact,
        keyword: LoliConfig.Keyword,
        defReplacement: MutableMap<String, SingleMessage>
    ): Pair<Boolean, MessageReceipt<Contact>> {
        val receipt = contact.sendMessage(keyword.replyFetching.replace(defReplacement))

        val tags = keyword.tags.filter { !it.contains("rating:") }.joinToString(" ")
        val lolies = LoliYouWant.searchLolis(Lolibooru.get(40, 1, "order:random -rating:e -video $tags"))
        if (lolies.isEmpty()) return Pair(false, receipt)

        val forward = ForwardMessageBuilder(contact.bot.asFriend)

        var count = 0
        for (loli in lolies) {
            if (count >= keyword.count) break

            val replacement = mutableMapOf<String, SingleMessage>()
            replacement.putAll(defReplacement)

            val url = when (keyword.quality) {
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
                    contact, url, keyword.imageFailDownload
                ) { input ->
                    if (!keyword.download) return@url input
                    val path = keyword.overrideDownloadPath.replace("\\", "/").removeSurrounding("/") + "/"
                    val file = LoliYouWant.resolveDataFile(path + url.substringAfterLast('/').replace("%20", " "))

                    file.writeBytes(input.readBytes())
                    return@url FileInputStream(file)
                }
            ))

            forward.add(
                contact.bot,
                keyword.replySuccess.replace(replacement),
                (System.currentTimeMillis() / 1000).toInt()
            )
            count++
        }
        contact.sendMessage(forward.build())
        return Pair(true, receipt)
    }
}

