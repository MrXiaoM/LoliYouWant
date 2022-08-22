import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.enable
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.load
import net.mamoe.mirai.console.terminal.MiraiConsoleImplementationTerminal
import net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import top.mrxiaom.loliyouwant.LoliYouWant
import java.io.File

@OptIn(ConsoleExperimentalApi::class)
suspend fun main() {
    val runDir = File("run")
    if (!runDir.exists()) runDir.mkdirs()
    MiraiConsoleTerminalLoader.startAsDaemon(MiraiConsoleImplementationTerminal(runDir.toPath()))

    val pluginInstance = LoliYouWant

    pluginInstance.load() // 主动加载插件, Console 会调用 SpecialTitleYouWant.onLoad
    pluginInstance.enable() // 主动启用插件, Console 会调用 SpecialTitleYouWant.onEnable

    //val bot = MiraiConsole.addBot(123456, "").alsoLogin() // 登录一个测试环境的 Bot

    MiraiConsole.job.join()
}