package com.gnoemes.shikimori.di.app.module.network;

import com.gnoemes.shikimori.BuildConfig;
import com.gnoemes.shikimori.di.app.annotations.BaseHttpClient;
import com.gnoemes.shikimori.utils.network.MissingPosterInterceptor;
import com.gnoemes.shikimori.utils.network.ShikimoriRateLimiter;
import com.gnoemes.shikimori.utils.network.UserAgentInterceptor;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Converter;
import retrofit2.Retrofit;

@Module
public interface CommonNetworkModule {

    @Provides
    @Singleton
    static OkHttpClient provideOkHttpClient(@BaseHttpClient OkHttpClient base,
                                            HttpLoggingInterceptor interceptor,
                                            UserAgentInterceptor userAgentInterceptor,
                                            MissingPosterInterceptor missingPosterInterceptor,
                                            ShikimoriRateLimiter rateLimiter) {
        return base.newBuilder()
                //application interceptor on purpose: it needs the final, decoded body, once per call
                .addInterceptor(missingPosterInterceptor)
                //innermost of the two, so the poster lookup it triggers is queued separately
                .addInterceptor(rateLimiter)
                //application interceptors, as on the client with the token: as network ones the name came
                //after OkHttp's own "okhttp/…" and both were sent, and a request that failed before a
                //connection was never logged (BUGS #79)
                .addInterceptor(userAgentInterceptor)
                .addInterceptor(interceptor)
                .build();
    }

    @Provides
    @Singleton
    static Retrofit provideRetrofit(Retrofit.Builder builder, Converter.Factory factory, OkHttpClient client) {
        return builder
                .client(client)
                .addConverterFactory(factory)
                .baseUrl(BuildConfig.ShikimoriBaseUrl)
                .build();
    }

}
