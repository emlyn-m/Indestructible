package xyz.emlyn.indestructible

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.room.Ignore
import java.io.*
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

        // setup user consent query
        val sp = getSharedPreferences(getString(R.string.sharedprefs), MODE_PRIVATE)
        if (sp.getBoolean(getString(R.string.needs_consent), true)) {

            uiHandler.postDelayed({
                findViewById<TextView>(R.id.consent_accept).isEnabled = true
            }, 3000)


            val inflater = this.layoutInflater
            val consentView =
                inflater.inflate(R.layout.consent_layout, findViewById(R.id.main_activity_cl))

            consentView.findViewById<TextView>(R.id.user_consent_desc).movementMethod = LinkMovementMethod.getInstance()
            consentView.findViewById<TextView>(R.id.consent_accept).setOnClickListener {
                findViewById<ConstraintLayout>(R.id.main_activity_cl).removeView(findViewById(R.id.user_consent_cl))

                val spedit = sp.edit()
                spedit.putBoolean(getString(R.string.needs_consent), false)
                spedit.apply()

                File("/data/data/xyz.emlyn.indestructible/log")
                    .appendText(String.format(getString(R.string.user_consent_accept), LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS"))))
                File("/data/data/xyz.emlyn.indestructible/service_killed").createNewFile()
            }
        }


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

        // Copy instagram_observer_executable from /res/raw to /data/data/xyz.emlyn.indestructible/
        val igObserverExe = File("/data/data/xyz.emlyn.indestructible/InstagramObserver")
        if (!igObserverExe.exists()) {
            val inStream: InputStream = resources.openRawResource(R.raw.instagram_observer_executable)
            val out = FileOutputStream("/data/data/xyz.emlyn.indestructible/InstagramObserver")
            val buff = ByteArray(1024)
            var read : Int

            try {
                while (inStream.read(buff).also { read = it } > 0) {
                    out.write(buff, 0, read)
                }
            } finally {
                inStream.close()
                out.close()
            }
            Runtime.getRuntime().exec("chmod 777 /data/data/xyz.emlyn.indestructible/InstagramObserver")

        }

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

    @Suppress("unused_parameter")
    @SuppressLint("SdCardPath")
    fun onClick(ignored : View) {

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


    override fun onResume() {
        super.onResume()
        setLogTV(logFile)
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
        logTV.text = "$existingTextSB>"

        // auto-scroll to bottom on new log event
        // postdelayed bc parent func (this) called in oncreate before layout finished
        uiHandler.postDelayed({ findViewById<ScrollView>(R.id.logSV).fullScroll(View.FOCUS_DOWN) }, 100)

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