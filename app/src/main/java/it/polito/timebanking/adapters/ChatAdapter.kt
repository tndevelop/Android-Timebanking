package it.polito.timebanking.adapters
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import it.polito.timebanking.Message
import it.polito.timebanking.R


class ChatAdapter(messageList: List<Message>, activity: Activity) :
    RecyclerView.Adapter<ChatAdapter.MyViewHolder>() {
    private val messageList: List<Message>
    private val activity: Activity
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view: View =
            LayoutInflater.from(activity).inflate(R.layout.adapter_message_one, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val message: String = messageList[position].message
        val isReceived: Boolean = messageList[position].isReceived
        if (isReceived) {
            holder.messageReceive.visibility = View.VISIBLE
            holder.messageSend.visibility = View.GONE
            holder.messageReceive.text = message
        } else {
            holder.messageSend.visibility = View.VISIBLE
            holder.messageReceive.visibility = View.GONE
            holder.messageSend.text = message
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
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
        this.messageList = messageList
        this.activity = activity
    }
}