package com.gnoemes.shikimori.data.repository.comments

import com.gnoemes.shikimori.data.network.CommentApi
import com.gnoemes.shikimori.data.repository.comments.converter.CommentResponseConverter
import com.gnoemes.shikimori.entity.comment.domain.Comment
import com.gnoemes.shikimori.entity.comment.domain.CommentableType
import com.gnoemes.shikimori.utils.dropExtraPageItem
import io.reactivex.Single
import javax.inject.Inject

class CommentRepositoryImpl @Inject constructor(
        private val api: CommentApi,
        private val converter: CommentResponseConverter
) : CommentRepository {

    override fun getList(id: Long, type: CommentableType, page: Int, limit: Int, desc: Int): Single<List<Comment>> =
            api.getComments(id, type.type, page, limit, desc)
                    .map { it.dropExtraPageItem(limit) }
                    .map(converter)
}