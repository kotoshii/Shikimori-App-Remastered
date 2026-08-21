package com.gnoemes.shikimori.di.app.module.network;

import com.gnoemes.shikimori.di.app.annotations.GithubApi;
import com.gnoemes.shikimori.entity.app.domain.Constants;
import com.gnoemes.shikimori.utils.network.UserAgentInterceptor;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

/**
 * Used only to find out whether a newer release has been published.
 * <p>
 * Gets its own client on purpose: this is a different host, so it must not go through
 * ShikimoriRateLimiter and must not carry a shikimori token. The User-Agent header is required,
 * the GitHub api rejects requests without one.
 */
@Module
public interface GithubNetworkModule {

    @Provides
    @Singleton
    @GithubApi
    static OkHttpClient provideOkHttpClient(UserAgentInterceptor userAgentInterceptor) {
        return new OkHttpClient.Builder()
                .addInterceptor(userAgentInterceptor)
                .connectTimeout(Constants.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton
    @GithubApi
    static Retrofit provideRetrofit(Converter.Factory factory, @GithubApi OkHttpClient client) {
        return new Retrofit.Builder()
                .client(client)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
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
