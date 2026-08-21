package com.gnoemes.shikimori.utils

/**
 * Compares dotted version strings without caring how many parts each side has, so `0.8.8` and
 * `0.8.8.0` count as the same version. That keeps the update check working across the switch from
 * four-part to three-part version names.
 *
 * Anything that is not a digit or a dot is ignored, which covers the `v` in a git tag and the
 * `-SNAPSHOT` suffix debug builds carry.
 *
 * Returns a positive number if [version] is newer than [other], 0 if they match.
 */
fun compareVersions(version: String?, other: String?): Int {
    val left = version.toVersionParts()
    val right = other.toVersionParts()

    for (i in 0 until maxOf(left.size, right.size)) {
        val diff = left.getOrElse(i) { 0 } - right.getOrElse(i) { 0 }
        if (diff != 0) return diff
    }

    return 0
}

private fun String?.toVersionParts(): List<Int> {
    if (this == null) return emptyList()
    return replace(Regex("[^0-9.]"), "")
            .split('.')
            .map { it.toIntOrNull() ?: 0 }
}
