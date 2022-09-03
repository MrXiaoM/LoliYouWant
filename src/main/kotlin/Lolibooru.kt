package top.mrxiaom.loliyouwant

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.mamoe.mirai.utils.MiraiLogger
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

object Lolibooru {
    private val logger = MiraiLogger.Factory.create(this::class, "Lolibooru")
    private val gson = Gson()
    var baseUrl = "https://lolibooru.moe/"

    /**
     * 调用 Lolibooru 接口获取图片
     * @param limit 限制返回数量
     * @param page 页码，从1开始
     * @param tags 搜索标签，请手动进行 URLEncode
     */
    fun get(limit: Int, page: Int = 1, tags: String? = null): List<Loli> {
        val list = mutableListOf<Loli>()
        try {
            val url = url(baseUrl, "post/index.json")
            val input = httpGet(url, mutableMapOf<String, Any>(
                "limit" to limit,
                "page" to page
            ).also { if (tags != null) it["tags"] = tags.trimStart().trimEnd() }) ?: return list
            val jsonString = String(input.readBytes())
            input.close()
            logger.debug("Response json: $jsonString")
            val type = object : TypeToken<ArrayList<JsonLoli>>() {}.type
            val json = gson.fromJson<ArrayList<JsonLoli>>(jsonString, type)
            json.forEach {
                if (it.file_url.startsWith("https://lolibooru.moe/"))
                    it.file_url = baseUrl + it.file_url.substring(22)
                if (it.sample_url.startsWith("https://lolibooru.moe/"))
                    it.sample_url = baseUrl + it.sample_url.substring(22)
                if (it.preview_url.startsWith("https://lolibooru.moe/"))
                    it.preview_url = baseUrl + it.preview_url.substring(22)
                list.add(Loli(it.id, it.file_url, it.sample_url, it.preview_url, it.tags, it.rating))
            }
        } catch (t: Throwable) {
            logger.error("Something was wrong when fetching images: ", t)
        }
        return list
    }

    private fun httpGet(url: String, params: Map<String, Any>, timeout: Int = 60): InputStream? {
        val paramsString = params.map { "${it.key}=${urlEncode(it.value.toString())}" }.joinToString("&")
        val finalUrl = "$url?$paramsString"

        logger.debug("Now connecting to $finalUrl")
        val conn = URL(finalUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = timeout * 1000
        conn.readTimeout = timeout * 1000
        conn.requestMethod = "GET"
        conn.addRequestProperty("User-Agent", "Chrome/104.0.5112.81")
        conn.connect()

        return conn.inputStream
    }
}

class Loli(
    val id: Int,
    val url: String,
    val urlSample: String,
    val urlPreview: String,
    val tags: String,
    val rating: String
)

fun url(site: String, path: String): String = site.removeSuffix("/") + "/" + path.removePrefix("/")

fun urlEncode(s: String): String = URLEncoder.encode(s, "UTF-8")
fun urlDecode(s: String): String = URLDecoder.decode(s, "UTF-8")
