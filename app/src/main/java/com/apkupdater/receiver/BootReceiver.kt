package com.apkupdater.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.apkupdater.worker.UpdatesWorker

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            UpdatesWorker.launch(WorkManager.getInstance(context))
        }
    }

}
