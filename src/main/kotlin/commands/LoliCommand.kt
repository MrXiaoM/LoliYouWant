package top.mrxiaom.loliyouwant.commands

import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.cast
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.*
import top.mrxiaom.loliyouwant.*
import top.mrxiaom.loliyouwant.api.BingTranslate
import top.mrxiaom.loliyouwant.api.Loli
import top.mrxiaom.loliyouwant.api.Lolibooru
import top.mrxiaom.loliyouwant.utils.replace
import java.util.regex.Pattern
import kotlin.random.Random

object LoliCommand: CompositeCommand(
    owner = LoliYouWant,
    primaryName = "LoliYouWant",
    secondaryNames = arrayOf("loli", "luw"),
    description = "LoliYouWant 主命令"
) {

    @SubCommand
    @Description("获取图片")
    @OptIn(ConsoleExperimentalApi::class)
    suspend fun CommandSenderOnMessage<MessageEvent>.get(
        @Name("[数量] 关键词") vararg keywords: String
    ) {
        val count: Int = keywords[0].toIntOrNull() ?: 1
        val replacement: MutableMap<String, SingleMessage> = mutableMapOf("quote" to QuoteReply(fromEvent.source))

        // 图片数量、冷却、经济系统等需求
        val cooldown = requirements(count, replacement) ?: return

        // 发送正在获取提示
        val receipt = sendMessage(LoliConfig.replyFetching.replace(replacement))

        val text = (if (keywords[0].toIntOrNull() == null) keywords.toList()
        else keywords.drop(1))

        // 正式开始获取图片
        fetchImage(count, text, receipt, cooldown, replacement)
    }

    @SubCommand
    @Description("搜索Tag并获取图片")
    @OptIn(ConsoleExperimentalApi::class)
    suspend fun CommandSenderOnMessage<MessageEvent>.search(
        @Name("[数量] 关键词") vararg keywords: String
    ) {
        val count: Int = keywords[0].toIntOrNull() ?: 1
        val replacement: MutableMap<String, SingleMessage> = mutableMapOf("quote" to QuoteReply(fromEvent.source))

        // 图片数量、冷却、经济系统等需求
        val cooldown = requirements(count, replacement) ?: return

        // 发送正在搜索提示
        var receipt = sendMessage(LoliConfig.replySearching.replace(replacement))

        val text = (if (keywords[0].toIntOrNull() == null) keywords.toList()
        else keywords.drop(1)).joinToString(" ").replace("\n", " ")

        replacement["text"] = PlainText(text)

        // 检查是否需要翻译
        val pattern = if (Pattern.matches(".*[\u4e00-\u9fa5]+.*", text)) runCatching {
            BingTranslate.invoke(text)
        }.onFailure {
            LoliYouWant.logger.warning("翻译“$text”失败: ", it)
            if (LoliConfig.recallFetchingMessage) receipt?.recallIgnoreError()
            sendMessage(LoliConfig.replySearchTranslateFailed.replace(replacement))
        }.getOrNull() ?: return else text

        // 正式开始搜索 tags
        val tagsFetched = Lolibooru.search(Lolibooru.baseUrl, pattern).map { it.name }

        // 撤回上一条提示
        if (LoliConfig.recallFetchingMessage) receipt?.recallIgnoreError()

        // 不允许空 tags 列表
        if (tagsFetched.isEmpty()) {
            sendMessage(LoliConfig.replySearchEmpty.replace(replacement))
            return
        }

        // 提示用户当前 tags 列表，并发送获取中提示
        replacement["tags"] = PlainText(tagsFetched.joinToString(", "))
        receipt = sendMessage(LoliConfig.replySearchFetching.replace(replacement))

        // 正式开始获取图片
        fetchImage(count, tagsFetched, receipt, cooldown, replacement)
    }
}

private suspend fun CommandSenderOnMessage<MessageEvent>.requirements(
    count: Int,
    replacement: MutableMap<String, SingleMessage>
): MutableMap<Long, Long>? {
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
        return null
    }
    if (fromEvent.sender is Member) replacement["at"] = At(fromEvent.sender)

    val cooldown = if (fromEvent.sender is Member) LoliYouWant.cooldown else LoliYouWant.cooldownFriend
    // 冷却
    if (!anyHasPerm(LoliYouWant.PERM_BYPASS_COOLDOWN, fromEvent.subject, fromEvent.sender)) {
        val cd = cooldown.getOrDefault(fromEvent.subject.id, 0)
        if (cd >= System.currentTimeMillis()) {
            replacement["cd"] = PlainText(((cd - System.currentTimeMillis()) / 1000L).toString())
            sendMessage(LoliConfig.replyCooldown.replace(replacement))
            return null
        }

        // 无权限时才设置冷却
        cooldown[fromEvent.subject.id] = System.currentTimeMillis() + LoliConfig.cooldown * 1000
    }

    // 经济系统
    if (!LoliConfig.commandEconomy.costMoney(
            group = fromEvent.subject.takeIf { it is Group }?.cast(),
            user = fromEvent.sender,
            source = fromEvent.source,
            count = count)) return null
    return cooldown
}

private suspend fun CommandSenderOnMessage<MessageEvent>.fetchImage(
    count: Int,
    tagsFetched: List<String>,
    receipt: MessageReceipt<Contact>?,
    cooldown: MutableMap<Long, Long>,
    replacement: MutableMap<String, SingleMessage>
) {

    // 正式开始获取图片
    val tags = tagsFetched.resolveTagsParams()
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
    // 列表为空则获取失败
    if (lolies.isEmpty()) {
        if (LoliConfig.recallFetchingMessage) receipt?.recallIgnoreError()
        sendMessage(LoliConfig.replyFail.replace(replacement))

        cooldown[fromEvent.subject.id] = System.currentTimeMillis() + LoliConfig.failCooldown * 1000
        return
    }
    if (count > 1) {
        val forward = ForwardMessageBuilder(fromEvent.bot.asFriend)

        for (loli in lolies) {
            forward.add(
                fromEvent.bot,
                LoliConfig.replySuccess.replace(replacement.plus(
                    loli.toReplacement(fromEvent.sender, null)
                )),
                (System.currentTimeMillis() / 1000).toInt()
            )
        }
        sendMessage(forward.build())
    } else {
        sendMessage(LoliConfig.replySuccess.replace(replacement.plus(
            lolies[0].toReplacement(fromEvent.sender, null)
        )))
    }
    if (LoliConfig.recallFetchingMessage) receipt?.recallIgnoreError()
}
