package fmrv.widgetstudio.widget

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import fmrv.widgetstudio.MainActivity
import fmrv.widgetstudio.R
import fmrv.widgetstudio.data.joplin.AuthResult
import fmrv.widgetstudio.data.joplin.OneDriveAuth
import javax.inject.Inject
import kotlinx.coroutines.launch

/** Reçoit la redirection `fmrv.widgetstudio://oauth` à la fin de la connexion Microsoft. Sans interface. */
@AndroidEntryPoint
class OAuthRedirectActivity : ComponentActivity() {

    @Inject lateinit var auth: OneDriveAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val redirect = intent?.data
        if (redirect == null) {
            finish()
            return
        }
        lifecycleScope.launch {
            val result = auth.completeAuthorization(redirect)
            val message = if (result is AuthResult.Success) R.string.joplin_connected else R.string.joplin_connect_failed
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
            if (result is AuthResult.Success) NotesRefreshWorker.enqueueNow(applicationContext)

            startActivity(
                Intent(this@OAuthRedirectActivity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
            finish()
        }
    }
}
