package com.gnoemes.shikimori.utils

import com.gnoemes.shikimori.entity.app.domain.Constants
import java.io.File

/**
 * Where the app saves its files: a `ShikimoriApp` folder inside the one chosen in settings, so a
 * shared folder such as Android's Downloads does not get an "anime" and a "screenshots" that say
 * nothing about which app made them. A chosen folder that already has that name is used as it is
 * rather than nested.
 */
fun appFolder(chosen: String): File {
    val folder = File(chosen)

    return if (folder.name.equals(Constants.APP_FOLDER_NAME, ignoreCase = true)) folder
    else File(folder, Constants.APP_FOLDER_NAME)
}
