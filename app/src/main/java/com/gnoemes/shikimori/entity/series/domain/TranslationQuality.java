package com.gnoemes.shikimori.entity.series.domain;

import com.google.gson.annotations.SerializedName;

public enum TranslationQuality {
    @SerializedName("bd")
    BD,
    @SerializedName(value = "tv", alternate = "web")
    TV,
    @SerializedName("dvd")
    DVD,
}
