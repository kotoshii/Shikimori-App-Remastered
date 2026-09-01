package com.gnoemes.shikimori.utils

import java.util.Locale

/**
 * Turns whatever a user typed into a bare domain, and decides whether a hosting is filtered out.
 *
 * Deliberately plain string work rather than a url parser, because none of them fit: `java.net.URL`
 * throws on `youtube.com` for want of a protocol, `java.net.URI` accepts it but reports a null host
 * because it parses as a path, and `android.net.Uri.parse` never rejects anything. Doing it by hand
 * also keeps this testable on the jvm, which matters - it is the only part of the hosting filter
 * that can be checked without a device.
 */
object HostingFilter {

    /** Labels of letters and digits, hyphens allowed inside only, and at least one dot. */
    private val DOMAIN = Regex("[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+")

    /**
     * Accepts a bare domain or a full link and returns the host, or null when there is no sensible
     * domain in it. `https://kodikplayer.com/seria/1/a/720p` and `www.KodikPlayer.com` both give
     * `kodikplayer.com`.
     */
    fun normalize(raw: String?): String? {
        if (raw == null) return null

        var value = raw.trim().toLowerCase(Locale.ROOT)
        if (value.isEmpty()) return null

        value = value.substringAfter("://")      //scheme
        value = value.substringAfterLast('@')    //user:password@
        value = value.substringBefore('/')       //path
        value = value.substringBefore('?')       //query
        value = value.substringBefore('#')       //fragment
        value = value.substringBefore(':')       //port
        value = value.removePrefix("www.")

        return if (DOMAIN.matches(value)) value else null
    }

    /**
     * True when [hosting] is hidden, matching subdomains as well - hiding `alloeclub.com` also hides
     * `arven.as.alloeclub.com`, which is what makes this a domain filter rather than a name filter.
     */
    fun isHidden(hosting: String?, hidden: Set<String>): Boolean {
        if (hosting.isNullOrBlank() || hidden.isEmpty()) return false

        val value = hosting.toLowerCase(Locale.ROOT)

        return hidden.any { entry -> value == entry || value.endsWith(".$entry") }
    }
}
