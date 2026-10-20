package com.gnoemes.shikimori.entity.app.domain.exceptions

/** A request that did not get through; [kind] says how, for the text the user sees. */
class NetworkException(val kind: Kind) : BaseException(TAG) {

    enum class Kind { NO_CONNECTION, SECURE_CONNECTION, TIMEOUT }

    override val message: String?
        get() = kind.name

    companion object {
        const val TAG = "NetworkException"
    }
}