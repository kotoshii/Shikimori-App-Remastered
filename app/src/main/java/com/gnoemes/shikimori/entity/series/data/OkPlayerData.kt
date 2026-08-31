package com.gnoemes.shikimori.entity.series.data

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class OkPlayerData(
        @SerializedName("flashvars") val flashvars: Flashvars?
) {

    data class Flashvars(
            /**
             * ⚠️ Either a json *string* holding the metadata, which is how ok.ru used to write it and
             * why the parser decodes it in two steps, or the metadata object nested directly, which
             * is what it writes now. Kept as a raw element so both survive - see `OkParserImpl`.
             */
            @SerializedName("metadata") val metadata: JsonElement?
    )
}
