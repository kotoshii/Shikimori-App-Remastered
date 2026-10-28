package com.gnoemes.shikimori.presentation.presenter.common.paginator

import com.gnoemes.shikimori.entity.common.domain.Page
import io.reactivex.Single
import io.reactivex.disposables.Disposable

class PageOffsetPaginator<T>(
        private val requestFactory: (page: Int) -> Single<Page<T>>,
        private val viewController: ViewController<T>
) : Paginator {

    private val defaultPage = 1
    private var currentPage: Int = defaultPage

    private var currentState: State<T> = EMPTY()

    private val currentData = mutableListOf<T>()
    private var disposable: Disposable? = null

    override fun refresh() = currentState.refresh()
    override fun loadNewPage() = currentState.loadNewPage()
    override fun release() = currentState.release()

    //a page that says it is the last ends the list: an empty page was the only end, one request more
    //than the list needed (P7-T5 17.4)
    private fun stateAfter(page: Page<T>): State<T> = if (page.isLast) ALL_DATA() else DATA()

    private fun loadPage(page: Int) {
        disposable?.dispose()
        disposable = requestFactory.invoke(page)
                .subscribe(
                        { currentState.newData(it) },
                        { currentState.error(it) }
                )
    }


    private inner class EMPTY : State<T> {

        override fun refresh() {
            currentState = EMPTY_PROGRESS()
            viewController.showEmptyProgress(true)
            loadPage(defaultPage)
        }

        override fun release() {
            currentState = RELEASED()
            disposable?.dispose()
        }
    }

    private inner class EMPTY_PROGRESS : State<T> {

        override fun newData(page: Page<T>) {
            if (page.items.isNotEmpty()) {
                currentState = stateAfter(page)
                currentData.clear()
                currentData.addAll(page.items)
                currentPage = defaultPage
                viewController.showData(true, currentData)
                viewController.showEmptyProgress(false)
                if (page.isLast) viewController.onAllData()
            } else {
                currentState = EMPTY_DATA()
                viewController.showEmptyProgress(false)
                viewController.showEmptyView(true)
            }
        }

        override fun error(error: Throwable) {
            currentState = EMPTY_ERROR()
            viewController.showEmptyProgress(false)
            viewController.showEmptyError(true, error)
        }

        override fun release() {
            currentState = RELEASED()
            disposable?.dispose()
        }
    }

    private inner class EMPTY_ERROR : State<T> {

        override fun refresh() {
            currentState = EMPTY_PROGRESS()
            viewController.showEmptyError(false)
            viewController.showEmptyProgress(true)
            loadPage(defaultPage)
        }

        override fun release() {
            currentState = RELEASED()
            disposable?.dispose()
        }
    }

    private inner class EMPTY_DATA : State<T> {

        override fun refresh() {
            currentState = EMPTY_PROGRESS()
            viewController.showEmptyView(false)
            viewController.showEmptyProgress(true)
            loadPage(defaultPage)
        }

        override fun release() {
            currentState = RELEASED()
            disposable?.dispose()
        }
    }

    private inner class DATA : State<T> {

        override fun refresh() {
            currentState = REFRESH()
            viewController.showRefreshProgress(true)
            loadPage(defaultPage)
        }

        //the page number moves on only once its page has come: a failed page was skipped for good (BUGS #34)
        override fun loadNewPage() {
            currentState = PAGE_PROGRESS()
            viewController.showPageProgress(true)
            loadPage(currentPage + 1)
        }

        override fun release() {
            currentState = RELEASED()
            disposable?.dispose()
        }
    }

    private inner class REFRESH : State<T> {

        override fun newData(page: Page<T>) {
            if (page.items.isNotEmpty()) {
                currentState = stateAfter(page)
                currentData.clear()
                currentData.addAll(page.items)
                currentPage = defaultPage
                viewController.showRefreshProgress(false)
                viewController.showData(true, currentData)
                if (page.isLast) viewController.onAllData()
            } else {
                currentState = EMPTY_DATA()
                currentData.clear()
                viewController.showData(false)
                viewController.showRefreshProgress(false)
                viewController.showEmptyView(true)
            }
        }

        override fun error(error: Throwable) {
            currentState = DATA()
            viewController.showRefreshProgress(false)
            viewController.showError(error)
        }

        override fun release() {
            currentState = RELEASED()
            disposable?.dispose()
        }
    }

    private inner class PAGE_PROGRESS : State<T> {

        override fun newData(page: Page<T>) {
            if (page.items.isNotEmpty()) {
                currentState = stateAfter(page)
                currentData.addAll(page.items)
                currentPage += 1
                viewController.showPageProgress(false)
                viewController.showData(true, currentData)
                if (page.isLast) viewController.onAllData()
            } else {
                currentState = ALL_DATA()
                viewController.showPageProgress(false)
                viewController.onAllData()
            }
        }

        override fun refresh() {
            currentState = REFRESH()
            viewController.showPageProgress(false)
            viewController.showRefreshProgress(true)
            loadPage(defaultPage)
        }

        override fun error(error: Throwable) {
            currentState = PAGE_ERROR()
            viewController.showPageProgress(false)
            viewController.showPageError(true, error)
        }

        //the spinner row would outlive the list: a screen that drops its paginator shows another one
        override fun release() {
            currentState = RELEASED()
            disposable?.dispose()
            viewController.showPageProgress(false)
        }
    }

    /**
     * A later page failed: the pages loaded stay, and the list ends with «Повторить» instead of hiding
     * behind the error view (BUGS #55). [loadNewPage] asks for the failed page again; only the user
     * calls it here - the list's own end-of-list loading waits while «Повторить» shows, or it would ask
     * in a loop (BasePaginationFragment).
     */
    private inner class PAGE_ERROR : State<T> {

        override fun loadNewPage() {
            currentState = PAGE_PROGRESS()
            viewController.showPageError(false)
            viewController.showPageProgress(true)
            loadPage(currentPage + 1)
        }

        override fun refresh() {
            currentState = REFRESH()
            viewController.showPageError(false)
            viewController.showRefreshProgress(true)
            loadPage(defaultPage)
        }

        override fun release() {
            currentState = RELEASED()
            viewController.showPageError(false)
        }
    }

    private inner class ALL_DATA : State<T> {

        override fun refresh() {
            currentState = REFRESH()
            viewController.showRefreshProgress(true)
            loadPage(defaultPage)
        }

        override fun release() {
            currentState = RELEASED()
            disposable?.dispose()
        }
    }

    private inner class RELEASED : State<T>
}