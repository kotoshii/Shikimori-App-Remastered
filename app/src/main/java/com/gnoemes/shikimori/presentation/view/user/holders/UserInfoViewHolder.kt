package com.gnoemes.shikimori.presentation.view.user.holders

import android.text.Html
import android.view.View
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.LayoutUserProfileInfoBinding
import com.gnoemes.shikimori.entity.user.presentation.UserInfoViewModel
import com.gnoemes.shikimori.entity.user.presentation.UserProfileAction
import com.gnoemes.shikimori.presentation.view.common.holders.DetailsPlaceholderViewHolder
import com.gnoemes.shikimori.utils.gone
import com.gnoemes.shikimori.utils.onClick

class UserInfoViewHolder(
        private val view: View,
        private val actionCallback: (UserProfileAction) -> Unit
) {

    private val binding = LayoutUserProfileInfoBinding.bind(view)

    private val placeholder by lazy { DetailsPlaceholderViewHolder(binding.infoContent.root, binding.infoPlaceholder.root) }
    private lateinit var item: UserInfoViewModel

    init {
        with(binding.infoContent) {
            messageFab.onClick { actionCallback.invoke(if (item.isMe) UserProfileAction.MessageBox else UserProfileAction.Message) }
            friendshipFab.onClick { actionCallback.invoke(UserProfileAction.ChangeFriendshipStatus(!item.isFriend)) }
            ignoreFab.onClick { actionCallback.invoke(UserProfileAction.ChangeIgnoreStatus(!item.isIgnored)) }
            historyFab.onClick { actionCallback.invoke(UserProfileAction.History) }
            aboutBtn.onClick { actionCallback.invoke(UserProfileAction.About) }
        }
    }

    fun bind(item: UserInfoViewModel) {
        this.item = item
        placeholder.showContent()

        with(binding.infoContent) {
            infoView.text = Html.fromHtml(item.info)

            if (item.isMe) {
                friendshipFab.gone()
                friendshipLabel.gone()
                ignoreFab.gone()
                ignoreLabel.gone()
                messageFab.setIconResource(R.drawable.ic_mail)
                messageLabel.setText(R.string.profile_message_box)
            }

            friendshipFab.isSelected = item.isFriend
            ignoreFab.isSelected = item.isIgnored
        }
    }

    //back to the placeholder, as before the first user
    fun reset() = placeholder.showPlaceholder()

}