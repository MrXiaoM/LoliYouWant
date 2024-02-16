package top.mrxiaom.loliyouwant.api

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object BingTranslate {
    operator fun invoke(text: String, from: String = "", to: String = "en"): String {
        val url = "https://api.microsofttranslator.com/v2/Http.svc/Translate?appId=AFC76A66CF4F434ED080D245C30CF1E71C22959C&from=$from&to=$to&text=${URLEncoder.encode(text)}"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 30 * 1000
        conn.readTimeout = 30 * 1000
        conn.requestMethod = "GET"
        conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36 Edg/114.0.1823.79")
        conn.connect()
        val result = conn.inputStream.readBytes().toString(Charsets.UTF_8).substringAfter(">")
        return result.substringBeforeLast("</")
    }
}