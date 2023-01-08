package it.polito.timebanking.adapters


import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import it.polito.timebanking.R
import it.polito.timebanking.database.AdvertisementDemo
import it.polito.timebanking.viewmodels.AdvertisementViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule
import kotlin.time.ExperimentalTime

class AdvertisementAdapter(val data: MutableList<AdvertisementDemo>, val vm : AdvertisementViewModel) : RecyclerView.Adapter<AdvertisementAdapter.AdvertisementViewHolder>() {

    class AdvertisementViewHolder(v: View): RecyclerView.ViewHolder(v) {
        private val months = listOf("Jan","Feb","March","April","May","June","July","Aug","Sept","Oct","Nov","Dec")
        private val amPm = listOf("am", "pm")
        private val colors = listOf("#BBDEFB", "#CDDC39")
        private val title : TextView = v.findViewById(R.id.titleTextView)
        private val date : TextView = v.findViewById(R.id.userView)
        private val location : TextView = v.findViewById(R.id.locationView)
        private val duration : TextView = v.findViewById(R.id.durationView)
        private val edit : ImageButton = v.findViewById(R.id.editButton)
        private val delete : ImageButton = v.findViewById(R.id.deleteButton)
        private val card : CardView = v.findViewById(R.id.cardView)
        val sdf: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm")


        fun bind(ad: AdvertisementDemo, pos: Int, action : (view:View) -> Unit){
            title.text = ad.title
            date.text = getDisplayableDate(sdf.parse(ad.dateAndTime))
            location.text = ad.location
            val durationHrs = (ad.duration/60).toInt()
            val durationMins = ad.duration - 60*durationHrs
            duration.text = durationHrs.toString() + "h " + durationMins.toString() + "m"
//            card.setCardBackgroundColor(Color.parseColor(colors.get(pos%2)))
            edit.setOnClickListener{
                val bundle = bundleOf("id" to ad.id, "title" to ad.title, "date" to ad.dateAndTime, "location" to ad.location,
                    "duration" to ad.duration, "description" to ad.description, "service" to  ad.service, "restrictions" to ad.restrictions,
                    "userId" to ad.userId)
                it.findNavController().navigate(R.id.action_timeSlotListFragment_to_timeSlotEditFragment, bundle)
            }
            card.setOnClickListener {
                val bundle = bundleOf("id" to ad.id, "title" to ad.title, "date" to ad.dateAndTime, "location" to ad.location,
                    "duration" to ad.duration, "description" to ad.description, "service" to  ad.service, "restrictions" to ad.restrictions,
                    "userId" to ad.userId)
                it.findNavController().navigate(R.id.action_timeSlotListFragment_to_timeSlotDetailsFragment, bundle)
            }
            delete.setOnClickListener(action)
        }

        private fun getDisplayableDate(date:Date):String {
            val minutes : String
            if (date.minutes < 10)
                minutes = "0" + date.minutes
            else
                minutes = date.minutes.toString()

            return "${date.date} ${months.get(date.month)} ${1900+date.year}, ${date.hours % 12}:${minutes}${amPm.get(date.hours/12)}"
            val s: SimpleDateFormat = SimpleDateFormat(R.string.date_time_format.toString())
            return s.format(date)
        }

        fun unbind() {
            edit.setOnClickListener(null)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdvertisementViewHolder {
        val vg = LayoutInflater.from(parent.context)
            .inflate(R.layout.advertisment_item, parent, false)
        return AdvertisementViewHolder(vg)
    }

    override fun onBindViewHolder(holder: AdvertisementViewHolder, position: Int) {
        val ad = data[position]
        holder.bind(ad, data.indexOf(ad) ){
            val pos = data.indexOf(ad)
            if(pos != -1){
                data.removeAt(pos)
                notifyItemRemoved(pos)
                Timer().schedule(300) {
                    vm.delete(ad.id,
                        {
                            Log.d("timebanking", "success")
                        },
                        { e: Exception ->
                            Log.d("timebanking", e.message.toString())
                        }
                    )
                }

            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

}