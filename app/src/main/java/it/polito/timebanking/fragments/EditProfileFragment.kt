package it.polito.timebanking.fragments

import android.Manifest
import android.R.attr.bitmap
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import it.polito.timebanking.R
import it.polito.timebanking.adapters.CustomAdapter
import it.polito.timebanking.database.User
import it.polito.timebanking.viewmodels.ServicesViewModel
import it.polito.timebanking.viewmodels.UserViewModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class EditProfileFragment: Fragment(R.layout.fragment_edit_profile) {

    val REQUEST_IMAGE_CAPTURE = 1
    val SELECT_IMAGE_FROM_GALLERY = 2
    private val PERMISSION_CODE = 1001;

    val userViewModel by activityViewModels<UserViewModel>()
    val servicesViewModel by activityViewModels<ServicesViewModel>()
    var newUser: Boolean = false
    var oldUser: User? = null

    lateinit var nicknameEditView: EditText
    lateinit var descriptionEditView: EditText
    lateinit var fullnameEditView: EditText
    lateinit var emailEditView: EditText
    lateinit var locationEditView: EditText
    lateinit var profileImageView: ImageView
    lateinit var loadingSpinner: ProgressBar
    lateinit var editImageButton: ImageButton
    lateinit var currentPhotoPath: String
    lateinit var skillsAdapter: CustomAdapter
    lateinit var skillsGridView: GridView
    lateinit var skillEditText: EditText
    lateinit var addSkillButton: Button
    var rating: Float = 0f
    var skills: MutableList<String> = mutableListOf()
    var selectedSkill = "Babysitting"
    var skillsArray: ArrayList<String> = arrayListOf<String>()
    var alreadyOpened: Boolean = false
    var items = arrayOf("Camera", "Gallery")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init(view)
        skillsAdapter = CustomAdapter(requireContext(), skillsArray)

        //Loading user's data from database
        userViewModel.user.observe(viewLifecycleOwner) {
            if (it == null) {
                setNoUserDetails()
            } else {
                oldUser = it
                setUserDetails(it)
            }
            skillsAdapter.skills = skillsArray
            skillsGridView.adapter = skillsAdapter
            skillsGridView.emptyView = view.findViewById(R.id.emptyTextView)
            skillsGridView.stopNestedScroll()
            skillsGridView.layoutParams.height = (45 * ((skillsArray.size + 15) / 2) as Int)
        }

        editImageButton.setOnClickListener {
            openAlertDialog()
        }

        //Set up the list of skills to choose from based on services available in the app
        servicesViewModel.services.observe(viewLifecycleOwner) {
            skills = it.toMutableList()
            setUpServicesSpinner()
        }

        addSkillButton.setOnClickListener {
            // check if skill already in users list, if not add it
            if (skillsArray.indexOf(selectedSkill) == -1) {
                skillsArray.add(selectedSkill)
                skillsAdapter.skills = skillsArray
                skillsAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun init(view: View) {
        nicknameEditView = view.findViewById(R.id.nicknameEditText)
        descriptionEditView = view.findViewById(R.id.descriptionEditText)
        fullnameEditView = view.findViewById(R.id.fullnameEditText)
        emailEditView = view.findViewById(R.id.emailEditText)
        locationEditView = view.findViewById(R.id.locationEditText)
        skillsGridView = view.findViewById(R.id.skillsGridView)
        addSkillButton = view.findViewById(R.id.addSkillButton)
        profileImageView = view.findViewById(R.id.avatarImageView)
        editImageButton = view.findViewById(R.id.editImageButton)
        loadingSpinner = view.findViewById(R.id.loadingSpinner)
    }

    private fun setNoUserDetails() {
        nicknameEditView.setText("Enter nickname")
        descriptionEditView.setText("Enter description")
        fullnameEditView.setText("Enter full name")
        emailEditView.setText("Enter email")
        locationEditView.setText("Enter location")
        skillsArray = arrayListOf(*"Crafting,Babysitting".split(",").toTypedArray())
        currentPhotoPath = ""
        newUser = true
        loadingSpinner.visibility = View.GONE
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
            loadingSpinner.visibility = View.GONE
            Log.e("Firestore", "Error loading profile image: " + it.toString())
        }
    }

    private fun setUserDetails(it: User) {
        nicknameEditView.setText(it.nickname)
        descriptionEditView.setText(it.description)
        fullnameEditView.setText(it.fullName)
        emailEditView.setText(it.email)
        locationEditView.setText(it.location)
        rating = it.rating

        if(it.skillsArray.isNullOrEmpty())
            skillsArray = arrayListOf()
        else
            skillsArray = arrayListOf(*it.skillsArray.split(",").toTypedArray())

        if (!it.imagePath.isEmpty() && !alreadyOpened) {
            currentPhotoPath = it.imagePath
            getProfilePicFromFirebaseStorage(it.imagePath)
            alreadyOpened = true
        }
        newUser = false
    }

    private fun openAlertDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Options")
        builder.setItems(items, DialogInterface.OnClickListener { dialogInterface, i ->
            if (items.get(i).equals("Camera")) {
                dispatchTakePictureIntent()
            }
            else if (items.get(i).equals("Gallery")) {
                galleryPermissionAndIntent()
            }
        })

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun setUpServicesSpinner() {
        // Set up the services in the spinner
        val spinner = requireView().findViewById<Spinner>(R.id.service_spinner)
        ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            skills
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }
        spinner.onItemSelectedListener = servicesSpinnerSelectedListenter
        spinner.setSelection(skills.indexOf(selectedSkill))
    }

    private val servicesSpinnerSelectedListenter = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
            val item = parent.getItemAtPosition(pos).toString()
            selectedSkill = item
        }

        override fun onNothingSelected(parent: AdapterView<*>) {
            // Do nothing
        }
    }

    override fun onStop() {
        super.onStop()

        //Saving new data in a User object
        val user = User()
        user.nickname = nicknameEditView.text.toString()
        user.description = descriptionEditView.text.toString()
        user.fullName = fullnameEditView.text.toString()
        user.email = emailEditView.text.toString()
        user.location = locationEditView.text.toString()
        user.skillsArray = skillsArray.joinToString(separator = ",")
        user.rating = rating
        if(this::currentPhotoPath.isInitialized) user.imagePath = "/profileImages/${currentPhotoPath.split("/").last()}"
        else user.imagePath = ""


        var file: Uri? = null
        if (!user.imagePath.isEmpty()) {
            file = if (Uri.parse(user.imagePath).isRelative)
                Uri.fromFile(File(user.imagePath.toString()))
            else
                Uri.parse(user.imagePath)
        }

        //Saving user data to database
        if (newUser)
            Firebase.auth.currentUser?.let {
                userViewModel.save(it.uid,
                    ObjectMapper().convertValue(user, HashMap::class.java) as HashMap<String, Any>,
                    {
                        if (file != null) {

                            val storage = FirebaseStorage.getInstance()

                            val gsReference =
                                storage.getReferenceFromUrl("gs://labs-1b5fa.appspot.com/")
                            val img = gsReference.child("/profileImages/${File(file.path).name}")
                            val targetW: Int = 1280
                            val targetH: Int = 1706

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
                                val baos = ByteArrayOutputStream()
                                // Save a low quality image
                                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 0, baos)
                                img.putBytes(baos.toByteArray()).addOnCompleteListener {
                                    user.imagePath = "/profileImages/${File(file.path).name}"
                                }
                            }

                        }
                    },
                    { e: Exception ->
                        Log.d("UserVM", e.message.toString())
                    }
                )
            }
        else {
            Firebase.auth.currentUser?.let {
                userViewModel.update(it.uid,
                    ObjectMapper().convertValue(user, HashMap::class.java) as HashMap<String, Any>,
                    {
                        if(file != null) {

                            val storage = FirebaseStorage.getInstance()

                            val gsReference = storage.getReferenceFromUrl("gs://labs-1b5fa.appspot.com/")
                            val img = gsReference.child("/profileImages/${File(file.path).name}")

                            val targetW: Int = 1280
                            val targetH: Int = 1706

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
                                val baos = ByteArrayOutputStream()
                                // Save a low quality image
                                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 0, baos)
                                img.putBytes(baos.toByteArray()).addOnCompleteListener {
                                    user.imagePath = "/profileImages/${File(file.path).name}"
                                }
                            }
                        }
                    },
                    { e: Exception ->
                        Log.d("UserVM", e.message.toString())
                    }
                )
            }

        }

        fun checkForChanges(): Boolean {
            return oldUser!=null && (oldUser?.nickname != user.nickname || oldUser?.description != user.description || oldUser?.fullName != user.fullName
                    || oldUser?.email != user.email || oldUser?.location != user.location || oldUser?.skillsArray != user.skillsArray
                    || oldUser?.imagePath != user.imagePath)
        }

        if (checkForChanges()) {
            Snackbar.make(nicknameEditView, "Changes saved", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun galleryPermissionAndIntent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (requireContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                requestPermissions(permissions, PERMISSION_CODE)
            }
            else
                chooseImageGallery()
        }
        else
            chooseImageGallery()
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->

            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(requireContext().packageManager)?.also {

                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }

                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        requireContext(),
                        "it.polito.timebanking.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == AppCompatActivity.RESULT_OK) {
            setPic(currentPhotoPath)
        } else if(requestCode == SELECT_IMAGE_FROM_GALLERY && resultCode == AppCompatActivity.RESULT_OK) {

            profileImageView.setImageURI(data!!.data!!)
            saveBitmapInTempLocation((profileImageView.drawable as BitmapDrawable).bitmap)
        }
    }

    private fun saveBitmapInTempLocation(bitmap: Bitmap) {
//       save bitmap to file
        currentPhotoPath = createImageFile().absolutePath

        val output = FileOutputStream(currentPhotoPath)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
        output.flush()
        output.close()
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun setPic(currentPhotoPath: String) {
        // Get the dimensions of the View
        val targetW: Int = 1280
        val targetH: Int = 1706

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
            profileImageView.setImageBitmap(rotatedBitmap)
            val baos = FileOutputStream(currentPhotoPath)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 0, baos)
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

    private fun chooseImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, SELECT_IMAGE_FROM_GALLERY)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    chooseImageGallery()
                else
                    Toast.makeText(requireContext(),"Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}