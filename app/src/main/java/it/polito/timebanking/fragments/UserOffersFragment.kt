package it.polito.timebanking.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import it.polito.timebanking.adapters.OffersAdapter
import it.polito.timebanking.R
import it.polito.timebanking.SortSpinner
import it.polito.timebanking.entities.OfferStatus
import it.polito.timebanking.viewmodels.OfferViewModel

class UserOffersFragment : Fragment(R.layout.fragment_user_offers){

    val offerModel : OfferViewModel by activityViewModels<OfferViewModel>()
    var selectedRating = "Any"
    var selectedLocation = "Any"
    var selectedDuration : Long? = null
    var selectedDateFrom : Long? = null
    var selectedDateTo: Long? = null
    var service: String? = null
    lateinit var anyRatingButton : Button
    lateinit var twoRatingButton : Button
    lateinit var threeRatingButton: Button
    lateinit var fourRatingButton: Button
    lateinit var fiveRatingButton: Button
    var ratingButtons: MutableList<Button> = mutableListOf()
    lateinit var filterLayout: ConstraintLayout
    lateinit var durationHourView : EditText
    lateinit var durationMinuteView : EditText
    lateinit var arrowImageView : ImageView
    lateinit var filterButton: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)
        filterButton = requireView().findViewById<LinearLayout>(R.id.filterButtonLayout)
        arrowImageView = requireView().findViewById<ImageView>(R.id.arrowImageView)
        filterLayout = requireView().findViewById(R.id.filter_layout)
        filterLayout.visibility = View.GONE
        arrowImageView.setImageDrawable(resources.getDrawable(R.drawable.downarrow))

        val rv = getView()?.findViewById<RecyclerView>(R.id.recView)
        rv?.layoutManager = LinearLayoutManager(getView()?.context)

        val noItemsMsg = getView()?.findViewById<LinearLayout>(R.id.no_offers_found_layout)
        val loadingSpinner = requireView().findViewById<ProgressBar>(R.id.loadingSpinner)

        //Temp list of ads
        //offerModel.createDummyData()

        service = arguments?.getString("Service")

        // Put a loading spinner on the page while waiting for firebase data
        offerModel.loaded.observe(viewLifecycleOwner) {
            if (it <  3) {
                loadingSpinner?.visibility = View.VISIBLE
            } else {
                loadingSpinner?.visibility = View.GONE
                offerModel.filter("Any", null, null, null, "Any", service)
            }
        }

        offerModel.adOffers.observe(viewLifecycleOwner) {
            //Set up the list of advertisements
            if ((it == null || it.isEmpty()) && !loadingSpinner!!.isVisible ) {
                noItemsMsg?.visibility = View.VISIBLE

            } else {
                noItemsMsg?.visibility = View.GONE
            }
            Log.d("kmb", "Offers list userids: ${it.map { it.userId }}")

            val adapter = OffersAdapter(it.filter{ it.status != OfferStatus.ACCEPTED}.toMutableList(), requireContext())

            rv?.adapter = adapter
        }

        // Set up the sort by spinner
        val spinner = getView()?.findViewById<Spinner>(R.id.sort_by_spinner)
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.sort_by_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner?.adapter = adapter
        }
        spinner?.onItemSelectedListener = SortSpinner(offerModel)

        // Set up the filter
        filterButton?.setOnClickListener {
            //findNavController().navigate(R.id.action_userOffersFragment_to_offerDetailsFragment2)
            if (filterLayout.visibility == View.VISIBLE) {
                arrowImageView!!.setImageDrawable(resources.getDrawable(R.drawable.downarrow))
                filterLayout.visibility = View.GONE
            } else {
                arrowImageView!!.setImageDrawable(resources.getDrawable(R.drawable.uparrow))
                filterLayout.visibility = View.VISIBLE
                setUpFilter()
            }
        }

    }

    fun setUpFilter() {
        // Setting up the date range picker
        var dateRangePicker =
            MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select dates")
                .build()

        val editDateRangeView = getView()?.findViewById<Button>(R.id.edit_date_range)
        editDateRangeView?.setOnClickListener {
            dateRangePicker.show(parentFragmentManager, "tag")
        }

        dateRangePicker.addOnPositiveButtonClickListener {
            editDateRangeView?.setText(dateRangePicker.headerText)
            selectedDateFrom = dateRangePicker.selection?.first
            selectedDateTo = dateRangePicker.selection?.second
        }

        // Set up the locations in the spinner
        val spinner = requireView().findViewById<Spinner>(R.id.location_spinner)
        val locations = mutableListOf("Any")
        locations.addAll(offerModel.locations)
        ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            locations
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }
        spinner.onItemSelectedListener = locationSpinnerSelectedListenter
        spinner.setSelection(locations.indexOf(selectedLocation))

        // Set up rating select buttons
        anyRatingButton = requireView().findViewById(R.id.any_rating_button)
        twoRatingButton = requireView().findViewById(R.id.two_rating_button)
        threeRatingButton = requireView().findViewById(R.id.three_rating_button)
        fourRatingButton = requireView().findViewById(R.id.four_rating_button)
        fiveRatingButton = requireView().findViewById(R.id.five_rating_button)
        ratingButtons.add(anyRatingButton)
        ratingButtons.add(twoRatingButton)
        ratingButtons.add(threeRatingButton)
        ratingButtons.add(fourRatingButton)
        ratingButtons.add(fiveRatingButton)
        anyRatingButton.setOnClickListener(ratingClickListener)
        twoRatingButton.setOnClickListener(ratingClickListener)
        threeRatingButton.setOnClickListener(ratingClickListener)
        fourRatingButton.setOnClickListener(ratingClickListener)
        fiveRatingButton.setOnClickListener(ratingClickListener)

        // Duration input
        durationHourView = requireView().findViewById(R.id.duration_hour_input)
        durationMinuteView = requireView().findViewById(R.id.duration_minute_input)
        Log.d("Hour tet", durationHourView.text.toString())

        // Set up apply filter button
        val applyButton = requireView().findViewById<Button>(R.id.apply_filter_button)
        applyButton.setOnClickListener {
            var hour = 0L;
            var minute = 0L;
            val hourString = durationHourView.text.toString()
            val minuteString = durationMinuteView.text.toString()
            if (hourString != "") {
                hour = hourString.toLong()
            }
            if (minuteString != "") {
                minute = minuteString.toLong()
            }
            selectedDuration = hour * 60 + minute
            offerModel.filter(selectedLocation, selectedDuration, selectedDateFrom, selectedDateTo, selectedRating, service)
            filterLayout.visibility = View.GONE

        }

        // Set up clear filters button
        val clearButton = requireView().findViewById<Button>(R.id.clear_filters_button)
        clearButton.setOnClickListener {
            editDateRangeView?.setText("Select dates")
            selectedDateTo = null; selectedDateFrom = null;
            selectedLocation = "Any"
            spinner.setSelection(locations.indexOf(selectedLocation))
            anyRatingButton.callOnClick()
            selectedDuration = null
            durationHourView.text.clear()
            durationMinuteView.text.clear()
            offerModel.filter(selectedLocation, selectedDuration, selectedDateFrom, selectedDateTo, selectedRating, service)
        }
    }

    private val ratingClickListener = View.OnClickListener {
        selectedRating = (it as Button).text.toString()
        it.setBackgroundColor(resources.getColor(R.color.green))
        it.setTextColor(Color.WHITE)
        if (it.text != "Any") {
            val star = resources.getDrawable(R.drawable.ic_baseline_star_rate_24_white)
            it.setCompoundDrawablesWithIntrinsicBounds(null, null, star, null)
        }
        for (button in ratingButtons) {
            if (button.text != it.text) {
                button.setBackgroundColor(resources.getColor(R.color.white))
                button.setTextColor(resources.getColor(R.color.green))
                if (button.text != "Any") {
                    val star = resources.getDrawable(R.drawable.ic_baseline_star_rate_24)
                    button.setCompoundDrawablesWithIntrinsicBounds(null, null, star, null)
                }
            }
        }
    }

    private val locationSpinnerSelectedListenter = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
            val item = parent.getItemAtPosition(pos).toString()
            selectedLocation = item
            Log.d("Loc sel", selectedLocation)
        }

        override fun onNothingSelected(parent: AdapterView<*>) {
            // Do nothing
        }
    }



}
