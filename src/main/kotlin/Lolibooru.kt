package top.mrxiaom.loliyouwant

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.URL

object Lolibooru {
    private val gson = Gson()
    var baseUrl = "https://lolibooru.moe/"

    fun get(limit: Int, page: Int = 1, tags: String? = null): List<Loli> {
        val list = mutableListOf<Loli>()
        val conn = URL("${baseUrl}post/index.json?limit=$limit&page$page" + (if (tags != null) "&tags=$tags" else "")).openConnection()
        conn.addRequestProperty("User-Agent", "Chrome/104.0.5112.81")
        conn.connect()
        val jsonString = String(conn.getInputStream().readBytes())
        println(jsonString)
        val type = object:TypeToken<ArrayList<JsonLoli>>(){}.type
        val json = gson.fromJson<ArrayList<JsonLoli>>(jsonString, type)
        json.forEach {
            if (it.file_url.startsWith("https://lolibooru.moe/"))
                it.file_url = baseUrl + it.file_url.substring(22)
            if (it.sample_url.startsWith("https://lolibooru.moe/"))
                it.sample_url = baseUrl + it.sample_url.substring(22)
            if (it.preview_url.startsWith("https://lolibooru.moe/"))
                it.preview_url = baseUrl + it.preview_url.substring(22)
            list.add(Loli(it.file_url, it.sample_url, it.preview_url, it.tags))
        }
        return list
    }

    fun random(limit: Int, page: Int = 1) : List<Loli> {
        return get(limit, page,"order%3Arandom")
    }
}
class Loli(
    val url: String,
    val urlSample: String,
    val urlPreview: String,
    val tags: String
)