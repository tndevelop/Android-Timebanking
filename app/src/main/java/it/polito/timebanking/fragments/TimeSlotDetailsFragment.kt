package it.polito.timebanking.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.*
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import it.polito.timebanking.R
import it.polito.timebanking.database.AdvertisementDemo
import it.polito.timebanking.viewmodels.AdvertisementViewModel
import java.text.SimpleDateFormat

class TimeSlotDetailsFragment : Fragment(R.layout.fragment_time_slot_details) {

    val advertisementViewModel by activityViewModels<AdvertisementViewModel>()
    var id: String? = null

    lateinit var titleTextView: TextView
    lateinit var serviceTextView: TextView
    lateinit var descriptionTextView: TextView
    lateinit var dateTextView: TextView
    lateinit var durationTextView: TextView
    lateinit var locationTextView: TextView
    lateinit var restrictionsTextView: TextView
    lateinit var timeFromTextView: TextView
    var oldAd: AdvertisementDemo = AdvertisementDemo()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_right)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState);

        setHasOptionsMenu(true)
        initView(view)

        id = arguments?.getString("id")
        if (id != null) {
            advertisementViewModel.loadByID(id!!)
            oldAd = AdvertisementDemo().also {  it.id = id!!; it.dateAndTime = arguments?.getString("date")!!;
                it.description = arguments?.getString("description")!!; it.duration = arguments?.getLong("duration")!!;
                it.location = arguments?.getString("location")!!; it.restrictions = arguments?.getString("restrictions")!!;
                it.service = arguments?.getString("service")!!; it.title = arguments?.getString("title")!!;
                it.userId = arguments?.getString("userId")!!}
            setThings(oldAd)
        } else {
            //Shouldn't reach here since detail fragment is only loaded with an id in the bundle
            Log.e("TimeSlotDetails","Someone didn't send an id to the details fragment")
        }

        advertisementViewModel.advertisement.observe(viewLifecycleOwner) {
            if (it == null ) {
                //setFakeTimeSlots(view)
            } else {
                setThings(it)

            }
        }

    }

    private fun setThings(it: AdvertisementDemo) {
        titleTextView.text= it.title
        serviceTextView.text= it.service
        descriptionTextView.text= it.description

        val durationHrs = (it.duration/60).toInt()
        val durationMins = it.duration - 60*durationHrs

        durationTextView.text= durationHrs.toString() + "h " + durationMins.toString() + "m"
        locationTextView.text= it.location
        restrictionsTextView.text= if(it.restrictions.length > 0) it.restrictions else "No restrictions."

        dateTextView.text = it.dateAndTime.substring(0, it.dateAndTime.length - 6)

        timeFromTextView.text= it.dateAndTime.substring(it.dateAndTime.length - 5, it.dateAndTime.length )

    }

    private fun setFakeTimeSlots(view :View){
            titleTextView.text= "TEST TITLE"
            serviceTextView.text= "Cooking"
            descriptionTextView.text= "I'm a chef!"
            durationTextView.text= "2h"
            locationTextView.text= "Turin"
            restrictionsTextView.text= "None"
        }

        private fun initView(view:View){
            titleTextView= view.findViewById(R.id.titleTextView)
            serviceTextView= view.findViewById(R.id.OfferedServiceTextView2)
            descriptionTextView= view.findViewById(R.id.descriptionTextView2)
            dateTextView= view.findViewById(R.id.dateTextView2)
            durationTextView= view.findViewById(R.id.durationTextView2)
            locationTextView= view.findViewById(R.id.locationTextView2)
            restrictionsTextView= view.findViewById(R.id.restrictionTextView2)
            timeFromTextView= view.findViewById(R.id.timefromTextView)
        }

        fun editTimeSlot() {
            val bundle = bundleOf("id" to id, "title" to oldAd.title, "date" to oldAd.dateAndTime, "location" to oldAd.location,
                "duration" to oldAd.duration, "description" to oldAd.description, "service" to  oldAd.service, "restrictions" to oldAd.restrictions,
                "userId" to oldAd.userId)
            findNavController().navigate(R.id.action_timeSlotDetailsFragment_to_timeSlotEditFragment, bundle)
        }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_edit, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_edit -> {
                editTimeSlot()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


}