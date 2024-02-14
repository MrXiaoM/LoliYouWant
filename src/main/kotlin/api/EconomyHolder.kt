package top.mrxiaom.loliyouwant.api

import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.utils.MiraiLogger
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
}