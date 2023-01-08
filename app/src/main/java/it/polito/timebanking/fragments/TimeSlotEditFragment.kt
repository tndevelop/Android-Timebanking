package it.polito.timebanking.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.polito.timebanking.R
import it.polito.timebanking.database.AdvertisementDemo
import it.polito.timebanking.database.DateConverter
import it.polito.timebanking.viewmodels.AdvertisementViewModel
import it.polito.timebanking.viewmodels.ServicesViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.*


class TimeSlotEditFragment : Fragment(R.layout.fragment_time_slot_edit) {

    val advertisementViewModel by activityViewModels<AdvertisementViewModel>()
    val servicesViewModel by activityViewModels<ServicesViewModel>()
    var id: String? = null
    var oldAd: AdvertisementDemo = AdvertisementDemo()
    private lateinit var auth: FirebaseAuth
    var myServices: MutableList<String> = mutableListOf()
    var selectedService = "Babysitting"

    lateinit var titleTextInput: TextInputEditText
    lateinit var descriptionTextInput: TextInputEditText
    lateinit var locationTextInput: TextInputEditText
    lateinit var restrictionsTextInput: TextInputEditText
    lateinit var dateDatePicker: DatePicker
    lateinit var timeFrom: TimePicker
    lateinit var durationInput: TimePicker
    val sdf: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView(view)

        id = arguments?.getString("id")
        if (id != null) {
            advertisementViewModel.loadByID(id!!)
            oldAd = AdvertisementDemo().also {  it.id = id!!; it.dateAndTime = arguments?.getString("date")!!;
                it.description = arguments?.getString("description")!!; it.duration = arguments?.getLong("duration")!!;
                it.location = arguments?.getString("location")!!; it.restrictions = arguments?.getString("restrictions")!!;
                it.service = arguments?.getString("service")!!; it.title = arguments?.getString("title")!!;
                it.userId = arguments?.getString("userId")!!}

            titleTextInput.setText(oldAd.title)
            selectedService = oldAd.service
            descriptionTextInput.setText(oldAd.description)

            val savedDate= DateConverter().dateToCalendar(sdf.parse(oldAd.dateAndTime))
            dateDatePicker.updateDate(savedDate!!.get(YEAR), savedDate.get(MONTH), savedDate.get(
                DATE))
            timeFrom.currentHour = sdf.parse(oldAd.dateAndTime).hours
            timeFrom.currentMinute = sdf.parse(oldAd.dateAndTime).minutes
            val durationHrs = (oldAd.duration/60).toInt()
            val durationMins = oldAd.duration - 60*durationHrs
            durationInput.currentHour = durationHrs
            durationInput.currentMinute = durationMins.toInt()
            locationTextInput.setText(oldAd.location)
            restrictionsTextInput.setText(oldAd.restrictions)
        } else {
                (activity as AppCompatActivity).supportActionBar?.setTitle(getString(R.string.create_advertisement))
            setFakeTimeSlots(view)
        }


        //Set up the list of services
        servicesViewModel.services.observe(viewLifecycleOwner) {
            myServices = it.toMutableList()
            setUpServicesSpinner()
        }
    }


    private fun setUpServicesSpinner() {
        // Set up the services in the spinner
        val spinner = requireView().findViewById<Spinner>(R.id.service_spinner)
        ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            myServices
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }
        spinner.onItemSelectedListener = servicesSpinnerSelectedListenter
        spinner.setSelection(myServices.indexOf(selectedService))
    }

    private val servicesSpinnerSelectedListenter = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
            val item = parent.getItemAtPosition(pos).toString()
            selectedService = item
        }

        override fun onNothingSelected(parent: AdapterView<*>) {
            // Do nothing
        }
    }

    private fun setFakeTimeSlots(view :View){
        titleTextInput.setText("Enter title")
        descriptionTextInput.setText("Enter description here")
        durationInput.currentHour=0
        durationInput.currentMinute=0
        locationTextInput.setText("Enter location")
        restrictionsTextInput.setText("None")
    }

    private fun initView(view:View){
        titleTextInput= view.findViewById(R.id.titleTextInput)
        descriptionTextInput= view.findViewById(R.id.descriptionTextInput2)
        durationInput= view.findViewById(R.id.duration_input)
        durationInput.setIs24HourView(true)
        locationTextInput= view.findViewById(R.id.locationTexInput2)
        restrictionsTextInput= view.findViewById(R.id.restrictionTextInput2)
        dateDatePicker= view.findViewById(R.id.editDate_DatePicker)
        dateDatePicker.minDate = System.currentTimeMillis()
        timeFrom = view.findViewById(R.id.time_from)
        timeFrom.setIs24HourView(true)
    }


    override fun onStop() {
        super.onStop()

        //Saving new data in an Advertise object
        val adv = AdvertisementDemo()
        auth = Firebase.auth
        adv.userId = auth.currentUser?.uid!!
        adv.title = titleTextInput.text.toString()
        adv.service = selectedService //serviceTextInput.text.toString()
        adv.description = descriptionTextInput.text.toString()
        adv.location = locationTextInput.text.toString()
        adv.restrictions = restrictionsTextInput.text.toString()

        // Building date
        val currDay= dateDatePicker.dayOfMonth
        val currMonth= dateDatePicker.month
        val currYear= dateDatePicker.year
        val currHour = timeFrom.currentHour
        val currMin = timeFrom.currentMinute
        val date = GregorianCalendar(currYear,currMonth,currDay,currHour, currMin).time
        adv.dateAndTime = sdf.format(date)
        adv.duration = (durationInput.currentHour * 60 + durationInput.currentMinute).toLong()

        // Set id if editing an existing object
        if (id != null) {
            adv.id = id!!
        }

        //Saving adv data to database
        advertisementViewModel.save(adv.id,
            ObjectMapper().convertValue(adv, HashMap::class.java) as HashMap<String, Any>,
            {
                Log.d("timebanking", "success")
            },
            { e: Exception ->
                Log.d("timebanking", e.message.toString())
            }
        )

        // See if any modification to an existing ad were made, and show snackbar to let users know their changes were saved, or not if no changes made
        fun checkForChanges(): Boolean {
            return oldAd==null || (oldAd?.title != adv.title || oldAd?.service != adv.service || oldAd?.description != adv.description
                    || oldAd?.dateAndTime != adv.dateAndTime || oldAd?.duration != adv.duration || oldAd?.location != adv.location
                    || oldAd?.restrictions != adv.restrictions)
        }

        if (checkForChanges()) {
            Snackbar.make(titleTextInput, "Changes saved", Snackbar.LENGTH_SHORT).show()
        }

    }

}