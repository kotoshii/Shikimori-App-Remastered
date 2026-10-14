package com.gnoemes.shikimori.entity.series.domain;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public enum TranslationType {
    @SerializedName(value = "fandub", alternate = "dub")
    VOICE_RU("fandub", "озвучка"),
    @SerializedName(value = "subtitles", alternate = "sub")
    SUB_RU("subtitles", "субтитры"),
    @SerializedName("raw")
    RAW("raw", "оригинал"),
    @SerializedName("all")
    ALL("all", "не выбрано");

    private final String type;
    //Shikicinema's word for the kind, which isEqualType accepts too. Not a label: the screens show
    //the strings in res, see TranslationTypeExtensions.kt
    private final String shikicinemaKind;

    TranslationType(String type, String shikicinemaKind) {
        this.type = type;
        this.shikicinemaKind = shikicinemaKind;
    }

    public boolean isEqualType(@Nullable String otherType) {
        return type.equals(otherType) || shikicinemaKind.equals(otherType);
    }

    @Nullable
    public String getType() {
        return type;
    }

    public String getShikicinemaKind() {
        return shikicinemaKind;
    }
}
