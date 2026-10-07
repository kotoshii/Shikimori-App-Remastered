package com.gnoemes.shikimori.entity.app.domain.exceptions

/**
 * A hosting answered the player page request with an anti-bot check instead of the page.
 *
 * There is nothing to parse out of such a page, and the check cannot be solved here - vk.com's is
 * an md5 proof of work over a salt built by obfuscated javascript that is regenerated per request.
 * The app can only say what happened and let the user pass the check in a browser.
 *
 * @see com.gnoemes.shikimori.data.repository.series.shikimori.parser.VkParser
 */
class HostingChallengeException : BaseException(TAG) {

    companion object {
        const val TAG = "HostingChallengeException"
    }
}
