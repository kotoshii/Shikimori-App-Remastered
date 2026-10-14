package com.gnoemes.shikimori.di.app.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

/**
 * The OkHttp client every other one is made from with {@code newBuilder()}: one connection pool and
 * one dispatcher for the whole app. It has no interceptors of its own.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface BaseHttpClient {
}
