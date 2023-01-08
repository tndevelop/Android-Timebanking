package it.polito.timebanking

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.polito.timebanking.database.User
import it.polito.timebanking.viewmodels.UserViewModel
import java.util.*
import kotlin.concurrent.schedule

class WelcomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    val userViewModel by viewModels<UserViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_token))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = Firebase.auth

        findViewById<SignInButton>(R.id.google_button).setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

    }

    override fun onStart() {
        super.onStart()
        signIn()

    }
    fun signOut(){
        Firebase.auth.signOut()
    }

    private fun loadToken(account: GoogleSignInAccount) {
        val am: AccountManager = AccountManager.get(this)
        val options = Bundle()

        am.getAuthToken(
            account.account,                     // Account retrieved using getAccountsByType()
            "oauth2: https://www.googleapis.com/auth/firebase.messaging",            // Auth scope
            options,                        // Authenticator-specific options
            this,                           // Your activity
            { MessagingService.oauthToken = it.result.getString(AccountManager.KEY_AUTHTOKEN)!! }, // Callback called when a token is successfully acquired
            Handler (Looper.getMainLooper()){
                Log.d("GoogleSignInError", "ERROR")
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.oauth_token_error), Snackbar.LENGTH_LONG).show()
                true
            }              // Callback called if an error occurs
        )
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    goToMainActivity()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.credentials_error), Snackbar.LENGTH_LONG).show()
                }
            }
    }

    private fun signIn() {
        if(auth.currentUser!=null) {
            GoogleSignIn.getLastSignedInAccount(this).also { loadToken(it!!) }
            goToMainActivity()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN){
            if(resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)!!

                    loadToken(account)

                    firebaseAuthWithGoogle(account.idToken!!)

                    val loggedInUser = auth.currentUser
                    Timer().schedule(300) {

                        loggedInUser?.let {

                            val user = User()
                            user.email = it.email!!
                            user.id = it.uid
                            user.nickname = it.email!!
                            user.description = "Hi there! I'm using TimeBanking"

                            userViewModel.save(it.uid,
                                ObjectMapper().convertValue(
                                    user,
                                    HashMap::class.java
                                ) as HashMap<String, Any>,
                                {
                                    Log.d("t", "success")
                                },
                                { e: Exception ->
                                    Log.d("t", e.message.toString())
                                }
                            )
                        }
                    }
                } catch (e: ApiException) {
                    // Google Sign In failed, update UI appropriately
                    Log.w("GoogleSignInError", "Google sign in failed", e)
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.firebase_login_error), Snackbar.LENGTH_LONG).show()
                }
            }else {
                if (data != null && data.extras!=null) {
                    val e = data.extras!!;
                    Log.d("firestore", e.keySet().size.toString())
                    e.keySet().iterator().forEach{ Log.d("firestore", "$it -> ${e.get(it).toString()}")}
                }
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.firebase_login_error), Snackbar.LENGTH_LONG).show()
            }}
    }

    companion object {
        private const val TAG = "MAD-group-29"
        private const val RC_SIGN_IN = 9001
    }

    private fun goToMainActivity(){
        findViewById<ConstraintLayout>(R.id.loading).visibility = View.VISIBLE
        startActivity(Intent(this, MainActivity::class.java))
        finishAffinity()
    }

}