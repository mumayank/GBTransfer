package phone_min_uiux.mu.gbtransfer

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serverButton.setOnClickListener { startActivity(Intent(this, ServerActivity::class.java)) }
        clientButton.setOnClickListener { startActivity(Intent(this, ClientActivity::class.java)) }
    }
}
