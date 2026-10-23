package com.gnoemes.shikimori.entity.user.presentation

data class UserContentViewModel(
        val type: UserContentType,
        val content: List<UserContentItem>,
        val needShowMore: Boolean,
        val moreSize: Int,
        //the list was cut at the api's limit: [moreSize] is the least there is («ещё 95+»)
        val moreIsAtLeast: Boolean = false
)