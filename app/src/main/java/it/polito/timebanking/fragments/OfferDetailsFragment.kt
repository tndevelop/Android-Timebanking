package it.polito.timebanking.fragments

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import it.polito.timebanking.R
import it.polito.timebanking.database.AdvertisementDemo
import it.polito.timebanking.database.DateConverter
import it.polito.timebanking.database.User
import it.polito.timebanking.entities.Offer
import it.polito.timebanking.entities.OfferStatus
import it.polito.timebanking.viewmodels.AdvertisementViewModel
import it.polito.timebanking.viewmodels.OfferViewModel
import it.polito.timebanking.viewmodels.UserViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class OfferDetailsFragment : Fragment(R.layout.fragment_offer_details){

    var idOff: String? = null
    lateinit var offer : Offer
    val userViewModel by activityViewModels<UserViewModel>()
    val advertisementViewModel by activityViewModels<AdvertisementViewModel>()
    lateinit var user: LiveData<User>
    lateinit var editUser: User
    val sdf: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm")
    var isFavourite = false //-->temporary value to understand if the fragment is in user's favourites

    val storage = FirebaseStorage.getInstance()
    val gsReference = storage.getReferenceFromUrl("gs://labs-1b5fa.appspot.com/")

    private val months = listOf("Jan","Feb","March","April","May","June","July","Aug","Sept","Oct","Nov","Dec")
    private val amPm = listOf("am", "pm")
    lateinit var titleTextView: TextView
    lateinit var userImageView: ImageView
    lateinit var userProfileLayout: LinearLayout
    lateinit var nicknameTextView: TextView
    lateinit var ratingTextView: TextView
    lateinit var favouriteButton: Button
    lateinit var serviceTextView: TextView
    lateinit var descriptionTextView: TextView
    lateinit var dateTextView: TextView
    lateinit var durationTextView: TextView
    lateinit var locationTextView: TextView
    lateinit var restrictionsTextView: TextView
    lateinit var timeFromTextView: TextView
    lateinit var chatButton : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_right)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState);

        setHasOptionsMenu(true)
        initView(view)

        idOff = arguments?.getString("id")
        Log.d("test", "part 1: offId= $idOff")
        if (idOff == null) {
            Log.e("OfferDetails","Someone didn't send an id to the offer details fragment")
        }

        //Loading user's data from database
        userViewModel.user.observe(viewLifecycleOwner) {
            if (it == null) {
                Log.e("OfferDetails","Error when observing the userViewModel, it==null")
            }

            editUser= User(it.id, it.nickname, it.fullName, it.description, it.email, it.location,
                            it.rating, it.skillsArray, it.imagePath, it.favourites)

            if(!it.favourites.isEmpty()){
                var userFav= it.favourites.split(",")
                for (off in userFav){
                    if(off == idOff) {isFavourite= true }
                }

                if(isFavourite){
                    favouriteButton.text= "Remove from Favourites"
                }else{
                    favouriteButton.text= "Add to favourites"
                }
            }
        }

        offer = Offer().also {  it.id = idOff!!;
            it.dateAndTime = sdf.parse(arguments?.getString("date")!!);
            it.description = arguments?.getString("description")!!; it.duration = arguments?.getLong("duration")!!;
            it.location = arguments?.getString("location")!!; it.restrictions = arguments?.getString("restrictions")!!;
            it.service = arguments?.getString("service")!!; it.title = arguments?.getString("title")!!;
            it.userId = arguments?.getString("userId")!!; it.rating = arguments?.getFloat("rating")!!;
            it.nickname = arguments?.getString("nickname")!!; it.imagePath = arguments?.getString("imagePath")!!;
            it.status = (arguments?.get("status") as OfferStatus?)!!}
        titleTextView.text = offer.title

        userImageView= view.findViewById(R.id.userImageView)
        if (offer.imagePath.isEmpty()) {
            // Set default user image
            userImageView.setImageDrawable(requireContext().getDrawable(R.drawable.avatar))

        }else {
            val imageRef: StorageReference = gsReference.child(offer.imagePath)
            val ONE_MEGABYTE: Long = 1024 * 1024
            imageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener {
                // Update profile image view
                userImageView.setImageBitmap(BitmapFactory.decodeByteArray(it, 0, it.size))
            }.addOnFailureListener {
                // Handle any errors
                Log.e("Firestore", "Error loading profile image: " + it.toString())
            }
        }

        nicknameTextView.text= offer.nickname
        ratingTextView.text= offer.rating.toString()
        serviceTextView.text = offer.service
        descriptionTextView.text = offer.description

        val durationHrs = (offer.duration / 60).toInt()
        val durationMins = offer.duration - 60 * durationHrs

        durationTextView.text= durationHrs.toString() + "h " + durationMins.toString() + "m"
        locationTextView.text= offer.location
        restrictionsTextView.text= if(offer.restrictions.length > 0) offer.restrictions else "No restrictions."

        var myDate= getDisplayableDate(offer.dateAndTime)
        dateTextView.text = myDate.substring(0, myDate.length-8)
        timeFromTextView.text= myDate.substring(myDate.length - 7, myDate.length )

        userProfileLayout.setOnClickListener{
            val bundle = bundleOf("id" to offer!!.userId)
            it.findNavController().navigate(R.id.action_offerDetailsFragment2_to_offerProfileFragment, bundle)
        }

        favouriteButton.setOnClickListener{

            if(isFavourite){
                isFavourite= false
                favouriteButton.text= "Add to Favourites"
                //remove this offer from the list of the user's favourites
                if(editUser.favourites.contains("$idOff,")){
                    editUser.favourites= editUser.favourites.replace("$idOff,", "")
                }

            }else{
                isFavourite= true
                favouriteButton.text= "Remove from Favourites"
                //add this offer to the list of the user's favourites
                editUser.favourites= editUser.favourites + "$idOff,"
            }


            Firebase.auth.currentUser?.let {
                userViewModel.update(it.uid,
                    ObjectMapper().convertValue(editUser, HashMap::class.java) as HashMap<String, Any>,
                    {Log.d("test", "success") },
                    { e: Exception -> Log.d("test", e.message.toString())}
                )
            }

        }

        // Get current user
        val auth = Firebase.auth
        val thisUser = auth.currentUser?.uid!!

        // Book now button only enabled if offer status is open, and only for people that are not the offer owner
        if (!(offer.status == OfferStatus.OPEN || offer.status == OfferStatus.PENDING) || offer.userId == thisUser) {
            chatButton.visibility = View.GONE
        } else {
            chatButton.setOnClickListener{
                // Change status of the offer to PENDING to indicate it is in negotiations
                updateStatus(offer.id, OfferStatus.PENDING)

                //Automatically add this offer to the list of the user's favourites
                isFavourite= true
                editUser.favourites= editUser.favourites + "$idOff,"
                Firebase.auth.currentUser?.let {
                    userViewModel.update(it.uid,
                        ObjectMapper().convertValue(editUser, HashMap::class.java) as HashMap<String, Any>,
                        {Log.d("test", "success") },
                        { e: Exception -> Log.d("test", e.message.toString())}
                    )
                }

                val bundle = bundleOf("otherUserId" to offer.userId, "offerUserId" to offer.userId,
                    "offerId" to offer.id, "offerTitle" to offer.title, "userNickName" to offer.nickname,
                    "cost" to offer.duration, "bookerUserId" to thisUser, "offerStatus" to offer.status)
                findNavController().navigate(R.id.action_offerDetailsFragment2_to_chatFragment, bundle)
            }
        }


    }


    private fun initView(view:View){
        titleTextView= view.findViewById(R.id.titleTextView)
        userImageView= view.findViewById(R.id.userImageView)
        userProfileLayout= view.findViewById(R.id.userProfileLayout)
        nicknameTextView= view.findViewById(R.id.nicknameTextView)
        ratingTextView= view.findViewById(R.id.ratingTextView)
        favouriteButton= view.findViewById(R.id.favouriteButton)
        serviceTextView= view.findViewById(R.id.OfferedServiceTextView2)
        descriptionTextView= view.findViewById(R.id.descriptionTextView2)
        dateTextView= view.findViewById(R.id.dateTextView2)
        durationTextView= view.findViewById(R.id.durationTextView2)
        locationTextView= view.findViewById(R.id.locationTextView2)
        restrictionsTextView= view.findViewById(R.id.restrictionTextView2)
        timeFromTextView= view.findViewById(R.id.timefromTextView)
        chatButton = view.findViewById(R.id.start_chat_button)
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

    fun updateStatus(adId: String, status: OfferStatus) {
        advertisementViewModel.updateStatus(adId, status, null,
            {
                Log.d("timebanking", "Status Updated")
            },
            { e: Exception ->
                Log.d("timebanking", "Error while updating offer $adId status: " + e.message.toString())
            }
        )
    }

}
