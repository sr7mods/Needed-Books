package com.needed.books

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable

/**
 * Data Model for Books and Multi-Level Folders in Kotlin.
 */
class BookModel : Serializable {
    companion object {
        private const val serialVersionUID = 1L

        @JvmStatic
        fun parseFirebaseJson(jsonStr: String?): List<BookModel> {
            val list = ArrayList<BookModel>()
            if (jsonStr == null || jsonStr.trim().isEmpty()) {
                return list
            }

            try {
                val trimmed = jsonStr.trim()
                if (trimmed.startsWith("{")) {
                    val rootObj = JSONObject(trimmed)
                    val keys = rootObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val `val` = rootObj.get(key)
                        if (`val` is JSONObject) {
                            val model = fromJsonObject(key, `val`, "root", "/")
                            list.add(model)
                        }
                    }
                } else if (trimmed.startsWith("[")) {
                    val rootArray = JSONArray(trimmed)
                    for (i in 0 until rootArray.length()) {
                        val obj = rootArray.optJSONObject(i)
                        if (obj != null) {
                            val model = fromJsonObject("item_$i", obj, "root", "/")
                            list.add(model)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return list
        }

        private fun fromJsonObject(id: String, obj: JSONObject, parentId: String, currentPath: String): BookModel {
            val model = BookModel()
            model.id = obj.optString("id", id)
            model.name = obj.optString("name", obj.optString("title", "Untitled"))
            model.author = obj.optString("author", obj.optString("authorName", obj.optString("author_name", "")))
            model.size = obj.optString("size", "")
            model.downloadUrl = obj.optString("url", obj.optString("downloadUrl", ""))
            model.parentId = obj.optString("parentId", parentId)

            val isDir = "folder".equals(obj.optString("type", ""), ignoreCase = true)
                    || obj.optBoolean("isFolder", false)
                    || obj.has("items")
                    || obj.has("children")
                    || obj.has("books")
            model.isFolder = isDir

            val itemPath = "$currentPath${model.name}/"
            model.path = itemPath

            // Check for nested children ("items", "children", or "books")
            var childrenObj: Any? = null
            if (obj.has("items")) {
                childrenObj = obj.opt("items")
            } else if (obj.has("children")) {
                childrenObj = obj.opt("children")
            } else if (obj.has("books")) {
                childrenObj = obj.opt("books")
            }

            if (childrenObj != null) {
                if (childrenObj is JSONObject) {
                    val keys = childrenObj.keys()
                    while (keys.hasNext()) {
                        val childKey = keys.next()
                        val childItem = childrenObj.optJSONObject(childKey)
                        if (childItem != null) {
                            model.addChild(fromJsonObject(childKey, childItem, model.id, itemPath))
                        }
                    }
                } else if (childrenObj is JSONArray) {
                    for (i in 0 until childrenObj.length()) {
                        val childItem = childrenObj.optJSONObject(i)
                        if (childItem != null) {
                            model.addChild(fromJsonObject("${model.id}_$i", childItem, model.id, itemPath))
                        }
                    }
                }
            }

            return model
        }
    }

    var id: String = ""
    var name: String = ""
    var author: String = ""
    var size: String = ""
        get() {
            if (isFolder) {
                val count = getEffectiveItemCount()
                return if (count == 1) "1 item" else "$count items"
            }
            return if (field.isNotEmpty()) field else "PDF Book"
        }
        set(value) {
            field = value
        }
    var downloadUrl: String = ""
    var isFolder: Boolean = false
    var parentId: String = "root"
    var itemCount: Int = 0
    var path: String = "/"
    var children: MutableList<BookModel> = ArrayList()

    constructor() {
        this.parentId = "root"
        this.path = "/"
        this.children = ArrayList()
    }

    constructor(
        id: String,
        name: String,
        author: String?,
        size: String?,
        downloadUrl: String?,
        isFolder: Boolean,
        parentId: String?
    ) {
        this.id = id
        this.name = name
        this.author = author ?: ""
        this.size = size ?: ""
        this.downloadUrl = downloadUrl ?: ""
        this.isFolder = isFolder
        this.parentId = parentId ?: "root"
        this.children = ArrayList()
        this.path = "/"
    }

    fun getEffectiveItemCount(): Int {
        if (children.isNotEmpty()) {
            return children.size
        }
        return itemCount
    }

    fun addChild(child: BookModel) {
        child.parentId = this.id
        this.children.add(child)
        this.itemCount = this.children.size
    }
}
