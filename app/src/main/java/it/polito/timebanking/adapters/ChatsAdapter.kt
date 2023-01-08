package it.polito.timebanking.adapters

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.polito.timebanking.entities.Chat
import it.polito.timebanking.R
import it.polito.timebanking.entities.Offer
import it.polito.timebanking.firebase.ChatsFirebaseService
import it.polito.timebanking.firebase.MessageFirebaseService
import it.polito.timebanking.viewmodels.MessageViewModel
import it.polito.timebanking.viewmodels.OfferViewModel
import it.polito.timebanking.viewmodels.SomeViewModelFactory
import it.polito.timebanking.viewmodels.UnreadViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlin.time.ExperimentalTime

class ChatsAdapter(chatList: List<Chat>, offerViewModel: OfferViewModel, unreadViewModel: UnreadViewModel, lifecycle: LifecycleOwner) :
    RecyclerView.Adapter<ChatsAdapter.ChatsViewHolder>() {
    private val chatList: List<Chat>
    private val offerViewModel: OfferViewModel
    private val unreadViewModel: UnreadViewModel
    private val lifecycle: LifecycleOwner

    class ChatsViewHolder(v: View, offerViewModel: OfferViewModel, unreadViewModel: UnreadViewModel, lifecycle: LifecycleOwner): RecyclerView.ViewHolder(v) {
//         private val colors = listOf("#BBDEFB", "#CDDC39")
        private val userTV : TextView = v.findViewById(R.id.userView)
        private val offerTitle : TextView = v.findViewById(R.id.titleTextView)
        private val card : CardView = v.findViewById(R.id.cardView)
        private val unreadMessageView : ImageView = v.findViewById(R.id.unreadMessageView)
        private val auth: FirebaseAuth = Firebase.auth
        private val vm: OfferViewModel = offerViewModel
        private val unreadvm: UnreadViewModel = unreadViewModel
        private val lifecycle: LifecycleOwner = lifecycle

        fun bind(chat: Chat, pos: Int){
            val offer = vm.getOfferById(chat.offerId)
            val otherUserId = chat.userIds.filter{ it != auth.currentUser?.uid}[0]
            val bookerUserId = chat.userIds.filter{ it != offer?.userId}[0]
            val myAd = auth.currentUser?.uid == offer?.userId
            val otherUser = vm.getUserById(otherUserId)
            userTV.text = otherUser?.nickname
            if (myAd) {
                offerTitle.text = "Request: ${offer?.title}"
                offerTitle.setTextColor(ContextCompat.getColor(offerTitle.context, R.color.blue_200))
            } else {
                offerTitle.text = "Booking: ${offer?.title}"
                offerTitle.setTextColor(ContextCompat.getColor(offerTitle.context, R.color.black))
                unreadMessageView.setColorFilter(ContextCompat.getColor(offerTitle.context, R.color.black))
            }

            // Check if message is unread
            unreadvm.chats.observe(lifecycle) {
                Log.d("test", "Chats ${it.map { "${it.unreadByUser2} + ${it.offerId}" }}")
                val unreadChat = it.find { it.offerId == offer?.id && it.unreadByUser2 == true }
                Log.d("test", "Message is unread: ${unreadChat?.unreadByUser2}")
                if (unreadChat != null) {
                    unreadMessageView.visibility = View.VISIBLE
                } else {
                    unreadMessageView.visibility = View.GONE
                }
            }

//            card.setCardBackgroundColor(Color.parseColor(colors.get(pos%2)))
                card.setOnClickListener {
                    MessageFirebaseService.updateReadMessages(otherUserId, auth.currentUser!!.uid, offer!!.id)
                    val bundle = bundleOf("otherUserId" to otherUserId, "offerUserId" to offer?.userId,
                        "offerId" to chat.offerId, "offerTitle" to offer?.title, "userNickName" to otherUser?.nickname,
                        "cost" to offer?.duration, "bookerUserId" to bookerUserId, "offerStatus" to offer?.status)
                    it.findNavController().navigate(R.id.action_chatListFragment_to_chatFragment, bundle)
                }

        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatsAdapter.ChatsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_item, parent, false)
        return ChatsAdapter.ChatsViewHolder(view, offerViewModel, unreadViewModel, lifecycle)
    }

    override fun onBindViewHolder(holder: ChatsViewHolder, position: Int) {
        val chat = chatList[position]
        holder.bind(chat, chatList.indexOf(chat) )
    }

    override fun getItemCount(): Int {
        return chatList.size
    }

    fun getOffer(id: String): Offer? {
        return offerViewModel.getOfferById(id)
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var messageSend: TextView
        var messageReceive: TextView

        init {
            messageSend = itemView.findViewById(R.id.message_send)
            messageReceive = itemView.findViewById(R.id.message_receive)
        }
    }

    init {
        this.chatList = chatList
        this.offerViewModel = offerViewModel
        this.unreadViewModel = unreadViewModel
        this.lifecycle = lifecycle
    }


    }
