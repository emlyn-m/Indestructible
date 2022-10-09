package xyz.emlyn.indestructible

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech.STOPPED
import android.telephony.ServiceState
import android.util.Log

class BackupService : Service() {

    private var wakeLock : PowerManager.WakeLock? = null
    private var isServiceStarted = false

    override fun onBind(intent: Intent): IBinder? {
        return null  // not binding this service
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

       //TODO: Check if start/stop service

        return START_STICKY  // if ever killed, auto-restart
    }


    override fun onCreate() {
        super.onCreate()

        val notification = createNotification()
        startForeground(1, notification)
    }


    @SuppressLint("WakelockTimeout")
    private fun startService() {
        //TODO: Proper code in here - trigger c code, etc.

        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BackupService::lock").apply {
                    acquire()
                }
            }
    }

    private fun stopService() {

        //TODO: Proper code in here - kill c code, etc.

        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            Log.e("xyz.emlyn", "Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
    }


    private fun createNotification() : Notification {
        val notifChannelId = "BACKUP_RESTORE_SERVICE_CHANNEL"

        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            notifChannelId,
            "Indestructible backup/restore service notification",
            NotificationManager.IMPORTANCE_HIGH
        ).let {
            it.description = "Indestructible running in background"
            it.enableLights(true)
            it.lightColor = Color.RED
            it.enableVibration(true)
            it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            it
        }
        notifManager.createNotificationChannel(channel)

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }

        val builder = Notification.Builder(this, notifChannelId)
        return builder
            .setContentTitle("Indestructible")
            .setContentText("Running in background")
            .setContentIntent(pendingIntent)
            .build()

    }
}