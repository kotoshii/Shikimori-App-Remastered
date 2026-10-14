package com.gnoemes.shikimori.di.character;

import com.gnoemes.shikimori.data.repository.roles.CharacterRepository;
import com.gnoemes.shikimori.data.repository.roles.CharacterRepositoryImpl;
import com.gnoemes.shikimori.data.repository.roles.converter.CharacterDetailsResponseConverter;
import com.gnoemes.shikimori.data.repository.roles.converter.CharacterDetailsResponseConverterImpl;
import com.gnoemes.shikimori.di.details.DetailsUtilModule;
import com.gnoemes.shikimori.domain.roles.CharacterInteractor;
import com.gnoemes.shikimori.domain.roles.CharacterInteractorImpl;
import com.gnoemes.shikimori.presentation.presenter.character.converter.CharacterDetailsViewModelConverter;
import com.gnoemes.shikimori.presentation.presenter.character.converter.CharacterDetailsViewModelConverterImpl;

import dagger.Binds;
import dagger.Module;

@Module(includes = DetailsUtilModule.class)
public interface CharacterModule {

    @Binds
    CharacterInteractor bindCharacterInteractor(CharacterInteractorImpl interactor);

    @Binds
    CharacterRepository bindCharacterRepository(CharacterRepositoryImpl repository);

    @Binds
    CharacterDetailsResponseConverter bindCharacterDetailsResponseConverter(CharacterDetailsResponseConverterImpl converter);

    @Binds
    CharacterDetailsViewModelConverter bindCharacterDetailsViewModelConterter(CharacterDetailsViewModelConverterImpl conterter);
}
