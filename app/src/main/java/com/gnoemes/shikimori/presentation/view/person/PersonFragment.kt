package com.gnoemes.shikimori.presentation.view.person

import android.os.Bundle
import android.view.View
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.FragmentPersonBinding
import com.gnoemes.shikimori.entity.app.domain.AppExtras
import com.gnoemes.shikimori.entity.common.presentation.DetailsContentItem
import com.gnoemes.shikimori.entity.common.presentation.DetailsContentType
import com.gnoemes.shikimori.entity.common.presentation.DetailsDescriptionItem
import com.gnoemes.shikimori.entity.common.presentation.DetailsHeadSimpleItem
import com.gnoemes.shikimori.presentation.presenter.person.PersonPresenter
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseFragment
import com.gnoemes.shikimori.presentation.view.base.fragment.RouterProvider
import com.gnoemes.shikimori.presentation.view.common.adapter.content.ContentAdapter
import com.gnoemes.shikimori.presentation.view.common.holders.DetailsContentViewHolder
import com.gnoemes.shikimori.presentation.view.common.holders.DetailsDescriptionViewHolder
import com.gnoemes.shikimori.presentation.view.common.holders.DetailsHeadSimpleViewHolder
import com.gnoemes.shikimori.utils.addBackButton
import com.gnoemes.shikimori.utils.ifNotNull
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.withArgs
import javax.inject.Inject

class PersonFragment : BaseFragment<PersonPresenter, PersonView>(), PersonView {

    @Inject
    lateinit var imageLoader: ImageLoader

    @InjectPresenter
    lateinit var personPresenter: PersonPresenter

    @ProvidePresenter
    fun providePresenter(): PersonPresenter {
        personPresenter = presenterProvider.get()

        parentFragment.ifNotNull {
            personPresenter.localRouter = (parentFragment as RouterProvider).localRouter
        }

        arguments.ifNotNull {
            personPresenter.id = it.getLong(AppExtras.ARGUMENT_PERSON_ID)
        }

        return personPresenter
    }

    private lateinit var headHolder: DetailsHeadSimpleViewHolder
    private lateinit var descriptionHolder: DetailsDescriptionViewHolder
    private val contentHolders = HashMap<DetailsContentType, DetailsContentViewHolder>()

    private val charactersAdapter by lazy { ContentAdapter(imageLoader, getPresenter()::onContentClicked) }
    private val worksAdapter by lazy { ContentAdapter(imageLoader, getPresenter()::onContentClicked) }


    companion object {
        fun newInstance(id: Long) = PersonFragment().withArgs { putLong(AppExtras.ARGUMENT_PERSON_ID, id) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        screenToolbar?.apply {
            addBackButton { getPresenter().onBackPressed() }
            setTitle(R.string.common_person)
            inflateMenu(R.menu.menu_browser)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.item_open_in_browser -> getPresenter().onOpenInBrowser()
                }
                false
            }
        }

        headHolder = DetailsHeadSimpleViewHolder(binding.headLayout.root, imageLoader)
        descriptionHolder = DetailsDescriptionViewHolder(binding.descriptionLayout.root, imageLoader, getPresenter())

        contentHolders.apply {
            put(DetailsContentType.CHARACTERS, DetailsContentViewHolder(binding.charactersLayout.root, charactersAdapter))
            put(DetailsContentType.WORKS, DetailsContentViewHolder(binding.worksLayout.root, worksAdapter))
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override fun getPresenter(): PersonPresenter = personPresenter

    private val binding by viewBinding(FragmentPersonBinding::bind)

    override fun getFragmentLayout(): Int = R.layout.fragment_person

    ///////////////////////////////////////////////////////////////////////////
    // MVP
    ///////////////////////////////////////////////////////////////////////////

    override fun setHead(item: DetailsHeadSimpleItem) {
        headHolder.bind(item)
    }

    override fun setDescription(item: DetailsDescriptionItem) {
        descriptionHolder.bind(item)
    }

    override fun setContent(type: DetailsContentType, item: DetailsContentItem) {
        contentHolders[type]?.bind(type, item)
    }

    override fun showContent(show: Boolean) = Unit

    override val pageView: View? get() = binding.nestedScroll
}