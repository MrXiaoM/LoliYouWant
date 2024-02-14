package top.mrxiaom.loliyouwant

import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.events.UserMessageEvent
import net.mamoe.mirai.message.data.*
import kotlin.random.Random

object LoliCommand: CompositeCommand(
    owner = LoliYouWant,
    primaryName = "LoliYouWant",
    secondaryNames = arrayOf("loli", "luw"),
    description = "LoliYouWant 主命令"
) {
    @SubCommand
    @Description("获取一张图片")
    @OptIn(ConsoleExperimentalApi::class)
    suspend fun CommandSenderOnMessage<MessageEvent>.get(
        @Name("关键词") vararg keywords: String
    ) {
        val replacement: MutableMap<String, SingleMessage> = mutableMapOf("quote" to QuoteReply(fromEvent.source))
        if (fromEvent.sender is Member) replacement["at"] = At(fromEvent.sender)

        val cooldown = if (fromEvent.sender is Member) LoliYouWant.cooldown else LoliYouWant.cooldownFriend
        // 冷却
        if (!anyHasPerm(LoliYouWant.PERM_BYPASS_COOLDOWN, fromEvent.subject, fromEvent.sender)) {
            val cd = cooldown.getOrDefault(fromEvent.subject.id, 0)
            if (cd >= System.currentTimeMillis()) {
                replacement["cd"] = PlainText(((cd - System.currentTimeMillis()) / 1000L).toString())
                sendMessage(LoliConfig.replyCooldown.replace(replacement))
                return
            }

            // 无权限时才设置冷却
            cooldown[fromEvent.subject.id] = System.currentTimeMillis() + LoliConfig.cooldown * 1000
        }

        // TODO 经济系统

        val receipt = sendMessage(LoliConfig.replyFetching.replace(replacement))

        val tags = keywords.toList().resolveTagsParams()
        val loli = run {
            var count = LoliConfig.commandRetryTimes
            var result: Loli? = null
            while (result == null && count > 0) {
                result = LoliYouWant.searchLolis(
                    Lolibooru.get(40, Random.nextInt(1, 11), tags)
                ).randomOrNull()
                count--
            }
            result
        }
        if (loli == null) {
            if (LoliConfig.recallFetchingMessage) receipt?.recallIgnoreError()
            sendMessage(LoliConfig.replyFail.replace(replacement))

            cooldown[fromEvent.subject.id] = System.currentTimeMillis() + LoliConfig.failCooldown * 1000
            return
        }

        replacement.putAll(loli.toReplacement(fromEvent.sender, null))

        sendMessage(LoliConfig.replySuccess.replace(replacement))
        if (LoliConfig.recallFetchingMessage) receipt?.recallIgnoreError()
    }
    @SubCommand
    @Description("获取N张图片")
    @OptIn(ConsoleExperimentalApi::class)
    suspend fun CommandSenderOnMessage<MessageEvent>.list(
        @Name("图片数量") count: Int,
        @Name("关键词") vararg keywords: String
    ) {
        if (count > LoliConfig.maxSearchCount) {
            this.sendMessage(buildMessageChain {
                if (LoliConfig.maxSearchCountWarn.contains("\$quote")) add(QuoteReply(fromEvent.source))
                addAll(Regex("\\\$at").split<SingleMessage>(
                    LoliConfig.maxSearchCountWarn
                        .replace("\$count", LoliConfig.maxSearchCount.toString())
                        .replace("\$quote", "")
                ) { s, isMatched ->
                    if (isMatched) At(fromEvent.sender.id) else PlainText(s)
                })
            })
            return
        }
        val replacement: MutableMap<String, SingleMessage> = mutableMapOf("quote" to QuoteReply(fromEvent.source))
        if (fromEvent.sender is Member) replacement["at"] = At(fromEvent.sender)

        val cooldown = if (fromEvent.sender is Member) LoliYouWant.cooldown else LoliYouWant.cooldownFriend
        // 冷却
        if (!anyHasPerm(LoliYouWant.PERM_BYPASS_COOLDOWN, fromEvent.subject, fromEvent.sender)) {
            val cd = cooldown.getOrDefault(fromEvent.subject.id, 0)
            if (cd >= System.currentTimeMillis()) {
                replacement["cd"] = PlainText(((cd - System.currentTimeMillis()) / 1000L).toString())
                sendMessage(LoliConfig.replyCooldown.replace(replacement))
                return
            }

            // 无权限时才设置冷却
            cooldown[fromEvent.subject.id] = System.currentTimeMillis() + LoliConfig.cooldown * 1000
        }

        // TODO 经济系统

        val receipt = sendMessage(LoliConfig.replyFetching.replace(replacement))

        val tags = keywords.toList().resolveTagsParams()
        val lolies = arrayListOf<Loli>()
        var retryCount = LoliConfig.commandRetryTimes
        while (retryCount > 0) {
            for (loli in LoliYouWant.searchLolis(
                Lolibooru.get(40, Random.nextInt(1, 11), tags)
            )) {
                if (lolies.size >= count) break
                lolies.add(loli)
            }
            if (lolies.size >= count) break
            retryCount--
        }
        if (lolies.isEmpty()) {
            if (LoliConfig.recallFetchingMessage) receipt?.recallIgnoreError()
            sendMessage(LoliConfig.replyFail.replace(replacement))

            cooldown[fromEvent.subject.id] = System.currentTimeMillis() + LoliConfig.failCooldown * 1000
            return
        }

        val forward = ForwardMessageBuilder(fromEvent.bot.asFriend)

        for (loli in lolies) {
            val replace = replacement.toMutableMap()
            replace.putAll(loli.toReplacement(fromEvent.sender, null))
            forward.add(
                fromEvent.bot,
                LoliConfig.replySuccess.replace(replace),
                (System.currentTimeMillis() / 1000).toInt()
            )
        }
        sendMessage(forward.build())
        if (LoliConfig.recallFetchingMessage) receipt?.recallIgnoreError()
    }
}