package com.gnoemes.shikimori.entity.app.domain

import com.gnoemes.shikimori.BuildConfig
import java.net.URLEncoder

object Constants {
    const val REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob"

    //the sign-in page, from the same build values AuthApi exchanges the code with
    val AUTH_URL = BuildConfig.ShikimoriBaseUrl + "oauth/authorize" +
            "?client_id=" + BuildConfig.ShikimoriClientId +
            "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "UTF-8") +
            "&response_type=code"

    const val DEFAULT_TIMEOUT = 15

    const val DEFAULT_LIMIT = 12

    const val LONG_TIMEOUT = 30

    const val NO_ID = -1L

    const val MAX_LIMIT = 1000

    //the most friends the api sends in one request
    const val MAX_FRIENDS_LIMIT = 100

    const val MAX_ANIME_LIMIT = 50

    const val EXIT_TIMEOUT = 3L * 1000

    const val DEFAULT_DEBOUNCE_INTERVAL = 300L


    const val TASK_LONG_DELAY = 3500L

    //dzen signs its stream links for the user agent that fetched the player page, so playback has
    //to send the very same one. Keep in sync with VideoApi.getPlayerHtml
    const val PLAYER_USER_AGENT = "Mozilla/5.0 (Linux; Android 4.4; Nexus 5 Build/_BuildID_) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36"

    const val GITHUB_API_URL = "https://api.github.com/"

    const val GITHUB_RELEASES_URL = "https://github.com/kotoshii/Shikimori-App-Remastered/releases/latest"

    const val GITHUB_ISSUES_URL = "https://github.com/kotoshii/Shikimori-App-Remastered/issues"

    const val FOUR_PDA_THEME_URL = "https://4pda.to/forum/index.php?showtopic=903970"

    const val SHIKICINEMA_URL = "https://smarthard.net/"

    const val MAX_PINNED_RATES = 3

    const val BACKUP_FILE_NAME= "shimori-backup.json"

    //the folder the app creates inside the download folder chosen in settings; named in the
    //setting's texts too
    const val APP_FOLDER_NAME = "ShikimoriApp"
}