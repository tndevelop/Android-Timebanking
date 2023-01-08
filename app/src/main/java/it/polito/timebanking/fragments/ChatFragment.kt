package it.polito.timebanking.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.*
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionInflater
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldValue.serverTimestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.ktx.Firebase
import it.polito.timebanking.MainActivity
import it.polito.timebanking.Message
import it.polito.timebanking.R
import it.polito.timebanking.adapters.ChatAdapter
import it.polito.timebanking.entities.Chat
import it.polito.timebanking.entities.OfferStatus
import it.polito.timebanking.entities.Rating
import it.polito.timebanking.firebase.AdvertisementFirebaseService
import it.polito.timebanking.firebase.RatingFirebaseService
import it.polito.timebanking.firebase.UserFirebaseService
import it.polito.timebanking.viewmodels.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class ChatFragment : Fragment(R.layout.fragment_chat) {

    var chatView: RecyclerView? = null
    var chatAdapter: ChatAdapter? = null
    var messageList: MutableList<Message> = mutableListOf()
    var editMessage: EditText? = null
    var btnSend: ImageButton? = null
    lateinit var btnAccept: Button
    lateinit var btnReject: Button
    lateinit var statusTextView: TextView
    lateinit var userTV : TextView
    lateinit var offerTV : TextView
    val messageViewModel : MessageViewModel by viewModels { SomeViewModelFactory(this, otherUserId, offerId) }
    val advertisementViewModel by activityViewModels<AdvertisementViewModel>()
    val userViewModel by activityViewModels<UserViewModel>()
    val unreadViewModel : UnreadViewModel by activityViewModels<UnreadViewModel>()
    var otherUserId : String = ""
    var offerId : String = ""
    var otherUserNickName: String = ""
    var offerStatus: OfferStatus? = null
    var oldRating: Rating? = null
    var thisUser: String = ""
    var canBeRated: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = TransitionInflater.from(requireContext())

        otherUserId = arguments?.getString("otherUserId").orEmpty()
        offerId = arguments?.getString("offerId")!!
        offerStatus = arguments?.get("offerStatus") as OfferStatus?
        enterTransition = inflater.inflateTransition(R.transition.slide_right)

        RatingFirebaseService.getRating(offerId) {
            if (it != null) {
                oldRating = it
                if (!(oldRating!!.ratingUserId.equals(Firebase.auth.currentUser?.uid!!)))
                    canBeRated = false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_rate, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_rate -> {
                openRatingDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStop() {
        super.onStop()
        unreadViewModel.reload()
    }

    fun openRatingDialog() {
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .create()
        val view = layoutInflater.inflate(R.layout.dialog_custom_rating,null)

        val ratingBar = view.findViewById<RatingBar>(R.id.ratingBar_indialog)
        val usernameTextView = view.findViewById<TextView>(R.id.usernameTextView)
        val commentEditText = view.findViewById<EditText>(R.id.commentEditText)
        val dismissButton = view.findViewById<Button>(R.id.dismiss_button)
        val rateButton = view.findViewById<Button>(R.id.rate_button)
        usernameTextView.text = "${usernameTextView.text} $otherUserNickName"

        if(oldRating != null) {
            ratingBar.rating = oldRating!!.rate
            commentEditText.setText(oldRating!!.comment)
        }

        builder.setView(view)
        dismissButton.setOnClickListener {
            builder.dismiss()
        }
        rateButton.setOnClickListener {
            if(canBeRated) {
                ratingBar.isEnabled = true
                commentEditText.isEnabled = true
                var rating = Rating(
                    ratingBar.rating,
                    Firebase.auth.currentUser?.uid!!,
                    otherUserId,
                    offerId,
                    commentEditText.text.toString()
                )

                if (oldRating != null) {
                    rating.id = oldRating!!.id
                    RatingFirebaseService.updateRating(oldRating!!.id,
                        ObjectMapper().convertValue(
                            rating,
                            java.util.HashMap::class.java
                        ) as java.util.HashMap<String, Any>,
                        {
                            Log.d("Rating", "Rating saved")
                            oldRating = rating
                            ratingBar.rating = rating.rate
                            commentEditText.setText(rating.comment)
                            builder.dismiss()
                        },
                        { e: Exception ->
                            Log.d(
                                "Rating",
                                "Error while updating offer $offerId status: " + e.message.toString()
                            )
                            builder.dismiss()
                        }
                    )
                } else {
                    RatingFirebaseService.saveRating(ObjectMapper().convertValue(
                        rating,
                        java.util.HashMap::class.java
                    ) as java.util.HashMap<String, Any>,
                        {
                            Log.d("Rating", "Rating saved")
                            builder.dismiss()
                        },
                        { e: Exception ->
                            Log.d(
                                "Rating",
                                "Error while updating offer $offerId status: " + e.message.toString()
                            )
                            builder.dismiss()
                        }
                    )
                }
            }
            else {
                ratingBar.isEnabled = false
                commentEditText.isEnabled = false
            }
        }
        builder.setCanceledOnTouchOutside(false)
        builder.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)

        val auth = Firebase.auth
        thisUser = auth.currentUser?.uid!!
        val offerUserId = arguments?.getString("offerUserId")!!
        val bookerUserId = arguments?.getString("bookerUserId")!!
        val offerTitle = arguments?.getString("offerTitle")
        otherUserNickName = arguments?.getString("userNickName")!!
        val cost = arguments?.getLong("cost")!!.toInt()


        messageViewModel.messages.observe(viewLifecycleOwner) {
            messageList.addAll(it)
            val messages = messageList.distinctBy { it.timestamp }.sortedBy { it.timestamp }
            chatAdapter = ChatAdapter(messages, this.requireActivity())
            chatView!!.adapter = chatAdapter
            Objects.requireNonNull(chatView!!.adapter).notifyDataSetChanged()
            Objects.requireNonNull(chatView!!.layoutManager)
                ?.scrollToPosition(messages.size - 1)
        }


        chatView = view.findViewById(R.id.chatView)
        editMessage = view.findViewById(R.id.editMessage)
        btnSend = view.findViewById(R.id.btnSend)
        btnAccept = view.findViewById(R.id.button_accept)
        btnReject = view.findViewById(R.id.button_reject)
        statusTextView = view.findViewById(R.id.text_offer_status)
        userTV = view.findViewById(R.id.userTV)
        offerTV = view.findViewById(R.id.offerTV)

        offerTV.text = getString(R.string.offer) + offerTitle
        userTV.text = getString(R.string.user) + otherUserNickName


        btnSend!!.setOnClickListener {
            val message = editMessage!!.text.toString()
            if (!message.isEmpty()) {
                //messageList.add(Message(message, false))
                editMessage!!.setText("")

                messageViewModel.save(
                    hashMapOf("text" to message, "sender" to thisUser, "receiver" to otherUserId,
                        "timestamp" to Timestamp.now(), "offerId" to offerId!!,
                        "unreadByUser1" to false, "unreadByUser2" to true),
                         { Log.d("messageFirebase", "success")},
                        {Log.d("messageFirebase", "failure")}
                )
                Objects.requireNonNull(chatView!!.adapter).notifyDataSetChanged()
                Objects.requireNonNull(chatView!!.layoutManager)
                    ?.scrollToPosition(messageList.size - 1)
            } else {
                Toast.makeText(this.context, "Please enter text!", Toast.LENGTH_SHORT).show()
            }
        }

        // Show accept/reject buttons for offer owner, else show status
        if (thisUser == offerUserId) {
            if(offerStatus == OfferStatus.PENDING) {
                btnReject.visibility = View.VISIBLE
                btnAccept.visibility = View.VISIBLE
            }else{
                btnReject.visibility = View.GONE
                btnAccept.visibility = View.GONE
            }
            statusTextView.visibility = View.GONE
            // Change offer status depending on decision of the owner
            btnAccept.setOnClickListener {
                // Change offer status
                updateStatusAndAssignOffer(offerId!!, thisUser,OfferStatus.ACCEPTED, bookerUserId)
                transferCredit(bookerUserId!! , thisUser,  cost )
                // Show outcome to user
                btnReject.visibility = View.GONE
                btnAccept.visibility = View.GONE
                statusTextView.visibility = View.VISIBLE
                statusTextView.text = getString(R.string.accepted)
            }
            btnReject.setOnClickListener {
                // Change offer status
                updateStatusAndAssignOffer(offerId!!, thisUser, OfferStatus.OPEN, null)
                // Show outcome to user
                btnReject.visibility = View.GONE
                btnAccept.visibility = View.GONE
                statusTextView.visibility = View.VISIBLE
                statusTextView.text = getString(R.string.rejected)
            }
        } else {
            btnReject.visibility = View.GONE
            btnAccept.visibility = View.GONE
            statusTextView.visibility = View.VISIBLE

        }

    }

    // Update status of timeslot
    fun updateStatusAndAssignOffer(adId: String, currUser:String, status: OfferStatus, assignedUser: String?) {
        advertisementViewModel.updateStatus(adId, status, assignedUser,
            {
                Log.d("timebanking", "Status Updated")
            },
            { e: Exception ->
                Log.d("timebanking", "Error while updating offer $adId status: " + e.message.toString())
            })


        if(assignedUser!=null) {
            //Update current user's acceptedList
            userViewModel.updateAccepted(currUser, adId,
                {
                    Log.d("timebanking", "User Updated")
                },
                { e: Exception ->
                    Log.d("timebanking", "Error while updating user $currUser status: " + e.message.toString())
                })

            //Update booking user's assignedList
            userViewModel.updateAssigned(assignedUser!!, adId,
                {
                    Log.d("timebanking", "User Updated")
                },
                { e: Exception ->
                    Log.d("timebanking", "Error while updating user $assignedUser status: " + e.message.toString())
                })

        }



    }

    // Create the credit transfer transaction
    fun transferCredit(senderId: String, receiverId: String, creditToTransfer: Int) {
        lifecycleScope.launch {
            UserFirebaseService.creditTransfer(senderId, receiverId, creditToTransfer, {
                Log.d("Transfer", "Credit of $creditToTransfer transfered from user " +
                        "$senderId to user $receiverId")
            }, {
                Log.d("Transfer", "Transfer failed with exception $it")
                if (it.message == "Insufficient credit")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "User has insufficient credit, aborting", Toast.LENGTH_SHORT).show()
                }
                updateStatusAndAssignOffer(offerId!!, thisUser ,OfferStatus.OPEN, null)
            })
        }
    }
}