package top.mrxiaom.loliyouwant.api

import kotlinx.serialization.json.*
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import java.util.*

object TencentTranslate {

    val clientKey: String
        get() = "browser-edge-chromium-121.0.0-Windows_10-${UUID.randomUUID()}-${System.currentTimeMillis()}"

    operator fun invoke(text: String, from: String = "auto", to: String = "en", key: String = clientKey): String {
        return invoke(listOf(text), from, to, key)[0]
    }
    operator fun invoke(text: List<String>, from: String = "auto", to: String = "en", key: String = clientKey): List<String> {
        val post = HttpPost("https://yi.qq.com/api/imt").apply {
            addHeader("Accept", "application/json, text/plain, */*")
            addHeader("Accept-Encoding", "gzip, deflate, br")
            addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
            addHeader("Cache-Control", "no-cache")
            addHeader("Content-Type", "application/json")
            addHeader("Dnt", "1")
            addHeader("Origin", "https://yi.qq.com")
            addHeader("Pragma", "no-cache")
            addHeader("Referer", "https://yi.qq.com/zh-CN/index")
            addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36 Edg/121.0.0.0")
            addHeader("X-Requested-With", "XMLHttpRequest")

            val json = buildJsonObject {
                put("header", buildJsonObject {
                    put("fn", "auto_translation")
                    put("session", "")
                    put("user", "")
                    put("client_key", key)
                })
                put("type", "plain")
                put("model_category", "normal")
                put("text_domain", "general")
                put("source", buildJsonObject {
                    put("lang", from)
                    put("text_list", buildJsonArray {
                        for (s in text) {
                            add(s)
                        }
                    })
                })
                put("target", buildJsonObject {
                    put("lang", to)
                })
            }
            entity = StringEntity(json.toString())
        }
        return HttpClients.createSystem().use { http ->
            val result = http.execute(post)
            val resultString = result.entity.content.readBytes().toString(Charsets.UTF_8)
            val json = Json.parseToJsonElement(resultString).jsonObject
            json["auto_translation"]!!.jsonArray.map { it.jsonPrimitive.content }
        }
    }
}