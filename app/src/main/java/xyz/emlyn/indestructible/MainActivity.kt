package xyz.emlyn.indestructible

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.FileObserver
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import xyz.emlyn.Indestructible.R
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity() {

    private lateinit var logObserver : FileObserver  // create up here to prevent GC
    private lateinit var logFile : File

    @SuppressLint("SdCardPath")
    override fun onCreate(savedInstanceState: Bundle?) {
        window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mToolbar = findViewById<Toolbar>(R.id.toolbar)
        mToolbar.title = ""
        setSupportActionBar(mToolbar)

        logFile = File("/data/data/xyz.emlyn.Indestructible/log")

        if (!logFile.exists()) {
            logFile.createNewFile()
            logFile.writeText(String.format(getString(R.string.event_log_preifx), LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS"))))
        }

        setLogTV(logFile)

        logObserver = object:FileObserver(logFile) {
            override fun onEvent(event : Int, path : String?) {
                if (event == MODIFY) { runOnUiThread { setLogTV(logFile) } }
            }
        }
        logObserver.startWatching()

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

    }
}