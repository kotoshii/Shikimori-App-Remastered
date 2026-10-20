package com.gnoemes.shikimori.utils.rx

import com.gnoemes.shikimori.entity.app.domain.exceptions.*
import com.gnoemes.shikimori.utils.network.SignedOutException
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.MalformedJsonException
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.HttpException
import java.io.EOFException
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.*
import javax.net.ssl.SSLException

class ErrorProcessing<T> {

    fun getObservableErrors(throwable: Throwable): Observable<T> {
        return Observable.error { throwProcessException(throwable) }
    }

    fun getSingleErrors(throwable: Throwable): Single<T> {
        return Single.error { throwProcessException(throwable) }
    }

    fun getCompletableErrors(throwable: Throwable): Completable {
        return Completable.error { throwProcessException(throwable) }
    }

    @Throws(TitleException::class, NetworkException::class, ServiceCodeException::class, ContentException::class)
    internal fun throwProcessException(throwable: Throwable): Throwable {
        //before the other connection errors: it is an IOException too
        if (throwable is SignedOutException) {
            throw NetworkException(NetworkException.Kind.SIGNED_OUT)
        }

        if (throwable is SocketTimeoutException) {
            throw NetworkException(NetworkException.Kind.TIMEOUT)
        }

        if (throwable is SSLException) {
            throw NetworkException(NetworkException.Kind.SECURE_CONNECTION)
        }

        //an answer that came but could not be read: Gson's own errors, an empty body included
        if (throwable is JsonSyntaxException || throwable is MalformedJsonException || throwable is EOFException) {
            throw TitleException("", "Json syntax error")
        }

        //no connection, refused, reset, cut off - anything else that kept the answer from arriving.
        //These used to be only logged, and the screen just never loaded (BUGS #65)
        if (throwable is IOException) {
            throw NetworkException(NetworkException.Kind.NO_CONNECTION)
        }

        if (throwable is HttpException) {
            throw ServiceCodeException(throwable.code())
        }

        if (throwable is NoSuchElementException) {
            throw ContentException("")
        }

        return throwable as? BaseException ?: TitleException("", throwable.message)
    }
}