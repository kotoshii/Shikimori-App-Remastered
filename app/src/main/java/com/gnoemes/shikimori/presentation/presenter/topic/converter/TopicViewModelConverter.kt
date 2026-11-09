package com.gnoemes.shikimori.presentation.presenter.topic.converter

import com.gnoemes.shikimori.entity.topic.domain.Topic
import com.gnoemes.shikimori.entity.topic.presentation.TopicViewModel
import io.reactivex.functions.Function

interface TopicViewModelConverter : Function<List<Topic>, List<TopicViewModel>> {

    /**
     * One topic; [withFooter] adds the wall the api sends apart (`html_footer`), for the topic page — the
     * forum list shows the post alone (P8-T4 plan §1). Reads HTML: not on the main thread.
     */
    fun convertTopic(it: Topic, withFooter: Boolean = true): TopicViewModel
}