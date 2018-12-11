package phone_min_uiux.mu.gbtransfer

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_client.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.ServerSocket
import java.net.Socket

class ClientActivity : AppCompatActivity() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var socket: Socket? = null
    private var dataInputStream: DataInputStream? = null
    private var dataOutputStream: DataOutputStream? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)
        setupWakeLog()

        connectButton.setOnClickListener {
            connectButton.visibility = View.GONE
            progressBar.visibility = View.VISIBLE

            doAsync {
                socket = Socket(Utils.ip, Utils.port)
                socket?.soTimeout = Utils.timeout
                dataInputStream = DataInputStream(socket?.getInputStream())
                dataOutputStream = DataOutputStream(socket?.getOutputStream())

                uiThread {
                    textView.text = "Connected, Receiving file..."
                    val timeStart = System.currentTimeMillis()
                    // receive file here

                    doAsync {
                        val fileOutputStream = FileOutputStream(File(filesDir, "file"))
                        val byteArray = ByteArray(Utils.byteArraySize)
                        var count = dataInputStream?.read(byteArray) ?: 0
                        while (count > 0) {
                            fileOutputStream.write(byteArray, 0, count)
                            count = dataInputStream?.read(byteArray) ?: 0
                        }
                        fileOutputStream.close()
                        socket?.close()
                        dataInputStream?.close()
                        dataOutputStream?.close()
                        uiThread {
                            val timeEnd = System.currentTimeMillis()
                            val timeDiff = timeEnd - timeStart
                            val timeSec = timeDiff / 1000
                            textView.text = "File received in ${timeSec} s\n\n${textView.text}"
                            progressBar.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun setupWakeLog() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        wakeLock =
                (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                    newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag").apply {
                        acquire(Utils.timeout.toLong())
                    }
                }
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.release()
        socket?.close()
        dataInputStream?.close()
        dataOutputStream?.close()
    }
}