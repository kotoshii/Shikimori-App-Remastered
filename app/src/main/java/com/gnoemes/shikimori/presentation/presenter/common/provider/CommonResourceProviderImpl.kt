package com.gnoemes.shikimori.presentation.presenter.common.provider

import android.content.Context
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.app.domain.exceptions.NetworkException
import javax.inject.Inject

class CommonResourceProviderImpl @Inject constructor(
        private val context: Context
) : CommonResourceProvider {

    override val emptyMessage: String
        get() = context.getString(R.string.common_empty)

    override val needAuth: String
        get() = context.getString(R.string.common_need_auth)

    override val needAuthRates: String
        get() = context.getString(R.string.rates_need_auth)

    override val topicNotFound: String
        get() = context.getString(R.string.common_topic_not_found)

    override fun networkError(kind: NetworkException.Kind): String = context.getString(when (kind) {
        NetworkException.Kind.NO_CONNECTION -> R.string.common_error_no_connection
        NetworkException.Kind.SECURE_CONNECTION -> R.string.common_error_secure_connection
        NetworkException.Kind.TIMEOUT -> R.string.common_error_timeout
        NetworkException.Kind.SIGNED_OUT -> R.string.common_error_signed_out
    })

    override fun serverError(code: Int): String = context.getString(R.string.common_error_server, code)
}