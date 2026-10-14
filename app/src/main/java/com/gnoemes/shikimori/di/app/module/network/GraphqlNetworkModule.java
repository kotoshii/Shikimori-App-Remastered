package com.gnoemes.shikimori.di.app.module.network;

import com.gnoemes.shikimori.BuildConfig;
import com.gnoemes.shikimori.data.repository.common.PosterSource;
import com.gnoemes.shikimori.data.repository.common.GenreVocabularySource;
import com.gnoemes.shikimori.data.repository.common.TitleGenreSource;
import com.gnoemes.shikimori.data.repository.common.impl.PosterSourceImpl;
import com.gnoemes.shikimori.data.repository.common.impl.GenreVocabularySourceImpl;
import com.gnoemes.shikimori.data.repository.common.impl.TitleGenreSourceImpl;
import com.gnoemes.shikimori.di.app.annotations.BaseHttpClient;
import com.gnoemes.shikimori.di.app.annotations.GraphqlApi;
import com.gnoemes.shikimori.utils.network.MissingPosterInterceptor;
import com.gnoemes.shikimori.utils.network.ShikimoriRateLimiter;
import com.gnoemes.shikimori.utils.network.UserAgentInterceptor;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * GraphQL recovers posters the REST api stopped serving (see {@link MissingPosterInterceptor}) and
 * supplies genres v2: each title's genres and the genre list.
 * <p>
 * It gets its own client, made from the bare base one: the poster interceptor is installed on the
 * REST clients, so reusing one of those would make every GraphQL answer re-enter the interceptor.
 * No authentication is needed either - these queries answer fine anonymously.
 */
@Module
public interface GraphqlNetworkModule {

    @Provides
    @Singleton
    @GraphqlApi
    static OkHttpClient provideOkHttpClient(@BaseHttpClient OkHttpClient base,
                                            UserAgentInterceptor userAgentInterceptor,
                                            ShikimoriRateLimiter rateLimiter) {
        return base.newBuilder()
                //the same limiter as the rest clients: one queue for the whole host
                .addInterceptor(rateLimiter)
                .addInterceptor(userAgentInterceptor)
                .build();
    }

    @Provides
    @Singleton
    @GraphqlApi
    static Retrofit provideRetrofit(Retrofit.Builder builder, Converter.Factory factory, @GraphqlApi OkHttpClient client) {
        //the builder brings Rx: the poster lookup takes its answer as a blocking Call, but the
        //genre lookup is composed into a repository chain and needs a Single
        return builder
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

    @Binds
    @Singleton
    TitleGenreSource bindTitleGenreSource(TitleGenreSourceImpl source);

    @Binds
    @Singleton
    GenreVocabularySource bindGenreVocabularySource(GenreVocabularySourceImpl source);
}
