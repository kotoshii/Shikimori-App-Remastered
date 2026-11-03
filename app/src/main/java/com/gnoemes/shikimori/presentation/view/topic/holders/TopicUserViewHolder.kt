package com.gnoemes.shikimori.presentation.view.topic.holders

import android.graphics.drawable.ColorDrawable
import android.view.View
import com.gnoemes.shikimori.databinding.LayoutTopicUserBinding
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.topic.presentation.TopicUserViewModel
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.visibleIf

class TopicUserViewHolder(
        private val view: View,
        private val imageLoader: ImageLoader,
        private val navigationCallback: ((Type, Long) -> Unit)? = null
) {
    private val binding = LayoutTopicUserBinding.bind(view)
    private lateinit var item: TopicUserViewModel

    init {
        binding.userInfoGroup.setOnClickListener { navigationCallback?.invoke(Type.USER, item.user.id) }
    }

    fun bind(item: TopicUserViewModel) {
        this.item = item
        with(view) {
            imageLoader.setCircleImage(binding.avatarView, item.user.avatar)
            binding.nameView.text = item.user.nickname
            binding.dateView.text = item.createdDate

            binding.tagView.text = item.tag
            item.tagColor?.let { binding.tagView.background = ColorDrawable(item.tagColor) }
            binding.tagView.visibleIf { !item.tag.isNullOrBlank() }
        }
    }
}