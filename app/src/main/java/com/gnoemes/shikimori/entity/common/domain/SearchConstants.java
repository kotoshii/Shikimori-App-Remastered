package com.gnoemes.shikimori.entity.common.domain;

import androidx.annotation.NonNull;

public class SearchConstants {

    public static final String GENRE = "genre";
    public static final String GENRE_V2 = "genre_v2";

    ////////////////////////////////////////////////////////////////////
    // Queries
    ////////////////////////////////////////////////////////////////////

    public static final String SEARCH = "search";
    public static final String PAGE = "page";
    public static final String LIMIT = "limit";
    public static final String SEASON = "season";
    public static final String ORDER = "order";
    public static final String IDS = "ids";
    public static final String CENSORED = "censored";
    public static final String STUDIO = "studio";

    public enum ORDER_BY {
        ORDER("order"),
        ID("id"),
        RANKED("ranked"),
        TYPE("kind"),
        POPULARITY("popularity"),
        NAME("name"),
        AIRED_ON("aired_on"),
        EPISODES("episodes"),
        VOLUMES("volumes"),
        CHAPTERS("chapters"),
        STATUS("status"),
        RANDOM("random"),;

        private final String orderBy;

        ORDER_BY(String orderBy) {
            this.orderBy = orderBy;
        }

        @NonNull
        @Override
        public String toString() {
            return this.orderBy;
        }
    }

    public enum SEASONS {
        FALL("fall"),
        WINTER("winter"),
        SPRING("spring"),
        SUMMER("summer");

        private final String season;

        SEASONS(String season) {
            this.season = season;
        }

        @NonNull
        @Override
        public String toString() {
            return season;
        }
    }
}
