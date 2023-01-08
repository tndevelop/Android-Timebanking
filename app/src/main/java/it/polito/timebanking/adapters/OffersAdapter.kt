package it.polito.timebanking.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import it.polito.timebanking.R
import it.polito.timebanking.entities.Offer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.coroutineContext

class OffersAdapter(val data: MutableList<Offer>, context: Context) : RecyclerView.Adapter<OffersAdapter.OfferViewHolder>() {
    private val context: Context

    class OfferViewHolder(v: View, context: Context): RecyclerView.ViewHolder(v) {
        private val months = listOf("Jan","Feb","March","April","May","June","July","Aug","Sept","Oct","Nov","Dec")
        private val amPm = listOf("am", "pm")
        private val title : TextView = v.findViewById(R.id.titleTextView)
        private val date : TextView = v.findViewById(R.id.userView)
        private val location : TextView = v.findViewById(R.id.locationView)
        private val duration : TextView = v.findViewById(R.id.durationView)
        private val nickname : TextView = v.findViewById(R.id.nicknameTextView)
        private val rating : TextView = v.findViewById(R.id.ratingTextView)
        private val imageView : ImageView = v.findViewById(R.id.userImageView)
        private val loadingSpinner : ProgressBar = v.findViewById(R.id.loadingSpinner)
        private val card : CardView = v.findViewById(R.id.cardView)

        val storage = FirebaseStorage.getInstance()
        val gsReference = storage.getReferenceFromUrl("gs://labs-1b5fa.appspot.com/")
        val imageList : MutableMap<String, Bitmap> = mutableMapOf()
        val context = context

        fun bind(ad: Offer, pos: Int) {
            title.text = ad.title
            date.text = getDisplayableDate(ad.dateAndTime)
            location.text = ad.location
            val durationHrs = (ad.duration/60).toInt()
            val durationMins = ad.duration - 60*durationHrs
            duration.text = durationHrs.toString() + "h " + durationMins.toString() + "m"
            nickname.text = ad.nickname
            rating.text = ad.rating.toString()

            // Setting user profile image
            if (imageList.containsKey(ad.id)) {
                // Use downloaded image
                imageView.setImageBitmap(imageList[ad.id])
            } else if (ad.imagePath.isEmpty()) {
                // Set default user image
                loadingSpinner.visibility = View.GONE
                imageView.setImageDrawable(context.getDrawable(R.drawable.avatar))
            } else {
                // Download image
                val imageRef : StorageReference = gsReference.child(ad.imagePath)
                val ONE_MEGABYTE: Long = 1024 * 1024
                imageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener {
                    // Update profile image view
                    val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                    imageList.put(ad.id , bitmap)
                    imageView.setImageBitmap(BitmapFactory.decodeByteArray(it, 0, it.size))
                    loadingSpinner.visibility = View.GONE
                }.addOnFailureListener {
                    // Handle any errors
                    Log.e("Firestore", "Error loading profile image: " + it.toString())
                }
            }

            card.setOnClickListener {

                val bundle = bundleOf("id" to ad.id, "title" to ad.title, "date" to SimpleDateFormat("yyyy-MM-dd HH:mm").format(ad.dateAndTime), "location" to ad.location,
                    "duration" to ad.duration, "description" to ad.description, "service" to  ad.service, "restrictions" to ad.restrictions,
                    "userId" to ad.userId, "nickname" to ad.nickname, "rating" to ad.rating, "imagePath" to ad.imagePath,
                    "status" to ad.status)
                val here= it.findNavController().currentDestination
                val favId= it.findNavController().graph.findNode(R.id.userFavouriteFragment)
                val pendId= it.findNavController().graph.findNode(R.id.userPendingFragment)
                if(here== favId){
                    it.findNavController().navigate(R.id.action_userFavouriteFragment_to_offerDetailsFragment2, bundle)
                }else if(here==pendId){
                    it.findNavController().navigate(R.id.action_userPendingFragment_to_offerDetailsFragment2, bundle)
                }else{
                    it.findNavController().navigate(R.id.action_userOffersFragment_to_offerDetailsFragment2, bundle)
                }
            }
        }

        private fun getDisplayableDate(date: Date):String {
            val minutes : String
            if (date.minutes < 10)
                minutes = "0" + date.minutes
            else
                minutes = date.minutes.toString()
            return "${date.date} ${months.get(date.month)} ${1900+date.year}, ${date.hours % 12}:${minutes}${amPm.get(date.hours/12)}"
            val s: SimpleDateFormat = SimpleDateFormat(R.string.date_time_format.toString())
            return s.format(date)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        val vg = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_offer_item, parent, false)
        return OfferViewHolder(vg, this.context)
    }

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        val ad = data[position]
        holder.bind(ad, data.indexOf(ad) )
    }

    override fun getItemCount(): Int {
        return data.size
    }

    init {
        this.context = context
    }
}