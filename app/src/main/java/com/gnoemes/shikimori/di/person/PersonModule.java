package com.gnoemes.shikimori.di.person;

import com.gnoemes.shikimori.data.repository.roles.PersonRepository;
import com.gnoemes.shikimori.data.repository.roles.PersonRepositoryImpl;
import com.gnoemes.shikimori.data.repository.roles.converter.PersonDetailsResponseConverter;
import com.gnoemes.shikimori.data.repository.roles.converter.PersonDetailsResponseConverterImpl;
import com.gnoemes.shikimori.domain.roles.PersonInteractor;
import com.gnoemes.shikimori.domain.roles.PersonInteractorImpl;
import com.gnoemes.shikimori.presentation.presenter.common.converter.DetailsContentViewModelConverter;
import com.gnoemes.shikimori.presentation.presenter.common.converter.DetailsContentViewModelConverterImpl;
import com.gnoemes.shikimori.presentation.presenter.person.converter.PersonDetailsViewModelConverter;
import com.gnoemes.shikimori.presentation.presenter.person.converter.PersonDetailsViewModelConverterImpl;

import dagger.Binds;
import dagger.Module;
import dagger.Reusable;

@Module(includes = {
})
public interface PersonModule {

    @Binds
    PersonInteractor bindPersonInteractor(PersonInteractorImpl interactor);

    @Binds
    PersonRepository bindPersonRepository(PersonRepositoryImpl repository);

    @Binds
    PersonDetailsResponseConverter bindPersonDetailsResponseConverter(PersonDetailsResponseConverterImpl converter);

    @Binds
    PersonDetailsViewModelConverter bindPersonDetailsViewModelConverter(PersonDetailsViewModelConverterImpl conterter);

    @Binds
    @Reusable
    DetailsContentViewModelConverter bindDetailsContentViewModelConverter(DetailsContentViewModelConverterImpl converter);
}