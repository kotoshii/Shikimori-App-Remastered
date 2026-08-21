package com.gnoemes.shikimori.utils.network

import com.gnoemes.shikimori.data.repository.common.PosterSource
import com.gnoemes.shikimori.entity.common.data.graphql.PosterEntityType
import com.gnoemes.shikimori.entity.common.data.graphql.PosterKey
import com.gnoemes.shikimori.entity.common.data.graphql.PosterResponse
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody

/**
 * Puts posters back into REST answers that Shikimori serves without one.
 *
 * Content added after the 2024 poster migration comes back as
 * `"image": {"original": "/assets/globals/missing_original.jpg", ...}` - the files are no longer
 * named after the content id, and only GraphQL publishes their real names.
 *
 * Patching here, on the transport level, instead of in every repository/converter is deliberate:
 * broken images are not limited to the five detail screens, they also show up nested inside other
 * answers (roles, similar, related, franchise nodes, a character's animes, a person's works,
 * a topic's linked content, favourites...). Rewriting the raw json keeps every entity, converter
 * and screen working against exactly the same contract as before - only the url values change.
 *
 * The interceptor is deliberately forgiving: any failure leaves the original answer untouched.
 */
class MissingPosterInterceptor(
        private val posterSource: PosterSource
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            //the app itself is being throttled, so stop spending quota on posters
            if (response.code() == HTTP_TOO_MANY_REQUESTS) posterSource.onRateLimited()
            return response
        }
        val body = response.body() ?: return response

        val mediaType = body.contentType()
        if (mediaType?.subtype()?.contains(JSON, true) != true) return response

        val original = try {
            body.string()
        } catch (e: Throwable) {
            return response
        }

        //the overwhelming majority of answers is untouched, so keep the check as cheap as possible
        val patched =
                if (!original.contains(MISSING_MARKER)) original
                else try {
                    patch(original, request.url().encodedPath())
                } catch (e: Throwable) {
                    original
                }

        return response.newBuilder()
                .body(ResponseBody.create(mediaType, patched))
                .build()
    }

    private fun patch(json: String, path: String): String {
        val root = JsonParser().parse(json)
        val targets = ArrayList<Target>()
        collect(root, null, typeFromPath(path), targets)
        if (targets.isEmpty()) return json

        val posters = posterSource.resolve(targets.map { PosterKey(it.type, it.id) }.toSet())
        if (posters.isEmpty()) return json

        var changed = false
        targets.forEach { target ->
            val poster = posters[PosterKey(target.type, target.id)]
            if (poster != null && applyPoster(target.owner, poster)) changed = true
        }

        return if (changed) root.toString() else json
    }

    ////////////////////////////////////////////////////////////////////////
    // Collecting
    ////////////////////////////////////////////////////////////////////////

    /**
     * Walks the whole tree looking for objects that carry a placeholder image.
     *
     * [key] is the json key the element was reached through - array items keep the key of the
     * array itself, which is what makes favourites (`{"animes": [...], "seyu": [...]}`) resolvable
     * even though their items carry neither `url` nor `kind`.
     *
     * [inherited] is only meaningful for the root element and comes from the request path.
     */
    private fun collect(
            element: JsonElement,
            key: String?,
            inherited: PosterEntityType?,
            out: MutableList<Target>
    ) {
        when {
            element.isJsonArray -> element.asJsonArray.forEach { collect(it, key, inherited, out) }
            element.isJsonObject -> {
                val obj = element.asJsonObject
                val type = detectType(obj, key) ?: inherited
                val id = obj.asLongOrNull(ID)

                if (type != null && id != null && hasMissingPoster(obj)) out.add(Target(obj, type, id))

                obj.entrySet().forEach { (childKey, child) -> collect(child, childKey, null, out) }
            }
        }
    }

    private fun detectType(obj: JsonObject, key: String?): PosterEntityType? =
            typeFromUrl(obj.asStringOrNull(URL))
                    ?: typeFromKind(obj.asStringOrNull(KIND))
                    ?: typeFromKey(key)

    private fun typeFromUrl(url: String?): PosterEntityType? {
        if (url == null) return null
        val match = URL_PATTERN.find(url) ?: return null
        return typeFromSegment(match.groupValues[1])
    }

    private fun typeFromPath(path: String?): PosterEntityType? {
        if (path == null) return null
        val match = PATH_PATTERN.find(path) ?: return null
        return typeFromSegment(match.groupValues[1])
    }

    private fun typeFromSegment(segment: String): PosterEntityType? = when (segment) {
        "animes" -> PosterEntityType.ANIME
        //ranobe are mangas, both url prefixes are in use
        "mangas", "ranobe" -> PosterEntityType.MANGA
        "characters" -> PosterEntityType.CHARACTER
        "people" -> PosterEntityType.PERSON
        else -> null
    }

    private fun typeFromKind(kind: String?): PosterEntityType? = when {
        kind == null -> null
        ANIME_KINDS.contains(kind) -> PosterEntityType.ANIME
        MANGA_KINDS.contains(kind) -> PosterEntityType.MANGA
        else -> null
    }

    private fun typeFromKey(key: String?): PosterEntityType? = when (key) {
        "anime", "animes", "similar" -> PosterEntityType.ANIME
        "manga", "mangas", "ranobe" -> PosterEntityType.MANGA
        "character", "characters" -> PosterEntityType.CHARACTER
        "person", "people", "seyu", "mangakas", "producers" -> PosterEntityType.PERSON
        else -> null
    }

    ////////////////////////////////////////////////////////////////////////
    // Patching
    ////////////////////////////////////////////////////////////////////////

    private fun hasMissingPoster(obj: JsonObject): Boolean {
        val image = obj.get(IMAGE)
        val missingImage =
                if (image != null && image.isJsonObject) image.asJsonObject.entrySet().any { it.value.isPlaceholder() }
                else image.isPlaceholder()

        return missingImage || obj.get(IMAGE_URL).isPlaceholder()
    }

    private fun applyPoster(obj: JsonObject, poster: PosterResponse): Boolean {
        var changed = false

        val image = obj.get(IMAGE)
        when {
            //anime, manga, ranobe, character, person: {"original": ..., "preview": ..., "x96": ..., "x48": ...}
            image != null && image.isJsonObject -> {
                val imageObj = image.asJsonObject
                //entrySet is a live view, so snapshot the keys before writing into it
                val sizes = imageObj.entrySet().map { it.key }
                sizes.forEach { size ->
                    val current = imageObj.get(size)
                    if (!current.isPlaceholder()) return@forEach
                    val url = poster.urlFor(sizeOf(current.asString) ?: size) ?: return@forEach
                    imageObj.add(size, JsonPrimitive(url))
                    changed = true
                }
            }
            //favourites: "image": "/system/animes/x64/10357.jpg"
            image.isPlaceholder() -> {
                val url = poster.urlFor(sizeOf(image!!.asString))
                if (url != null) {
                    obj.add(IMAGE, JsonPrimitive(url))
                    changed = true
                }
            }
        }

        //franchise nodes: "image_url": "https://shikimori.io/assets/globals/missing_x96.jpg"
        val imageUrl = obj.get(IMAGE_URL)
        if (imageUrl.isPlaceholder()) {
            val url = poster.urlFor(sizeOf(imageUrl!!.asString))
            if (url != null) {
                obj.add(IMAGE_URL, JsonPrimitive(url))
                changed = true
            }
        }

        return changed
    }

    /** `.../missing_x96.jpg` -> `x96`, so the replacement keeps the size the caller expects. */
    private fun sizeOf(placeholder: String): String? =
            SIZE_PATTERN.find(placeholder)?.groupValues?.get(1)

    ////////////////////////////////////////////////////////////////////////

    private fun JsonElement?.isPlaceholder(): Boolean =
            this != null && this.isJsonPrimitive && this.asJsonPrimitive.isString &&
                    this.asString.contains(MISSING_MARKER)

    private fun JsonObject.asStringOrNull(key: String): String? {
        val element = get(key) ?: return null
        return if (element.isJsonPrimitive && element.asJsonPrimitive.isString) element.asString else null
    }

    private fun JsonObject.asLongOrNull(key: String): Long? {
        val element = get(key) ?: return null
        if (!element.isJsonPrimitive) return null
        return try {
            element.asLong
        } catch (e: Exception) {
            null
        }
    }

    private class Target(
            val owner: JsonObject,
            val type: PosterEntityType,
            val id: Long
    )

    companion object {
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val JSON = "json"
        private const val ID = "id"
        private const val URL = "url"
        private const val KIND = "kind"
        private const val IMAGE = "image"
        private const val IMAGE_URL = "image_url"

        private const val MISSING_MARKER = "assets/globals/missing_"

        private val SIZE_PATTERN = Regex("missing_([a-z0-9_]+)\\.")
        private val URL_PATTERN = Regex("/(animes|mangas|ranobe|characters|people)/")
        private val PATH_PATTERN = Regex("^/api/(?:v\\d+/)?(animes|mangas|ranobe|characters|people)")

        private val ANIME_KINDS = setOf(
                "tv", "movie", "ova", "ona", "special", "music",
                "tv_13", "tv_24", "tv_48", "tv_special", "pv", "cm"
        )
        private val MANGA_KINDS = setOf(
                "manga", "manhwa", "manhua", "one_shot", "doujin", "novel", "light_novel"
        )
    }
}
