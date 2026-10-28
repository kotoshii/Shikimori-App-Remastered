package com.gnoemes.shikimori.utils

import android.Manifest
import android.content.Context
import android.os.Environment
import androidx.fragment.app.FragmentActivity
import com.afollestad.materialdialogs.files.folderChooser
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.app.domain.SettingsExtras
import com.kotlinpermissions.KotlinPermissions
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

/** Asks for the storage permissions that saving and reading files need, then runs [onAccepted]. */
fun FragmentActivity.askStoragePermission(onAccepted: () -> Unit) {
    KotlinPermissions.with(this)
            .permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
            .onAccepted { onAccepted() }
            .ask()
}

/**
 * Picks the download folder and saves it, opening on the one set before while it can still be written
 * to. [onChosen] runs once a folder is saved; [onPositive], when given, on «OK», picked or not.
 */
fun Context.chooseDownloadFolder(onChosen: () -> Unit = {}, onPositive: (() -> Unit)? = null) {
    val prefs = getDefaultSharedPreferences()
    val path = prefs.getString(SettingsExtras.DOWNLOAD_FOLDER, "").orEmpty()
    val initialDirectory = try {
        File(path).let { if (it.canWrite()) it else Environment.getExternalStorageDirectory() }
    } catch (e: Exception) {
        Environment.getExternalStorageDirectory()
    }

    materialDialog().show {
        folderChooser(
                initialDirectory = initialDirectory,
                allowFolderCreation = true,
                emptyTextRes = R.string.download_folder_empty,
                folderCreationLabel = R.string.download_new_folder)
        { _, file ->
            prefs.putString(SettingsExtras.DOWNLOAD_FOLDER, file.absolutePath)
            onChosen()
        }
        if (onPositive != null) positiveButton { onPositive() }
    }
}
