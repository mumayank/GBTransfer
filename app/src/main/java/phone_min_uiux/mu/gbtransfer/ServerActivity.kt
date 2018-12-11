package phone_min_uiux.mu.gbtransfer

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_server.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class ServerActivity : AppCompatActivity() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null
    private var dataInputStream: DataInputStream? = null
    private var dataOutputStream: DataOutputStream? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)
        setupWakeLog()

        startButton.setOnClickListener {
            startButton.visibility = View.GONE
            progressBar.visibility = View.VISIBLE

            doAsync {
                serverSocket = ServerSocket(Utils.port)
                serverSocket?.soTimeout = Utils.timeout
                socket = serverSocket?.accept()
                dataInputStream = DataInputStream(socket?.getInputStream())
                dataOutputStream = DataOutputStream(socket?.getOutputStream())

                uiThread {
                    textView.text = "Connected, Sending file..."
                    val timeStart = System.currentTimeMillis()

                    doAsync {
                        // create file from assets
                        val inputStream = assets.open("mb50.zip")
                        val file = File(filesDir, "file")
                        val byteArray = ByteArray(Utils.byteArraySize)
                        val fileInputStream = FileInputStream(file)
                        var count = fileInputStream.read(byteArray)
                        while(count > 0) {
                            dataOutputStream?.write(byteArray, 0, count)
                            count = fileInputStream.read(byteArray)
                        }
                        inputStream.close()
                        socket?.close()
                        serverSocket?.close()
                        dataInputStream?.close()
                        dataOutputStream?.close()

                        uiThread {
                            val timeEnd = System.currentTimeMillis()
                            val timeDiff = timeEnd - timeStart
                            val timeSec = timeDiff / 1000
                            textView.text = "File sent in ${timeSec} s\n\n${textView.text}"
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
        serverSocket?.close()
        dataInputStream?.close()
        dataOutputStream?.close()
    }

}