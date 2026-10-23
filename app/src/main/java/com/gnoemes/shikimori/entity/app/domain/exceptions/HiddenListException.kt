package com.gnoemes.shikimori.entity.app.domain.exceptions

/**
 * A user's list or history that their «Могут видеть мой список» hides from the viewer: the api's 403.
 * [guest]: the viewer is signed out, and may see it after signing in.
 */
class HiddenListException(val guest: Boolean) : BaseException(TAG) {

    companion object {
        const val TAG = "HiddenListException"
    }
}
