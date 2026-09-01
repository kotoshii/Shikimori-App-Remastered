package com.gnoemes.shikimori.presentation.view.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.gnoemes.shikimori.R
import java.io.File

/**
 * Hands a downloaded apk to the system installer, asking for the "install unknown apps" permission
 * first when it is missing.
 *
 * It exists as an activity because that permission can only be requested from one, and because the
 * grant screen returns a result: without this, "Установить" would send the user to a settings page
 * and then leave them to find their way back and tap the notification a second time. It draws
 * nothing - it is translucent, does its work in `onCreate`, and finishes.
 */
class InstallActivity : Activity() {

    companion object {
        private const val TAG = "AppUpdate"

        private const val EXTRA_PATH = "install_path"
        private const val REQUEST_GRANT = 4230

        private const val APK_MIME = "application/vnd.android.package-archive"

        fun intent(context: Context, apk: File): Intent =
                Intent(context, InstallActivity::class.java)
                        .putExtra(EXTRA_PATH, apk.absolutePath)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private var apk: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val path = intent?.getStringExtra(EXTRA_PATH)
        val file = path?.let(::File)

        if (file == null || !file.exists()) {
            //the apk is deleted when a newer one is downloaded, so a stale notification can land here
            Log.e(TAG, "nothing to install at $path")
            toast(R.string.update_install_failed)
            finish()
            return
        }

        apk = file

        if (needsPermission()) requestPermission() else installAndFinish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_GRANT) return

        //the grant screen reports cancelled even when the switch was turned on, so ask the system
        //what the state actually is rather than trusting the result code
        if (needsPermission()) {
            Log.d(TAG, "install permission was not granted")
            finish()
        } else installAndFinish()
    }

    private fun needsPermission(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()

    private fun requestPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))

        //not every rom ships that screen; letting the installer refuse says more than doing nothing
        if (intent.resolveActivity(packageManager) != null) startActivityForResult(intent, REQUEST_GRANT)
        else {
            Log.w(TAG, "no screen for ACTION_MANAGE_UNKNOWN_APP_SOURCES on this device")
            installAndFinish()
        }
    }

    private fun installAndFinish() {
        val file = apk ?: return finish()

        try {
            startActivity(installIntent(file))
        } catch (e: Exception) {
            Log.e(TAG, "could not open the installer for ${file.absolutePath}", e)
            toast(R.string.update_install_failed)
        }

        finish()
    }

    /**
     * A `content://` uri from `FileProvider` is required from Android 7 - a `file://` one throws
     * `FileUriExposedException`. Before that it is the opposite: the old installer cannot read a
     * content uri, and the apk sits in external storage where it can read the path directly.
     */
    private fun installIntent(file: File): Intent {
        val intent = Intent(Intent.ACTION_VIEW).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            intent.setDataAndType(uri, APK_MIME).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            intent.setDataAndType(Uri.fromFile(file), APK_MIME)
        }
    }

    private fun toast(messageRes: Int) = Toast.makeText(this, messageRes, Toast.LENGTH_LONG).show()
}
