package phone_min_uiux.mu.gbtransfer

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
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
import android.content.Intent
import android.net.Uri
import android.support.v4.app.ShareCompat
import android.support.v4.content.FileProvider
import android.widget.TextView
import android.media.MediaScannerConnection








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
                    textView.text = "Connected, Receiving file size..."
                    val timeStart = System.currentTimeMillis()
                    // receive file here

                    doAsync {
                        val fileSize: String = dataInputStream?.readUTF() ?: "1024"

                        uiThread {
                            textView.text = "File size = ${fileSize} , Receiving file...\n\n${textView.text}"

                            doAsync {

                                val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                path.mkdirs()
                                val file = File(path, "${System.currentTimeMillis()}.${Utils.fileExtension}")
                                val fileOutputStream = FileOutputStream(file)
                                val byteArray = ByteArray(fileSize.toInt())
                                var count = dataInputStream?.read(byteArray) ?: 0
                                while (count > 0) {
                                    fileOutputStream.write(byteArray, 0, count)
                                    count = dataInputStream?.read(byteArray) ?: 0
                                }

                                fileOutputStream.flush()
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

                                    openFileViaIntent(file)

                                    openFileButton.visibility = View.VISIBLE
                                    openFileButton.setOnClickListener {
                                        openFileViaIntent(file)
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openFileViaIntent(file: File) {
        val intent = Intent()
        intent.action = android.content.Intent.ACTION_VIEW
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        val uri = Uri.parse(file.canonicalPath)
        intent.setDataAndType(uri, Utils.fileType)
        startActivity(intent)
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