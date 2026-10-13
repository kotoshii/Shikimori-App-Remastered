package com.gnoemes.shikimori.di.app.module.network;

import com.gnoemes.shikimori.di.app.annotations.BaseHttpClient;
import com.gnoemes.shikimori.di.app.annotations.GithubApi;
import com.gnoemes.shikimori.entity.app.domain.Constants;
import com.gnoemes.shikimori.utils.network.UserAgentInterceptor;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Used only to find out whether a newer release has been published.
 * <p>
 * Gets its own client on purpose (made from the bare base one): this is a different host, so it
 * must not go through ShikimoriRateLimiter and must not carry a shikimori token. The User-Agent
 * header is required, the GitHub api rejects requests without one.
 */
@Module
public interface GithubNetworkModule {

    @Provides
    @Singleton
    @GithubApi
    static OkHttpClient provideOkHttpClient(@BaseHttpClient OkHttpClient base, UserAgentInterceptor userAgentInterceptor) {
        return base.newBuilder()
                .addInterceptor(userAgentInterceptor)
                .build();
    }

    @Provides
    @Singleton
    @GithubApi
    static Retrofit provideRetrofit(Retrofit.Builder builder, Converter.Factory factory, @GithubApi OkHttpClient client) {
        return builder
                .client(client)
                .addConverterFactory(factory)
                .baseUrl(Constants.GITHUB_API_URL)
                .build();
    }

    @Provides
    @Singleton
    static com.gnoemes.shikimori.data.network.GithubApi bindGithubApi(@GithubApi Retrofit retrofit) {
        return retrofit.create(com.gnoemes.shikimori.data.network.GithubApi.class);
    }
}
