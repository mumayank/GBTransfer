package phone_min_uiux.mu.gbtransfer

import android.app.Activity
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
import android.content.Intent
import android.net.Uri


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


            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(Intent.createChooser(intent, "Select Files"), 100)

            return@setOnClickListener

            startButton.visibility = View.GONE
            progressBar.visibility = View.VISIBLE

            doAsync {
                serverSocket = ServerSocket(Utils.port)
                serverSocket?.soTimeout = Utils.timeout
                socket = serverSocket?.accept()
                dataInputStream = DataInputStream(socket?.getInputStream())
                dataOutputStream = DataOutputStream(socket?.getOutputStream())

                uiThread {
                    textView.text = "Connected, Sending file size.."
                    val timeStart = System.currentTimeMillis()

                    doAsync {
                        // open original stream
                        val inputStream = resources.openRawResource(R.raw.prada)

                        // create file
                        val file = File(cacheDir, "file.${Utils.fileExtension}")
                        val fileOutputStream = FileOutputStream(file)
                        val byteArray = ByteArray(1024)
                        var count = inputStream.read(byteArray)
                        while (count != -1) {
                            fileOutputStream.write(byteArray, 0, count)
                            count = inputStream.read(byteArray)
                        }
                        fileOutputStream.flush()
                        fileOutputStream.close()

                        // send file size
                        val fileLengthString = file.length().toInt().toString()
                        dataOutputStream?.writeUTF(fileLengthString)
                        dataOutputStream?.flush()

                        uiThread {
                            textView.text = "Sent file size = ${fileLengthString} , Sending file..\n\n${textView.text}"

                            // send file
                            doAsync {
                                val byteArray = ByteArray(file.length().toInt())
                                val inputStream = resources.openRawResource(R.raw.prada)
                                var count = inputStream.read(byteArray)
                                while(count > 0) {
                                    dataOutputStream?.write(byteArray, 0, count)
                                    count = inputStream.read(byteArray)
                                }
                                dataOutputStream?.flush()
                                inputStream.close()
                                file.delete()
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

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            if (data?.clipData != null) {
                val count = data.clipData.itemCount
                var currentItem = 0
                while (currentItem < count) {
                    val uri = data.clipData.getItemAt(currentItem).uri
                    print("MOJOJOJO: " + uri)
                    currentItem++
                }
            } else if (data?.data != null) {
                val path = data.data.path
                val uri = Uri.fromFile(File(path))
                print("MOJOJOJO: " + uri)
            }
        }
        /*
        if(requestCode == SELECT_PICTURES) {
        if(resultCode == Activity.RESULT_OK) {
            if(data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                int currentItem = 0;
                while(currentItem < count) {
                    Uri imageUri = data.getClipData().getItemAt(currentItem).getUri();
                    //do something with the image (save it to some directory or whatever you need to do with it here)
                    currentItem = currentItem + 1;
                }
            } else if(data.getData() != null) {
                String imagePath = data.getData().getPath();
                //do something with the image (save it to some directory or whatever you need to do with it here)
            }
        }
    }
         */
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