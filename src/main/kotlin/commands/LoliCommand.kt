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
import top.mrxiaom.loliyouwant.api.Loli
import top.mrxiaom.loliyouwant.api.Lolibooru
import top.mrxiaom.loliyouwant.api.TencentTranslate
import top.mrxiaom.loliyouwant.config.LoliConfig
import top.mrxiaom.loliyouwant.config.entity.CommandEconomy
import top.mrxiaom.loliyouwant.config.entity.Keyword
import top.mrxiaom.loliyouwant.config.split
import top.mrxiaom.loliyouwant.utils.EnglishWordUtil
import top.mrxiaom.loliyouwant.utils.replace
import top.mrxiaom.loliyouwant.utils.singularize
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
        val cooldown = requirements(
            LoliConfig.command.maxSearchCount,
            LoliConfig.command.maxSearchCountWarn,
            LoliConfig.commandEconomy,
            count, replacement) ?: return

        // 发送正在获取提示
        val receipt = sendMessage(LoliConfig.command.replyFetching.replace(replacement))

        val text = (if (keywords[0].toIntOrNull() == null) keywords.toList()
        else keywords.drop(1))

        // 正式开始获取图片
        fetchImage(
            MessageHost::toReplacementCommand,
            LoliConfig.command.recallFetchingMessage,
            LoliConfig.command.replyFail,
            LoliConfig.command.replySuccess,
            count, text, receipt, cooldown, replacement
        )
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
        val cooldown = requirements(
            LoliConfig.commandSearch.maxSearchCount,
            LoliConfig.commandSearch.maxSearchCountWarn,
            LoliConfig.commandEconomySearch,
            count, replacement) ?: return

        // 发送正在搜索提示
        var receipt = sendMessage(LoliConfig.commandSearch.replySearching.replace(replacement))

        val text = (if (keywords[0].toIntOrNull() == null) keywords.toList()
        else keywords.drop(1)).joinToString(" ").replace("\n", " ")

        replacement["text"] = PlainText(text)

        // 检查是否需要翻译
        val pattern = if (Pattern.matches(".*[\u4e00-\u9fa5]+.*", text)) runCatching {
            val key = TencentTranslate.clientKey

            // 先 中译日 再 日译英，否则可能会出现 `和泉纱雾` 翻译成 `And spring yarn fog` 的情况
            val ja = TencentTranslate(Lolibooru.split(text), "zh", "ja", key)
            val en = TencentTranslate(ja, "ja", "en", key)

            val tags = en.toMutableList()
            // 腾讯交互翻译 日译英 喜欢用英文习惯(姓在后)书写日文人名，所以倒一倒
            tags.addAll(en
                .filter { it.contains(" ") }
                .map { it.split(" ", limit = 2).reversed().joinToString(" ") }
            )
            tags.joinToString(",").singularize()
        }.onFailure {
            LoliYouWant.logger.warning("翻译“$text”失败: ", it)
            if (LoliConfig.commandSearch.recallFetchingMessage) receipt?.recallIgnoreError()
            sendMessage(LoliConfig.commandSearch.replySearchTranslateFailed.replace(replacement))
        }.getOrNull() ?: return else text

        // 正式开始搜索 tags
        val tagsFetched = Lolibooru.search(Lolibooru.baseUrl, pattern).map { it.name }

        // 撤回上一条提示
        if (LoliConfig.commandSearch.recallFetchingMessage) receipt?.recallIgnoreError()

        // 不允许空 tags 列表
        if (tagsFetched.isEmpty()) {
            sendMessage(LoliConfig.commandSearch.replySearchEmpty.replace(replacement))
            return
        }

        // 提示用户当前 tags 列表，并发送获取中提示
        replacement["tags"] = PlainText(tagsFetched.joinToString(", "))
        receipt = sendMessage(LoliConfig.commandSearch.replySearchFetching.replace(replacement))

        // 正式开始获取图片
        fetchImage(
            MessageHost::toReplacementSearch,
            LoliConfig.commandSearch.recallFetchingMessage,
            LoliConfig.commandSearch.replyFail,
            LoliConfig.commandSearch.replySuccess,
            count, tagsFetched, receipt, cooldown, replacement)
    }
}

private suspend fun CommandSenderOnMessage<MessageEvent>.requirements(
    maxSearchCount: Int,
    maxSearchCountWarn: String,
    commandEconomy: CommandEconomy,
    count: Int,
    replacement: MutableMap<String, SingleMessage>
): MutableMap<Long, Long>? {
    if (count > maxSearchCount) {
        this.sendMessage(buildMessageChain {
            if (maxSearchCountWarn.contains("\$quote")) add(QuoteReply(fromEvent.source))
            addAll(Regex("\\\$at").split<SingleMessage>(
                maxSearchCountWarn
                    .replace("\$count", maxSearchCount.toString())
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
    if (!LoliYouWant.PERM_BYPASS_COOLDOWN.anyHasPerm(fromEvent.subject, fromEvent.sender)) {
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
    if (!commandEconomy.costMoney(
            group = fromEvent.subject.takeIf { it is Group }?.cast(),
            user = fromEvent.sender,
            source = fromEvent.source,
            count = count)) return null
    return cooldown
}

private suspend fun CommandSenderOnMessage<MessageEvent>.fetchImage(
    replacementFunction: (Loli, Contact, Keyword?) -> Map<String, SingleMessage>,
    recallFetchingMessage: Boolean,
    replyFail: String,
    replySuccess: String,
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
        if (recallFetchingMessage) receipt?.recallIgnoreError()
        sendMessage(replyFail.replace(replacement))

        cooldown[fromEvent.subject.id] = System.currentTimeMillis() + LoliConfig.failCooldown * 1000
        return
    }
    if (count > 1) {
        val forward = ForwardMessageBuilder(fromEvent.bot.asFriend)

        for (loli in lolies) {
            forward.add(
                fromEvent.bot,
                replySuccess.replace(replacement.plus(
                    replacementFunction(loli, fromEvent.sender, null)
                )),
                (System.currentTimeMillis() / 1000).toInt()
            )
        }
        sendMessage(forward.build())
    } else {
        sendMessage(
            replySuccess.replace(replacement.plus(
                replacementFunction(lolies[0], fromEvent.sender, null)
            ))
        )
    }
    if (recallFetchingMessage) receipt?.recallIgnoreError()
}
