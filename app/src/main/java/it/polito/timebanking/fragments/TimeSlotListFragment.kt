package it.polito.timebanking.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionInflater
import com.google.firebase.auth.FirebaseAuth
import it.polito.timebanking.adapters.AdvertisementAdapter
import it.polito.timebanking.R
import it.polito.timebanking.viewmodels.AdvertisementViewModel

class TimeSlotListFragment : Fragment(R.layout.fragment_time_slot_list) {

    val advertisementViewModel by activityViewModels<AdvertisementViewModel>()

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = TransitionInflater.from(requireContext())
        exitTransition = inflater.inflateTransition(R.transition.fade)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)
        val rv = getView()?.findViewById<RecyclerView>(R.id.recView)
        rv?.layoutManager = LinearLayoutManager(getView()?.context)

        val noItemsMsg = getView()?.findViewById<TextView>(R.id.noItemsMessage)

        advertisementViewModel.advertisements.observe(viewLifecycleOwner) {
            //Set up the list of advertisements
            if (it == null || it.isEmpty()) {
                noItemsMsg?.visibility = View.VISIBLE
            } else {
                noItemsMsg?.visibility = View.GONE
            }

            val adapter = AdvertisementAdapter(it.toMutableList(), advertisementViewModel)
            rv?.adapter = adapter
        }

        val fab = getView()?.findViewById<ImageButton>(R.id.floatingActionButton)
        fab?.setOnClickListener{
            findNavController().navigate(R.id.action_timeSlotListFragment_to_timeSlotEditFragment)
        }

    }

}