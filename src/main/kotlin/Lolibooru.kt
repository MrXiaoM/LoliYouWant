package top.mrxiaom.loliyouwant

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.jsonArray
import net.mamoe.mirai.utils.MiraiLogger
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

object Lolibooru {
    private val logger = MiraiLogger.Factory.create(this::class, "Lolibooru")
    private const val defaultApi = "https://lolibooru.moe/"
    private val json = Json {
        ignoreUnknownKeys = true
    }
    var baseUrl = "https://lolibooru.moe/"

    /**
     * 调用 Lolibooru 接口获取图片
     * @param api 接口地址
     * @param limit 限制返回数量
     * @param page 页码，从1开始
     * @param tags 搜索标签，请手动进行 URLEncode
     */
    fun get(api: String, limit: Int, page: Int = 1, tags: String? = null): List<Loli> {
        val apiUrl = api.removeSuffix("/") + "/"
        return runCatching {
            val jsonString = httpGet(apiUrl + "post/index.json",
                mutableMapOf<String, Any>(
                    "limit" to limit,
                    "page" to page
                ).also { if (tags != null) it["tags"] = tags.trimStart().trimEnd() }
            )!!.use {
                it.readBytes().toString(Charsets.UTF_8)
            }

            logger.verbose("Response json: $jsonString")
            val array = json.parseToJsonElement(jsonString).jsonArray
            array.map {
                val loli = json.decodeFromJsonElement(JsonLoli.serializer(), it)
                Loli(
                    loli.id,
                    loli.fileUrl.replace(defaultApi, apiUrl),
                    loli.sampleUrl.replace(defaultApi, apiUrl),
                    loli.previewUrl.replace(defaultApi, apiUrl),
                    loli.tags,
                    loli.rating
                )
            }
        }.getOrElse {
            logger.error("Something was wrong when fetching images: ", it)
            if (apiUrl != defaultApi) {
                get(defaultApi, limit, page, tags)
            } else {
                emptyList()
            }
        }
    }

    /**
     * @see get
     */
    fun get(limit: Int, page: Int = 1, tags: String? = null): List<Loli> {
        return get(baseUrl, limit, page, tags)
    }

    private fun httpGet(url: String, params: Map<String, Any>, timeout: Int = 60): InputStream? {
        val paramsString = params.map { "${it.key}=${urlEncode(it.value.toString())}" }.joinToString("&")
        val finalUrl = "$url?$paramsString"

        logger.debug("Now connecting to $finalUrl")
        val conn = URL(finalUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = timeout * 1000
        conn.readTimeout = timeout * 1000
        conn.requestMethod = "GET"
        conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36 Edg/114.0.1823.79")
        conn.connect()

        return conn.inputStream
    }
}

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class JsonLoli(
    var id: Int,
    @JsonNames("file_url")
    var fileUrl: String,
    @JsonNames("preview_url")
    var previewUrl: String,
    @JsonNames("sample_url")
    var sampleUrl: String,
    var rating: String,
    var tags: String
)
class Loli(
    val id: Int,
    val url: String,
    val urlSample: String,
    val urlPreview: String,
    val tags: String,
    val rating: String
)

/**
 * @see java.net.URLStreamHandler.toExternalForm
 */
fun browserLikeUrlEncode(url: String): String {
    val u = URL(url)
    val authority = if (u.authority != null && u.authority.isNotEmpty()) "//${u.authority}" else ""
    val path = (u.path ?: "").split("/").joinToString("/") { urlEncode(it) }
    val query = if (u.query != null) ("?" + u.query.split("&").map {
        if (!it.contains("=")) it
        else (it.substringBefore("=") + "=" + urlEncode(it.substringAfter("=")))
    }) else ""
    val ref = if (u.ref != null) "#${urlEncode(u.ref)}" else ""
    return u.protocol + ':' + authority + path + query + ref
}

fun urlEncode(s: String): String = URLEncoder.encode(s, "UTF-8")
fun urlDecode(s: String): String = URLDecoder.decode(s, "UTF-8")
