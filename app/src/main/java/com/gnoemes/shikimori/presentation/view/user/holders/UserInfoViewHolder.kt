package com.gnoemes.shikimori.presentation.view.user.holders

import android.view.View
import com.facebook.shimmer.ShimmerFrameLayout
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.user.presentation.UserInfoViewModel
import com.gnoemes.shikimori.entity.user.presentation.UserProfileAction
import com.gnoemes.shikimori.presentation.view.common.holders.DetailsPlaceholderViewHolder
import com.gnoemes.shikimori.utils.*
import kotlinx.android.synthetic.main.layout_user_profile_info.view.*
import kotlinx.android.synthetic.main.layout_user_profile_info_content.view.*

class UserInfoViewHolder(
        private val view: View,
        private val actionCallback : (UserProfileAction) -> Unit
) {

    private val placeholder by lazy { DetailsPlaceholderViewHolder(view.infoContent, view.infoPlaceholder as ShimmerFrameLayout) }
    private lateinit var item: UserInfoViewModel

    init {
        with(view) {
            messageFab.background = messageFab.background.apply { tint(view.context.colorAttr(R.attr.colorPrimary)) }
            messageFab.setImageDrawable(context.drawable(R.drawable.ic_message, R.color.background_transparent_text))

            friendshipFab.background = friendshipFab.background.apply { tint(view.context.colorAttr(R.attr.colorPrimary)) }
            friendshipFab.setImageDrawable(context.drawable(R.drawable.ic_add_person, R.color.background_transparent_text))

            ignoreFab.background = ignoreFab.background.apply { tint(view.context.colorAttr(R.attr.colorPrimary)) }
            ignoreFab.setImageDrawable(context.drawable(R.drawable.ic_visibility_off, R.color.background_transparent_text))

            bansFab.background = bansFab.background.apply { tint(view.context.colorAttr(R.attr.colorPrimary)) }
            bansFab.setImageDrawable(context.drawable(R.drawable.ic_ban_history, R.color.background_transparent_text))

            messageFab.onClick { actionCallback.invoke(UserProfileAction.Message) }
            friendshipFab.onClick { actionCallback.invoke(UserProfileAction.ChangeFriendshipStatus(!item.isFriend)) }
            ignoreFab.onClick { actionCallback.invoke(UserProfileAction.ChangeIgnoreStatus(!item.isIgnored)) }
            bansFab.onClick { actionCallback.invoke(UserProfileAction.Bans) }
            aboutBtn.onClick { actionCallback.invoke(UserProfileAction.About) }
        }
    }

    fun bind(item: UserInfoViewModel) {
        this.item = item
        placeholder.showContent()

        with(view) {
            if (item.isMe) {
                friendshipFab.gone()
                ignoreFab.gone()
            }

            if (item.isFriend) {
                friendshipFab.background = friendshipFab.background.apply { tint(view.context.colorAttr(R.attr.colorAccent)) }
                friendshipFab.setImageDrawable(context.drawable(R.drawable.ic_delete_person)?.apply { tint(colorAttr(R.attr.colorAccent)) })
            }

            if (item.isIgnored) {
                ignoreFab.background = ignoreFab.background.apply { tint(view.context.colorAttr(R.attr.colorAccent)) }
                ignoreFab.setImageDrawable(context.drawable(R.drawable.ic_visibility)?.apply { tint(colorAttr(R.attr.colorAccent)) })
            }

        }
    }

}