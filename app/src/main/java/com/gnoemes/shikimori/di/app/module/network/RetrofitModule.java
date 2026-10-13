package com.gnoemes.shikimori.di.app.module.network;

import com.gnoemes.shikimori.di.app.annotations.BaseHttpClient;
import com.gnoemes.shikimori.entity.app.domain.Constants;
import com.gnoemes.shikimori.entity.topic.data.TopicResponse;
import com.gnoemes.shikimori.entity.user.data.UserHistoryResponse;
import com.gnoemes.shikimori.utils.network.DateTimeResponseConverter;
import com.gnoemes.shikimori.utils.network.DateTimeResponseConverterImpl;
import com.gnoemes.shikimori.utils.network.TopicResponseSerializator;
import com.gnoemes.shikimori.utils.network.UserAgentInterceptor;
import com.gnoemes.shikimori.utils.network.UserHistoryResponseSerializator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.joda.time.DateTime;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
public interface RetrofitModule {

    /**
     * The one client the others are made from with {@code newBuilder()}, so they share one
     * connection pool and one dispatcher. No interceptors here: each variant adds its own, in its
     * own order, and the order matters - see the comments at each.
     */
    @Provides
    @Singleton
    @BaseHttpClient
    static OkHttpClient provideBaseOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Constants.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }

    /** A new builder for each Retrofit, with Rx calls; each adds its own client, converter and url. */
    @Provides
    static Retrofit.Builder provideRetrofitBuilder() {
        return new Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create());
    }

    @Provides
    @Singleton
    static HttpLoggingInterceptor provideHttpLoggingInterceptor() {
        return new HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BODY);
    }

    @Provides
    @Singleton
    static UserAgentInterceptor provideUserAgentIntercepter() {
        return new UserAgentInterceptor();
    }

    @Provides
    @Singleton
    static Converter.Factory provideFactory(Gson gson) {
        return GsonConverterFactory.create(gson);
    }

    @Provides
    @Singleton
    static Gson provideGson(DateTimeResponseConverter dateTimeResponseConverter,
                            TopicResponseSerializator topicResponseSerializator,
                            UserHistoryResponseSerializator historyResponseSerializator
    ) {
        return new GsonBuilder()
                .registerTypeAdapter(DateTime.class, dateTimeResponseConverter)
                .registerTypeAdapter(TopicResponse.class, topicResponseSerializator)
                .registerTypeAdapter(UserHistoryResponse.class, historyResponseSerializator)
                .create();
    }

    @Binds
    DateTimeResponseConverter bindDateTimeConverter(DateTimeResponseConverterImpl converter);

    @Provides
    static TopicResponseSerializator bindTopicResponseSerializator() {
        return new TopicResponseSerializator();
    }

    @Provides
    static UserHistoryResponseSerializator bindUserHistoryResponseSerializator() {
        return new UserHistoryResponseSerializator();
    }
}
