package it.polito.timebanking

import android.content.ContentValues
import android.content.Intent
import android.content.IntentSender
import android.graphics.*
import android.media.ExifInterface
import android.os.Bundle
import android.util.Log
import android.view.View

import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import it.polito.timebanking.database.User
import it.polito.timebanking.viewmodels.OfferViewModel
import it.polito.timebanking.viewmodels.ServicesViewModel
import it.polito.timebanking.viewmodels.UserViewModel


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    val userViewModel by viewModels<UserViewModel>()
    val offerViewModel by viewModels<OfferViewModel>()
    val servicesViewModel by viewModels<ServicesViewModel>()
    lateinit var user: LiveData<User>
    lateinit var timeCreditTextView: TextView
    lateinit var nicknameTextView: TextView
    lateinit var profileImageView: ImageView
    lateinit var logoutButton: LinearLayout

    //google sign in
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private val REQ_ONE_TAP = 2
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up the navigation drawer
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)

        appBarConfiguration = AppBarConfiguration(setOf(R.id.showProfileFragment,
                                                        R.id.timeSlotListFragment,
                                                        R.id.serviceItemListFragment,
                                                        R.id.userFavouriteFragment,
                                                        R.id.userPendingFragment,
                                                        R.id.chatListFragment
                                                        ), drawerLayout)

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        //Find drawer views
        timeCreditTextView = navView.getHeaderView(0).findViewById(R.id.time_credit_value_text_view)
        nicknameTextView = navView.getHeaderView(0).findViewById(R.id.header_nickname)
        profileImageView = navView.getHeaderView(0).findViewById(R.id.header_image)
        logoutButton = navView.getHeaderView(0).findViewById(R.id.logout_button)
        profileImageView.setImageDrawable(resources.getDrawable(R.drawable.avatar))

        logoutButton?.setOnClickListener{
            Firebase.auth.signOut()
            backToLogin()
        }

        //Loading user's data from database
        userViewModel.user.observe(this) {
            //Set up the drawer header
            if (it == null) {
                timeCreditTextView.text = "00:00"
                nicknameTextView.text = "Nickname"
            } else {
                val creditHours = it.credit / 60
                val creditMins = it.credit - creditHours*60
                timeCreditTextView.text = String.format("%02d:%02d",creditHours, creditMins)
                nicknameTextView.text = it.nickname
                if (!it.imagePath.isEmpty()) {
                    if(!it.imagePath.contains("profileImages"))
                        it.imagePath = "/profileImages/${it.imagePath.split("/").last()}"
                    getProfilePicFromFirebaseStorage(it.imagePath)
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.serviceItemListFragment -> supportActionBar?.setTitle("Services")
                R.id.timeSlotListFragment -> supportActionBar?.setTitle(getString(R.string.menu_advertisements))
                R.id.showProfileFragment -> supportActionBar?.setTitle(getString(R.string.menu_profile))
                R.id.editProfileFragment -> supportActionBar?.setTitle(getString(R.string.edit_profile))
                R.id.timeSlotDetailsFragment -> supportActionBar?.setTitle(getString(R.string.advertisement))
                R.id.timeSlotEditFragment -> supportActionBar?.setTitle(getString(R.string.edit_advertisement))
                R.id.userOffersFragment -> supportActionBar?.setTitle(getString(R.string.user_offers))
                R.id.offerDetailsFragment2 -> supportActionBar?.setTitle(getString(R.string.offer_details))
                R.id.offerProfileFragment -> supportActionBar?.setTitle(getString(R.string.offer_profile))
                R.id.userFavouriteFragment -> supportActionBar?.setTitle(getString(R.string.favourites))
                R.id.userPendingFragment -> supportActionBar?.setTitle("Pending")
                R.id.chatFragment -> supportActionBar?.setTitle(getString(R.string.chat))
                R.id.chatListFragment -> supportActionBar?.setTitle(getString(R.string.chatList))
            }
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
//            loadingSpinner.visibility = View.GONE
        }.addOnFailureListener {
            // Handle any errors
            Log.e("Firestore", "Error loading profile image: " + it.toString())
        }
    }

    private fun backToLogin(){
        //findViewById<ConstraintLayout>(R.id.loading).visibility = View.VISIBLE
        startActivity(Intent(this, WelcomeActivity::class.java))
        finishAffinity()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        for(fragment: Fragment in supportFragmentManager.fragments)
            fragment.onActivityResult(requestCode, resultCode, data)


    }
    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        auth = Firebase.auth
        val currentUser = auth.currentUser
        /*
        if(currentUser != null)
            updateUI(currentUser)

         */
    }



    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun setPic(currentPhotoPath: String) {
        // Get the dimensions of the View
        val targetW: Int = profileImageView.maxWidth
        val targetH: Int = profileImageView.maxHeight

        val bmOptions = BitmapFactory.Options()
        bmOptions.apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(currentPhotoPath, bmOptions)

            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Int = Math.max(1, Math.min(photoW / targetW, photoH / targetH))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inPurgeable = true
        }
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions)?.also { bitmap ->
            val rotatedBitmap = checkPictureRotation(bitmap, currentPhotoPath)
            val circleBitmap = cropBitmapToCircle(rotatedBitmap)
            profileImageView.setImageBitmap(circleBitmap)
        }
    }

    fun checkPictureRotation(bitmap: Bitmap, currentPhotoPath: String) : Bitmap {
        val ei = ExifInterface(currentPhotoPath)
        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        val rotatedBitmap : Bitmap
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotatedBitmap = rotateImage(bitmap, 90.toFloat())
            ExifInterface.ORIENTATION_ROTATE_180 -> rotatedBitmap = rotateImage(bitmap, 180.toFloat())
            ExifInterface.ORIENTATION_ROTATE_270 -> rotatedBitmap = rotateImage(bitmap, 270.toFloat())
            else -> {
                rotatedBitmap = bitmap
            }
        }
        return rotatedBitmap
    }

    fun rotateImage(bitmap: Bitmap, angle: Float) : Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun cropBitmapToCircle(bitmap: Bitmap) : Bitmap {
        val width: Int = profileImageView.maxWidth
        val height: Int = profileImageView.maxHeight

        val output = Bitmap.createBitmap(
            width, height, Bitmap.Config.ARGB_8888  )

        val canvas = Canvas(output)
        val paintColor = Paint()
        paintColor.setFlags(Paint.ANTI_ALIAS_FLAG)

        val rectF = RectF(Rect(0, 0, width, height))

        canvas.drawRoundRect(rectF, width.toFloat() / 2, height.toFloat() / 2, paintColor)

        val paintImage = Paint()
        paintImage.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP))
        canvas.drawBitmap(bitmap, Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()),
            Rect(0, 0, width, height), paintImage)

        return output
    }

}