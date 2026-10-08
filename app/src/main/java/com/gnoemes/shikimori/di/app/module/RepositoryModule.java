package com.gnoemes.shikimori.di.app.module;

import com.gnoemes.shikimori.data.local.services.DownloadSource;
import com.gnoemes.shikimori.data.local.services.impl.DownloadSourceImpl;
import com.gnoemes.shikimori.data.repository.app.AuthorizationRepository;
import com.gnoemes.shikimori.data.repository.app.TaskRepository;
import com.gnoemes.shikimori.data.repository.app.TokenRepository;
import com.gnoemes.shikimori.data.repository.app.TokenSource;
import com.gnoemes.shikimori.data.repository.app.impl.AuthorizationRepositoryImpl;
import com.gnoemes.shikimori.data.repository.app.impl.TaskRepostioryImpl;
import com.gnoemes.shikimori.data.repository.app.impl.TokenRepositoryImpl;
import com.gnoemes.shikimori.data.repository.app.impl.TokenSourceImpl;
import com.gnoemes.shikimori.data.repository.download.DownloadRepository;
import com.gnoemes.shikimori.data.repository.download.DownloadRepositoryImpl;
import com.gnoemes.shikimori.data.repository.rates.RatesRepository;
import com.gnoemes.shikimori.data.repository.rates.RatesRepositoryImpl;
import com.gnoemes.shikimori.data.repository.series.smotretanime.Anime365TokenSource;
import com.gnoemes.shikimori.data.repository.series.smotretanime.Anime365TokenSourceImpl;
import com.gnoemes.shikimori.data.repository.user.UserRepository;
import com.gnoemes.shikimori.data.repository.user.UserRepositoryImpl;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Reusable;

@Module
public interface RepositoryModule {

    @Binds
    @Singleton
    TokenRepository bindTokenRepository(TokenRepositoryImpl repository);

    @Binds
    @Singleton
    TokenSource bindTokenSource(TokenSourceImpl source);

    @Binds
    @Reusable
    UserRepository bindUserRepository(UserRepositoryImpl repository);

    @Binds
    @Reusable
    RatesRepository bindRatesRepository(RatesRepositoryImpl repository);

    @Binds
    @Reusable
    AuthorizationRepository bindAuthorizationRepository(AuthorizationRepositoryImpl repository);

    @Binds
    @Singleton
    DownloadSource bindDownloadSource(DownloadSourceImpl source);

    @Binds
    @Reusable
    DownloadRepository bindDownloadRepository(DownloadRepositoryImpl repository);

    @Binds
    @Singleton
    TaskRepository bindTaskRepository(TaskRepostioryImpl repostiory);

    @Binds
    @Singleton
    Anime365TokenSource bindAnime365TokenSource(Anime365TokenSourceImpl source);
}
