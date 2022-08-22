package top.mrxiaom.loliyouwant

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.mamoe.mirai.utils.MiraiLogger
import java.net.URL

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
            val url = "${baseUrl}post/index.json?limit=$limit&page$page" + (if (tags != null) "&tags=$tags" else "")
            logger.debug("Now connecting to $url")
            val conn = URL(url).openConnection()
            conn.connectTimeout = 30 * 1000
            conn.readTimeout = 30 * 1000
            conn.addRequestProperty("User-Agent", "Chrome/104.0.5112.81")
            conn.connect()
            val jsonString = String(conn.getInputStream().readBytes())
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

    /**
     * 调用 Lolibooru 接口随机获取图片
     * @see get
     */
    fun random(limit: Int, page: Int = 1) : List<Loli> {
        return get(limit, page,"order%3Arandom")
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