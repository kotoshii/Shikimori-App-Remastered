package com.gnoemes.shikimori.entity.common.domain

import com.google.gson.annotations.SerializedName

enum class Status(val status: String) {
    @SerializedName("anons")
    ANONS("anons"),
    @SerializedName("ongoing")
    ONGOING("ongoing"),
    @SerializedName("released")
    RELEASED("released"),
    //manga and ranobe only; an unknown name reads as null, which was NONE for these two (BUGS #9)
    @SerializedName("paused")
    PAUSED("paused"),
    @SerializedName("discontinued")
    DISCONTINUED("discontinued"),
    NONE("none");
}