package com.gnoemes.shikimori.di.app.module.network;

import com.gnoemes.shikimori.BuildConfig;
import com.gnoemes.shikimori.di.app.annotations.AuthCommonApi;
import com.gnoemes.shikimori.di.app.annotations.BaseHttpClient;
import com.gnoemes.shikimori.utils.network.MissingPosterInterceptor;
import com.gnoemes.shikimori.utils.network.ShikiAuthenticator;
import com.gnoemes.shikimori.utils.network.ShikimoriRateLimiter;
import com.gnoemes.shikimori.utils.network.TokenInterceptor;
import com.gnoemes.shikimori.utils.network.UserAgentInterceptor;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Converter;
import retrofit2.Retrofit;

@Module
public interface AuthCommonNetworkModule {

    @Provides
    @Singleton
    @AuthCommonApi
    static OkHttpClient provideOkHttpClient(@BaseHttpClient OkHttpClient base,
                                            HttpLoggingInterceptor interceptor,
                                            UserAgentInterceptor userAgentInterceptor,
                                            ShikiAuthenticator authenticator,
                                            TokenInterceptor tokenInterceptor,
                                            MissingPosterInterceptor missingPosterInterceptor,
                                            ShikimoriRateLimiter rateLimiter) {
        return base.newBuilder()
                .authenticator(authenticator)
                //outermost, so it only ever sees the final body of an already re-authenticated call
                .addInterceptor(missingPosterInterceptor)
                .addInterceptor(rateLimiter)
                .addInterceptor(tokenInterceptor)
                .addInterceptor(userAgentInterceptor)
                .addInterceptor(interceptor)
                .build();
    }

    @Provides
    @Singleton
    @AuthCommonApi
    static Retrofit provideRetrofit(Retrofit.Builder builder, Converter.Factory factory, @AuthCommonApi OkHttpClient client) {
        return builder
                .client(client)
                .addConverterFactory(factory)
                .baseUrl(BuildConfig.ShikimoriBaseUrl)
                .build();
    }
}
