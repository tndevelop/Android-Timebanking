package it.polito.timebanking.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import it.polito.timebanking.R
import it.polito.timebanking.adapters.ChatsAdapter
import it.polito.timebanking.entities.Chat
import it.polito.timebanking.viewmodels.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

class ChatListFragment : Fragment(R.layout.fragment_chat_list) {

    val chatsViewModel by activityViewModels<ChatsViewModel>()
    val offerModel : OfferViewModel by activityViewModels<OfferViewModel>()
    val unreadViewModel : UnreadViewModel by activityViewModels<UnreadViewModel>()
    @OptIn(ExperimentalCoroutinesApi::class)
    private lateinit var auth: FirebaseAuth
    var chatList = mutableListOf<Chat>()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = getView()?.findViewById<RecyclerView>(R.id.recView)
        rv?.layoutManager = LinearLayoutManager(getView()?.context)

        val noItemsMsg = getView()?.findViewById<TextView>(R.id.noItemsMessage)

        chatsViewModel.chatties.observe(viewLifecycleOwner) {
            //Set up the list of advertisements
            if (it == null || it.isEmpty()) {
                noItemsMsg?.visibility = View.VISIBLE
            } else {
                noItemsMsg?.visibility = View.GONE
                val chats = it.distinctBy {it.offerId + it.userIds.sorted()};
                // Merge flows from firebase
                chatList.addAll(chats)
                val chatL = chatList.distinctBy {it.offerId + it.userIds.sorted()};
                val adapter = ChatsAdapter(chatL, offerModel, unreadViewModel, viewLifecycleOwner)
                rv?.adapter = adapter
            }
        }

    }

}