package it.polito.timebanking


import android.os.Parcelable
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.gson.Gson


class MessagingService : FirebaseMessagingService() {

    companion object Oauth {
        lateinit var oauthToken: String

        private const val projectId = 1
        const val host = "fcm.googleapis.com"
        const val path = "/v1/projects/$projectId/messages:send"
    }

}
