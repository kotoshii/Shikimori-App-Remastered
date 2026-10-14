package com.gnoemes.shikimori.di.app.module.network;

import com.gnoemes.shikimori.di.app.annotations.BaseHttpClient;
import com.gnoemes.shikimori.di.app.annotations.ShikicinemaVideoApi;
import com.gnoemes.shikimori.di.app.annotations.VideoApi;
import com.gnoemes.shikimori.entity.app.domain.Constants;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * The video sites (Kodik and the parsed hostings) and Shikicinema: one client for both, with the
 * longer timeout, and the plain Gson converter rather than the app's shikimori one.
 */
@Module
public interface VideoNetworkModule {

    @Provides
    @Singleton
    @VideoApi
    static OkHttpClient provideOkHttpClient(@BaseHttpClient OkHttpClient base, HttpLoggingInterceptor interceptor) {
        return base.newBuilder()
                .addInterceptor(interceptor)
                .connectTimeout(Constants.LONG_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.LONG_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton
    @VideoApi
    static Retrofit provideRetrofit(Retrofit.Builder builder, @VideoApi OkHttpClient client) {
        return builder
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                //Retrofit requires a base url, but every VideoApi call passes a full one. Keep https: a
                //protocol-relative player url ("//host/...") takes its scheme from here.
                .baseUrl("https://localhost/")
                .build();
    }

    @Provides
    @Singleton
    @ShikicinemaVideoApi
    static Retrofit provideShikicinemaRetrofit(Retrofit.Builder builder, @VideoApi OkHttpClient client) {
        return builder
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(Constants.SHIKICINEMA_URL)
                .build();
    }
}
