package xyz.emlyn.indestructible

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
import android.os.FileObserver
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import java.io.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class BackupService : Service() {

    private var wakeLock : PowerManager.WakeLock? = null
    private var isServiceStarted = false

    private lateinit var observer : FixedFileObserver

    override fun onBind(intent: Intent): IBinder? {
        return null  // not binding this service
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

       //TODO: Check if start/stop service

        return START_STICKY  // if ever killed, auto-restart
    }


    @SuppressLint("SdCardPath")
    override fun onCreate() {
        super.onCreate()

        val notification = createNotification()
        startService()

        observer = object:FixedFileObserver(File("/data/data/xyz.emlyn.indestructible/")) {
            override fun onEvent(event: Int, path: String?) {
                if (event == FileObserver.CREATE && path == "kill_service") {
                    File("/data/data/xyz.emlyn.indestructible/kill_service").delete()
                    stopService()
                }

                if (event == FileObserver.CREATE && path =="direct.db") {
                    Log.d("xyz.emlyn", "DIRECT.DB CREATED")
                    mergeDatabase()
                }
            }
        }
        observer.startWatching()

        startForeground(1, notification)
    }


    @SuppressLint("WakelockTimeout", "SdCardPath")
    private fun startService() {
        //Start c++ code
        Runtime.getRuntime().exec(arrayOf("su", "-M", "-c", "/data/data/xyz.emlyn.indestructible/InstagramObserver"))

        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BackupService::lock").apply {
                    acquire()
                }
            }

        File("/data/data/xyz.emlyn.indestructible/log")
            .appendText(String.format(getString(R.string.service_start_log), LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS"))))

        File("/data/data/xyz.emlyn.indestructible/service_created").createNewFile()

    }

    @SuppressLint("SdCardPath")
    private fun stopService() {

        // Create c++ kill signal
        File("/data/data/xyz.emlyn.indestructible/kill_sig").createNewFile()

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

        // kill observer
        observer.stopWatching()


        File("/data/data/xyz.emlyn.indestructible/log")
            .appendText(String.format(getString(R.string.service_stop_log), LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS"))))
        File("/data/data/xyz.emlyn.indestructible/service_killed").createNewFile()

    }


    private fun createNotification() : Notification {
        val notifChannelId = "BACKUP_RESTORE_SERVICE_CHANNEL"

        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            notifChannelId,
            "Indestructible backup/restore service notification",
            NotificationManager.IMPORTANCE_MIN
        ).let {
            it.description = "Indestructible backup service running"
            it.enableLights(false)
            it.enableVibration(false)
            it
        }
        notifManager.createNotificationChannel(channel)

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        val builder = Notification.Builder(this, notifChannelId)
        return builder
            .setContentTitle("Indestructible")
            .setContentText("Running in background")
            .setContentIntent(pendingIntent)
            .build()

    }


    @SuppressLint("SdCardPath")
    private fun mergeDatabase() {
        // check dbBak exists
        if (!File("/data/data/xyz.emlyn.indestructible/direct-bak.db").exists()) {
            copy("/data/data/xyz.emlyn.indestructible/direct.db", "/data/data/xyz.emlyn.indestructible/direct-bak.db")
        }

        //janky af bullshit to avoid weird sqlite permission issues
        copy("/data/data/xyz.emlyn.indestructible/direct-copy.db","/data/data/xyz.emlyn.indestructible/direct.db")



        val dbBak = SQLiteDatabase.openDatabase("/data/data/xyz.emlyn.indestructible/direct-bak.db", null, OPEN_READWRITE)
        val dbNew = SQLiteDatabase.openDatabase("/data/data/xyz.emlyn.indestructible/direct-copy.db", null, OPEN_READWRITE)
        val dbBakCursor = dbBak.rawQuery("SELECT * FROM sqlite_schema", null)
        val dbNewCursor = dbNew.rawQuery("SELECT * FROM sqlite_schema", null)

        dbBakCursor.close()
        dbBak.close()
        dbNewCursor.close()
        dbNew.close()


    }


    private fun copy(src : String, dst : String) {
        val inS: InputStream = FileInputStream(File(src))
        inS.use { inStream ->
            val os: OutputStream = FileOutputStream(File(dst))
            os.use { out ->
                // Transfer bytes from in to out
                val buf = ByteArray(1024)
                var len: Int
                while (inStream.read(buf).also { len = it } > 0) {
                    out.write(buf, 0, len)
                }
            }
        }
    }
}