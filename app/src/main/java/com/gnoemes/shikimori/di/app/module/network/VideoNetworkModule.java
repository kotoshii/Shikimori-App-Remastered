package com.gnoemes.shikimori.di.app.module.network;

import com.gnoemes.shikimori.BuildConfig;
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
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
public interface VideoNetworkModule {

    @Provides
    @Singleton
    @VideoApi
    static OkHttpClient provideOkHttpClient(HttpLoggingInterceptor interceptor) {
        return new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .connectTimeout(Constants.LONG_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.LONG_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton
    @VideoApi
    static Retrofit.Builder provideRetrofitBuilder(@VideoApi OkHttpClient client) {
        return new Retrofit.Builder()
                .client(client)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create());
    }

    @Provides
    @Singleton
    @VideoApi
    static Retrofit provideRetrofit(@VideoApi Retrofit.Builder builder) {
        return builder.baseUrl(BuildConfig.VideoBaseUrl).build();
    }

    @Provides
    @Singleton
    @ShikicinemaVideoApi
    static OkHttpClient provideShikicinemaOkHttpClient(HttpLoggingInterceptor interceptor) {
        return new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .connectTimeout(Constants.LONG_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.LONG_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton
    @ShikicinemaVideoApi
    static Retrofit.Builder provideShikicinemaRetrofitBuilder(@ShikicinemaVideoApi OkHttpClient client) {
        return new Retrofit.Builder()
                .client(client)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create());
    }

    @Provides
    @Singleton
    @ShikicinemaVideoApi
    static Retrofit provideShikicinemaRetrofit(@ShikicinemaVideoApi Retrofit.Builder builder) {
        return builder.baseUrl(Constants.SHIKICINEMA_URL).build();
    }
}
