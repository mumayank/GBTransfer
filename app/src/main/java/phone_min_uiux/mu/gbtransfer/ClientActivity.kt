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

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        wakeLock =
                (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                    newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag").apply {
                        acquire(1000 * 60 * 60 * 6) // 6 hours
                    }
                }

        connectButton.setOnClickListener {
            connectButton.visibility = View.GONE
            progressBar.visibility = View.VISIBLE

            doAsync {
                socket = Socket("192.168.43.1", 5000)
                socket?.soTimeout = 1000 * 60 * 60 * 6 // 6 hours
                dataInputStream = DataInputStream(socket?.getInputStream())
                dataOutputStream = DataOutputStream(socket?.getOutputStream())

                uiThread {
                    textView.text = "Connected, Receiving file..."
                    // receive file here

                    doAsync {
                        val fileOutputStream = FileOutputStream(File(filesDir, "file"))
                        val byteArray = ByteArray(1024)
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
                            textView.text = "File received.\n\n${textView.text}"
                            progressBar.visibility = View.GONE
                        }
                    }
                }
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