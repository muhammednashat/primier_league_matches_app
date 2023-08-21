package com.example.primierleaguematches

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File


class ApkDownloader(private val context: Context) {

    private var downloadId: Long = 0

    fun downloadApk(apkUrl: String, apkName: String) {
        // Create a DownloadManager.Request with the download URL
        val downloadRequest = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Downloading $apkName")
            .setDescription("Please wait while $apkName is being downloaded.")
            .setNotificationVisibility(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // On Android 10 and above, show the notification only when the download is complete
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                } else {
                    // On earlier versions of Android, show the notification when the download starts and when it completes
                    DownloadManager.Request.VISIBILITY_VISIBLE
                }
            )

        // Set the destination directory for the downloaded APK file
        val apkDir = when {
            // On Android 10 and above, use the public Downloads directory
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            }
            // On earlier versions of Android, use the app's private external storage directory
            else -> {
                ContextCompat.getExternalFilesDirs(context, Environment.DIRECTORY_DOWNLOADS).firstOrNull()
            }
        }

        // Create the destination directory if it doesn't exist
        apkDir?.mkdirs()

        // Set the destination file path for the downloaded APK file
        val apkFile = File(apkDir, apkName)

        // Set the destination URI for the downloaded APK file
        downloadRequest.setDestinationUri(Uri.fromFile(apkFile))

        // Enqueue the download request with the DownloadManager
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(downloadRequest)

        // Show a toast message to the user
        Toast.makeText(context, "Downloading $apkName...", Toast.LENGTH_SHORT).show()

        // Register a BroadcastReceiver to show a notification when the download is complete
        val onDownloadComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId) {
                    // Unregister the BroadcastReceiver
                    context?.unregisterReceiver(this)

                    // Show a toast message to the user
                    Toast.makeText(context, "$apkName downloaded.", Toast.LENGTH_SHORT).show()

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        // On earlier versions of Android, install the APK from the file path
                        val apkUri = Uri.fromFile(apkFile)
                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(apkUri, "application/vnd.android.package-archive")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        val chooserIntent = Intent.createChooser(installIntent, "Install $apkName?")
                        context?.startActivity(chooserIntent)
                    } else {
                        // On Android 7.0 and above, install the APK from the content URI
                        val apkUri = FileProvider.getUriForFile(
                            context!!, "${context.packageName}.fileprovider", apkFile
                        )
                        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                            setData(apkUri)
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        context?.startActivity(installIntent)
                    }
                }
            }
        }

        context.registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    fun cancelDownload() {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.remove(downloadId)
        Toast.makeText(context, "Download cancelled.", Toast.LENGTH_SHORT).show()
    }
}