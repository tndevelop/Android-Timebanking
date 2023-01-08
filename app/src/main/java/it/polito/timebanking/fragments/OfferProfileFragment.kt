package it.polito.timebanking.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import it.polito.timebanking.R
import it.polito.timebanking.adapters.RatingsAdapter
import it.polito.timebanking.database.User
import it.polito.timebanking.entities.Rating
import it.polito.timebanking.firebase.RatingFirebaseService
import it.polito.timebanking.viewmodels.PublicUserViewModel
import it.polito.timebanking.viewmodels.UserViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.json.JSONObject
import java.time.LocalDate

class OfferProfileFragment: Fragment(R.layout.fragment_show_profile) {

    val publicUserViewModel by activityViewModels<PublicUserViewModel>()
    lateinit var user: LiveData<User>
    lateinit var nicknameTextView: TextView
    lateinit var ratingTextView: TextView
    lateinit var descriptionTextView: TextView
    lateinit var emailTextView2: TextView
    lateinit var fullnameTextView2: TextView
    lateinit var locationTextView2: TextView
    lateinit var timeCreditTextView: TextView
    lateinit var profileImageView: ImageView
    lateinit var loadingSpinner: ProgressBar
    lateinit var ratingBar: RatingBar
    lateinit var skillsGridView: GridView
//    lateinit var extraview: View
//    lateinit var reviewTextView: TextView
    lateinit var reviewsGridView: GridView
    lateinit var emptyrTextView: TextView
//    lateinit var reviewsCardView: CardView
    lateinit var skillsAdapter: ArrayAdapter<String>
    lateinit var reviewsAdapter: RatingsAdapter
    lateinit var skillsArray: ArrayList<String>
    var reviewsArray: ArrayList<Rating> = arrayListOf()
    lateinit var ppImage: String
    var userID: String? = null


    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        init(view)
        reviewsAdapter = RatingsAdapter(requireContext(), reviewsArray)

        userID = arguments?.getString("id")
        if (userID == null) {
            Log.e("Offer Profile","Someone didn't send an id to the offer profile fragment")
        }
        publicUserViewModel.loadUser(userID!!)

        // Set up loading spinner
        loadingSpinner = view.findViewById(R.id.loadingSpinner)

        //Loading user's data from database
        publicUserViewModel.user.observe(viewLifecycleOwner) {
            if (it == null) {
                // loading spinner is showing
                loadingSpinner.visibility = View.VISIBLE
            } else {
                // Hide loading spinner and set user details
                loadingSpinner.visibility = View.GONE
                setUserDetails(it)
            }
//            skills
            skillsAdapter = ArrayAdapter(requireContext(), R.layout.skill_item, R.id.textview, skillsArray)
            skillsGridView.adapter = skillsAdapter
            skillsGridView.emptyView = view.findViewById(R.id.emptyTextView)
            skillsGridView.stopNestedScroll()
            skillsGridView.layoutParams.height = (40 * ((skillsArray.size + 10) / 2) as Int)

//            reviews
            reviewsAdapter.ratings = reviewsArray
            reviewsGridView.adapter = reviewsAdapter
            reviewsGridView.emptyView = emptyrTextView
//            reviewsGridView.stopNestedScroll()
            reviewsGridView.layoutParams.height = (100 * ((skillsArray.size + 15) / 2) as Int)
        }
    }

    private fun init(view: View) {
        nicknameTextView = view.findViewById(R.id.nicknameTextView)
        ratingTextView = view.findViewById(R.id.ratingTextView)
        descriptionTextView = view.findViewById(R.id.descriptionTextView)
        emailTextView2 = view.findViewById(R.id.emailTextView2)
        fullnameTextView2 = view.findViewById(R.id.fullnameTextView2)
        locationTextView2 = view.findViewById(R.id.locationTextView2)
        ratingBar = view.findViewById(R.id.ratingBar)
        skillsGridView = view.findViewById(R.id.skillsGridView)
        profileImageView = view.findViewById(R.id.avatarImageView)
        timeCreditTextView = view.findViewById(R.id.time_credit_value_text_view)
        reviewsGridView = view.findViewById(R.id.reviewsGridView)
        emptyrTextView = view.findViewById(R.id.emptyrTextView)
    }

    private fun setUserDetails(it: User) {
        nicknameTextView.text = it.nickname
        ratingBar.rating = it.rating
        ratingTextView.text = "${it.rating} stars"
        descriptionTextView.text = it.description
        emailTextView2.text = it.email
        fullnameTextView2.text = it.fullName
        locationTextView2.text = it.location
        val creditHours = it.credit / 60
        val creditMins = it.credit - creditHours*60
        timeCreditTextView.text = String.format("%02d:%02d",creditHours, creditMins)

//        skills array
        if(it.skillsArray.isNullOrEmpty())
            skillsArray = arrayListOf()
        else
            skillsArray = arrayListOf(*it.skillsArray.split(",").toTypedArray())

//        reviews array
        RatingFirebaseService.getUsersRating(it.id) {
            reviewsArray = it
            reviewsAdapter.ratings = reviewsArray
            reviewsAdapter.notifyDataSetChanged()
        }

        ppImage = ""
        if(!it.imagePath.isEmpty()) {

            if(!it.imagePath.contains("profileImages"))
                it.imagePath = "/profileImages/${it.imagePath.split("/").last()}"
            ppImage = it.imagePath
            getProfilePicFromFirebaseStorage(it.imagePath)
        }
    }

    private fun getProfilePicFromFirebaseStorage(imagePath: String) {
        val storage = FirebaseStorage.getInstance()
        val gsReference = storage.getReferenceFromUrl("gs://labs-1b5fa.appspot.com/")
        val imageRef : StorageReference = gsReference.child("/profileImages/${imagePath.split("/").last()}")
        val ONE_MEGABYTE: Long = 1024 * 1024
        imageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener {
            // Update profile image view
            profileImageView.setImageBitmap(BitmapFactory.decodeByteArray(it, 0, it.size))
            loadingSpinner.visibility = View.GONE
        }.addOnFailureListener {
            // Handle any errors
            Log.e("Firestore", "Error loading profile image: " + it.toString())
        }
    }
}