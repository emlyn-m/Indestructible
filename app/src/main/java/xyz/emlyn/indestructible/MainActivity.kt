package xyz.emlyn.indestructible

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity() {

    private lateinit var logObserver : FixedFileObserver  // create up here to prevent GC
    private lateinit var logFile : File
    private lateinit var observeDirectory : File

    private lateinit var uiHandler : Handler


    @SuppressLint("SdCardPath")
    override fun onCreate(savedInstanceState: Bundle?) {
        window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mToolbar = findViewById<Toolbar>(R.id.toolbar)
        mToolbar.title = ""
        setSupportActionBar(mToolbar)

        uiHandler = Handler(Looper.getMainLooper())

        observeDirectory = File("/data/data/xyz.emlyn.indestructible")
        logFile = File("/data/data/xyz.emlyn.indestructible/log")

        if (!logFile.exists()) {
            logFile.createNewFile()
            logFile.writeText(String.format(getString(R.string.event_log_preifx), LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS"))))
        }

        setLogTV(logFile)

        logObserver = object:FixedFileObserver(observeDirectory) {
            override fun onEvent(event : Int, path : String?) {
                Log.d("xyz.emlyn", "MAFO+P="+path)
                if (event == FileObserver.MODIFY && path == "log") { runOnUiThread { setLogTV(logFile) } }

                if (event == FileObserver.CREATE && path == "service_created") {
                    File("/data/data/xyz.emlyn.indestructible/service_created").delete()
                    uiHandler.post { findViewById<TextView>(R.id.stateChangeTV).setText(R.string.kill) }
                }

                if (event == FileObserver.CREATE && path == "service_killed") {
                    File("/data/data/xyz.emlyn.indestructible/service_killed").delete()
                    uiHandler.post { findViewById<TextView>(R.id.stateChangeTV).setText(R.string.start) }
                }
            }
        }
        logObserver.startWatching()


        // UI Run delayed
        findViewById<ConstraintLayout>(R.id.startStopCL).setOnClickListener(this::onClick)
        Handler(Looper.getMainLooper()).postDelayed({
            val sttv = findViewById<TextView>(R.id.stateChangeTV)
            if (isMyServiceRunning(BackupService::class.java)) {
                sttv.setText(R.string.kill)
            } else {
                sttv.setText(R.string.start)
            }

            //todo: add icon changes

        }, 100)

    }

    @SuppressLint("SdCardPath")
    fun onClick(v : View) {

        val sttv = findViewById<TextView>(R.id.stateChangeTV)

        if (isMyServiceRunning(BackupService::class.java)) {
            sttv.setText(R.string.loading)
            uiHandler.postDelayed({ File("/data/data/xyz.emlyn.indestructible/kill_service").createNewFile() }, 200);

        } else {
            sttv.setText(R.string.loading)
            val startServIntent = Intent(this, BackupService::class.java)
            uiHandler.postDelayed({ this.startForegroundService(startServIntent) }, 200)

        }
    }

    @SuppressLint("SdCardPath", "SetTextI18n")
    fun setLogTV(lf : File) {

        val existingTextSB = StringBuilder()
        lateinit var br : BufferedReader
        try {
            br = BufferedReader(FileReader(lf))
            var line = br.readLine()

            while (line != null) {

                existingTextSB.append("> $line\n")
                line = br.readLine()
            }

            br.close()

        } catch (e : IOException) {
            Log.e("xyz.emlyn", e.message!!)
            br.close()
        }

        val logTV = findViewById<TextView>(R.id.loggingTV)
        logTV.text = "$existingTextSB>\n"

        Log.d("xyz.emlyn", "ltv")

    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}