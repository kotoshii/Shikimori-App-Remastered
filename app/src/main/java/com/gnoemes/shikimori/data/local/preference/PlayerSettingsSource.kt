package com.gnoemes.shikimori.data.local.preference

interface PlayerSettingsSource {
    val isGesturesEnabled : Boolean
    val isVolumeAndBrightnessGesturesEnabled : Boolean
    val isVolumeAndBrightnessInverted : Boolean
    val isForwardRewindSlide : Boolean
    val isOpenLandscape : Boolean
    val isZoomProportional : Boolean
    val isAutoPip : Boolean

    val forwardRewindOffset : Long
    val forwardRewindOffsetBig : Long
}