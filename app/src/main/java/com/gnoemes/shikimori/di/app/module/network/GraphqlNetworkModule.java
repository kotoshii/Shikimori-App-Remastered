package com.gnoemes.shikimori.di.app.module.network;

import com.gnoemes.shikimori.BuildConfig;
import com.gnoemes.shikimori.data.repository.common.PosterSource;
import com.gnoemes.shikimori.data.repository.common.impl.PosterSourceImpl;
import com.gnoemes.shikimori.di.app.annotations.GraphqlApi;
import com.gnoemes.shikimori.entity.app.domain.Constants;
import com.gnoemes.shikimori.utils.network.MissingPosterInterceptor;
import com.gnoemes.shikimori.utils.network.ShikimoriRateLimiter;
import com.gnoemes.shikimori.utils.network.UserAgentInterceptor;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * GraphQL is only used to recover posters the REST api stopped serving, see
 * {@link MissingPosterInterceptor}.
 * <p>
 * It gets a dedicated, deliberately bare OkHttp client: the poster interceptor is installed on the
 * REST clients, so reusing one of those would make every GraphQL answer re-enter the interceptor.
 * No authentication is needed either - the poster query answers fine anonymously.
 */
@Module
public interface GraphqlNetworkModule {

    @Provides
    @Singleton
    @GraphqlApi
    static OkHttpClient provideOkHttpClient(UserAgentInterceptor userAgentInterceptor,
                                            ShikimoriRateLimiter rateLimiter) {
        return new OkHttpClient.Builder()
                //the same limiter as the rest clients: one queue for the whole host
                .addInterceptor(rateLimiter)
                .addInterceptor(userAgentInterceptor)
                .connectTimeout(Constants.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton
    @GraphqlApi
    static Retrofit provideRetrofit(Converter.Factory factory, @GraphqlApi OkHttpClient client) {
        return new Retrofit.Builder()
                .client(client)
                .addConverterFactory(factory)
                .baseUrl(BuildConfig.ShikimoriBaseUrl)
                .build();
    }

    @Provides
    @Singleton
    static com.gnoemes.shikimori.data.network.GraphqlApi bindGraphqlApi(@GraphqlApi Retrofit retrofit) {
        return retrofit.create(com.gnoemes.shikimori.data.network.GraphqlApi.class);
    }

    @Provides
    @Singleton
    static MissingPosterInterceptor provideMissingPosterInterceptor(PosterSource posterSource) {
        return new MissingPosterInterceptor(posterSource);
    }

    @Binds
    @Singleton
    PosterSource bindPosterSource(PosterSourceImpl source);
}
