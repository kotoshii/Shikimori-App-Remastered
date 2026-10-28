package com.gnoemes.shikimori.presentation.view.settings.fragments

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import com.afollestad.materialdialogs.files.fileChooser
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.data.local.preference.SettingsBackup
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.app.domain.SettingsExtras
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseBottomSheetDialogFragment
import com.gnoemes.shikimori.utils.*
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.dialog_backup.*
import kotlinx.android.synthetic.main.dialog_base_bottom_sheet.*
import kotlinx.android.synthetic.main.layout_backup_save.view.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import javax.inject.Inject

class BackupDialog : BaseBottomSheetDialogFragment() {

    @Inject
    lateinit var settingsBackup: SettingsBackup

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        peekHeight = context.dimenAttr(android.R.attr.actionBarSize)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(toolbar) {
            setTitle(R.string.settings_backup_title)
            inflateMenu(R.menu.menu_close)
            onMenuClick { dismiss(); true }
        }

        saveLayout.deviceBtn.onClick { writeFileToPublic(createBackupPrivate()) }
        saveLayout.shareBtn.onClick { shareBackup() }
        downloadLayout.deviceBtn.onClick { findBackupLocal() }
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override fun getDialogLayout(): Int = R.layout.dialog_backup

    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    private fun createBackupPrivate(): File {
        val file = File(context!!.filesDir, Constants.BACKUP_FILE_NAME)

        try {
            if (!file.exists()) file.createNewFile()
            val writer = FileWriter(file.absoluteFile)
            BufferedWriter(writer).run {
                write(settingsBackup.toJson())
                close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return file
    }

    private fun writeFileToPublic(file: File) {
        activity!!.askStoragePermission {
            try {
                val androidDownloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
                val path = context!!.getDefaultSharedPreferences().getString(SettingsExtras.DOWNLOAD_FOLDER, androidDownloadFolder)
                        ?: androidDownloadFolder
                //into the app's folder, beside the downloads; copyTo creates the folder if needed
                file.copyTo(File(appFolder(path), Constants.BACKUP_FILE_NAME), overwrite = true)
                Toast.makeText(context, R.string.backup_saved, Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(context, R.string.backup_write_error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun findBackupLocal() {
        activity!!.askStoragePermission {
            try {
                val androidDownloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
                val chosenFolder = context!!.getDefaultSharedPreferences().getString(SettingsExtras.DOWNLOAD_FOLDER, "").orEmpty()
                val folders = listOf(chosenFolder, androidDownloadFolder).filter { it.isNotEmpty() }

                //the app's folder first, where backups are saved now, then the folders themselves,
                //where they were saved before it, so an older backup is still found
                val found = (folders.map { appFolder(it) } + folders.map { File(it) })
                        .map { File(it, Constants.BACKUP_FILE_NAME) }
                        .firstOrNull { it.exists() }

                val read: (File?) -> Unit = { readBackup(it) }

                if (found != null) {
                    context?.fileFoundDialog({ read.invoke(found) }) { showFolderChooserDialog(read) }
                } else {
                    showFolderChooserDialog(read)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, R.string.backup_read_error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun readBackup(file: File?) {
        if (file == null || !file.exists()) {
            Toast.makeText(context, R.string.backup_read_error, Toast.LENGTH_LONG).show()
            return
        }

        try {
            val result = settingsBackup.restore(String(file.readBytes()))

            val text = if (result == 0) R.string.backup_empty_file else R.string.backup_read_success
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, R.string.backup_read_error, Toast.LENGTH_LONG).show()
        }
    }

    private fun shareBackup() {
        try {
            context?.shareFile(createBackupPrivate().absolutePath)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            Toast.makeText(context, R.string.backup_write_error, Toast.LENGTH_LONG).show()
        }
    }

    private fun Context.fileFoundDialog(onAccepted: () -> Unit, onCancel: () -> Unit) {
        materialDialog().show {
            title(res = R.string.backup_found_title)
            message(res = R.string.backup_found_message)
            positiveButton(res = R.string.common_apply) { onAccepted.invoke() }
            negativeButton(res = R.string.filter_select) { onCancel.invoke() }
        }
    }

    private fun showFolderChooserDialog(onFileChosen: (File) -> Unit) {
        context!!.materialDialog().show {
            fileChooser(
                    allowFolderCreation = false,
                    emptyTextRes = R.string.download_folder_empty)
            { _, file -> onFileChosen.invoke(file) }
        }
    }

    private fun Context.shareFile(path: String?) {
        val share = Intent(Intent.ACTION_SEND)
        share.type = "text/plain"

        val media = File(path)
        val uri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", media)

        share.putExtra(Intent.EXTRA_STREAM, uri)
        share.clipData = ClipData.newRawUri("", uri)
        share.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION


        val shareChooser = Intent.createChooser(share, getString(R.string.common_share))
        shareChooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION

        startActivity(shareChooser)
    }
}
