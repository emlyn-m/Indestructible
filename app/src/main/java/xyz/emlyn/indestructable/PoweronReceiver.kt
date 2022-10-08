package xyz.emlyn.Indestructible

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PoweronReceiver : BroadcastReceiver() {

    @SuppressLint("SdCardPath")
    override fun onReceive(context: Context, intent: Intent) {
        File("/data/data/xyz.emlyn.Indestructible/log").delete()
        val logFile = File("/data/data/xyz.emlyn.Indestructible/log")
        logFile.writeText(String.format(context.getString(R.string.event_log_preifx), LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS"))))

        //TODO: Add trigger for service in this file
        //TODO: In service, add low

    }
}