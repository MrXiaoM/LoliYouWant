package top.mrxiaom.loliyouwant.utils

import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.MiraiLogger
import top.mrxiaom.loliyouwant.split
import xyz.cssxsh.mirai.economy.EconomyService
import xyz.cssxsh.mirai.economy.economy
import xyz.cssxsh.mirai.economy.globalEconomy

object EconomyHolder {
    val logger = MiraiLogger.Factory.create(this::class)
    val hasEconomyCorePlugin by lazy {
        try {
            EconomyService
            true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * 金钱扣除结果
     */
    enum class CostResult {
        SUCCESS, NOT_ENOUGH, NO_CURRENCY, NO_PLUGIN
    }

    /**
     * 检查并扣除金钱
     * @return 金钱扣除结果
     */
    fun costMoney(
        user: User,
        currencyName: String,
        money: Double
    ): CostResult {
        if (!hasEconomyCorePlugin) return CostResult.NO_PLUGIN
        val currency = EconomyService.basket[currencyName] ?: return CostResult.NO_CURRENCY
        return globalEconomy {
            val account = service.account(user)
            if (account[currency] < money) return@globalEconomy CostResult.NOT_ENOUGH
            account -= (currency to money)
            return@globalEconomy CostResult.SUCCESS
        }
    }

    /**
     * 检查并扣除金钱
     * @return 金钱扣除结果
     */
    fun costMoney(
        group: Group,
        user: User,
        currencyName: String,
        money: Double
    ) : CostResult {
        if (!hasEconomyCorePlugin) return CostResult.NO_PLUGIN
        val currency = EconomyService.basket[currencyName] ?: return CostResult.NO_CURRENCY
        return group.economy {
            val account = service.account(user)
            if (account[currency] < money) return@economy CostResult.NOT_ENOUGH
            account -= (currency to money)
            return@economy CostResult.SUCCESS
        }
    }

    suspend fun costMoney(
        group: Group?,
        user: User,
        source: MessageSource,
        costMoney: Double,
        costMoneyGlobal: Boolean,
        costMoneyCurrency: String,
        costMoneyNotEnough: String,
    ): Boolean = when(
        if (costMoneyGlobal || group == null) costMoney(user, costMoneyCurrency, costMoney)
        else costMoney(group, user, costMoneyCurrency, costMoney)
    ) {
        CostResult.NO_CURRENCY -> false.also { logger.warning("货币种类 `$costMoneyCurrency` 不存在") }
        CostResult.NOT_ENOUGH -> false.also {
            (group ?: user).sendMessage(buildMessageChain {
                if (costMoneyNotEnough.contains("\$quote")) add(QuoteReply(source))
                addAll(Regex("\\\$at").split<SingleMessage>(
                    costMoneyNotEnough
                        .replace("\$cost", costMoney.toString())
                        .replace("\$quote", "")
                ) { s, isMatched ->
                    if (isMatched) At(user.id) else PlainText(s)
                })
            })
        }
        else -> true
    }
}