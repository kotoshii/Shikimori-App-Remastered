package com.gnoemes.shikimori.presentation.view.friends.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.ItemUserMoreBinding
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.user.presentation.FriendViewModel
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.inflate
import com.gnoemes.shikimori.utils.onClick
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate

class FriendAdapterDelegate(
        private val imageLoader: ImageLoader,
        private val callback: (Type, Long) -> Unit
) : AbsListItemAdapterDelegate<FriendViewModel, Any, FriendAdapterDelegate.ViewHolder>() {

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean =
            item is FriendViewModel

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_user_more))

    override fun onBindViewHolder(item: FriendViewModel, holder: ViewHolder, payloads: MutableList<Any>) {
        holder.bind(item)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val binding = ItemUserMoreBinding.bind(view)

        private lateinit var item: FriendViewModel

        init {
            binding.container.onClick { callback.invoke(Type.USER, item.id) }
        }

        fun bind(item: FriendViewModel) {
            this.item = item

            with(itemView) {
                imageLoader.setCircleImage(binding.avatarView, item.image.x160)
                binding.nameView.text = item.name
                binding.lastOnlineView.text = item.lastOnline
            }
        }

    }
}
