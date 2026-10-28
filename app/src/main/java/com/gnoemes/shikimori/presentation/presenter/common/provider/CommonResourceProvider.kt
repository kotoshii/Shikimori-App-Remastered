package com.gnoemes.shikimori.presentation.presenter.common.provider

import com.gnoemes.shikimori.entity.app.domain.exceptions.NetworkException

interface CommonResourceProvider {

    val emptyMessage: String

    val needAuth: String

    val needAuthRates: String

    val topicNotFound: String

    /** What the user reads when a request did not get through. */
    fun networkError(kind: NetworkException.Kind): String

    /** What the user reads when the server answered with an error [code]. */
    fun serverError(code: Int): String
}