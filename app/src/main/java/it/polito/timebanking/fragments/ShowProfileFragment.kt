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
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import it.polito.timebanking.R
import it.polito.timebanking.database.User
import it.polito.timebanking.viewmodels.UserViewModel
import org.json.JSONObject
import java.io.File

class ShowProfileFragment: Fragment(R.layout.fragment_show_profile) {

    val userViewModel by activityViewModels<UserViewModel>()
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
    lateinit var reviewsCardView: CardView
    lateinit var skillsAdapter: ArrayAdapter<String>
    lateinit var skillsArray: ArrayList<String>
    lateinit var ppImage: String


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        init(view)

        //Loading user's data from database
        userViewModel.user.observe(viewLifecycleOwner) {
            if (it == null) {
                setNoUserDetails()
            } else {
                setUserDetails(it)
            }
            skillsAdapter = ArrayAdapter(requireContext(), R.layout.skill_item, R.id.textview, skillsArray)
            skillsGridView.adapter = skillsAdapter
            skillsGridView.emptyView = view.findViewById(R.id.emptyTextView)
            skillsGridView.stopNestedScroll()
            skillsGridView.layoutParams.height = (40 * ((skillsArray.size + 10) / 2) as Int)
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
        reviewsCardView = view.findViewById(R.id.reviewsCardView)

        reviewsCardView.visibility = View.GONE


        // Set up loading spinner
        loadingSpinner = view.findViewById(R.id.loadingSpinner)
    }

    private fun setNoUserDetails() {
        nicknameTextView.text = ""
        ratingBar.rating = 0f
        ratingTextView.text = "${ratingBar.rating} stars"
        descriptionTextView.text = ""
        emailTextView2.text = ""
        fullnameTextView2.text = ""
        locationTextView2.text = "Torino"
        timeCreditTextView.text = "05:00"
        skillsArray = arrayListOf(*"Crafting,Babysitting".split(",").toTypedArray())
        ppImage = ""
        profileImageView.setImageDrawable(resources.getDrawable(R.drawable.avatar))
        loadingSpinner.visibility = View.GONE
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

        if(it.skillsArray.isNullOrEmpty())
            skillsArray = arrayListOf()
        else
            skillsArray = arrayListOf(*it.skillsArray.split(",").toTypedArray())

        ppImage = ""
        if(!it.imagePath.isEmpty()) {
            if(!it.imagePath.contains("profileImages"))
                it.imagePath = "/profileImages/${it.imagePath.split("/").last()}"
            ppImage = it.imagePath
            getProfilePicFromFirebaseStorage(it.imagePath)
        } else {
            // Set default image
            profileImageView.setImageDrawable(resources.getDrawable(R.drawable.avatar))
            loadingSpinner.visibility = View.GONE
        }
    }

    private fun getProfilePicFromFirebaseStorage(imagePath: String) {
        val storage = FirebaseStorage.getInstance()
        val gsReference = storage.getReferenceFromUrl("gs://labs-1b5fa.appspot.com/")
        val imageRef : StorageReference = gsReference.child("/profileImages/${imagePath.split("/").last()}")
        val ONE_MEGABYTE: Long = 1024 * 1024
        imageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener {
//            // Update profile image view
            profileImageView.setImageBitmap(BitmapFactory.decodeByteArray(it, 0, it.size))
            loadingSpinner.visibility = View.GONE
        }.addOnFailureListener {
            // Handle any errors
            loadingSpinner.visibility = View.GONE
            Log.e("Firestore", "Error loading profile image: " + it.toString())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_edit, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_edit -> {
                editProfile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun editProfile() {
        findNavController().navigate(R.id.action_showFragment_to_editFragment)
    }
}