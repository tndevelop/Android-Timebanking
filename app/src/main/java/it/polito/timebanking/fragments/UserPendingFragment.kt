package it.polito.timebanking.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.polito.timebanking.R
import it.polito.timebanking.adapters.OffersAdapter
import it.polito.timebanking.entities.Offer
import it.polito.timebanking.entities.OfferStatus
import it.polito.timebanking.viewmodels.OfferViewModel
import it.polito.timebanking.viewmodels.UserViewModel

class UserPendingFragment : Fragment(R.layout.fragment_pending_offers) {

    val userViewModel by activityViewModels<UserViewModel>()
    val offerViewModel by activityViewModels<OfferViewModel>()
    var userAccepted : MutableList<String> = mutableListOf()
    var userRequested : MutableList<String> = mutableListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)
        val recViewAccepted = getView()?.findViewById<RecyclerView>(R.id.recyclerViewAccepted)
        recViewAccepted?.layoutManager = LinearLayoutManager(getView()?.context)
        val noPendingAccepted = getView()?.findViewById<TextView>(R.id.noPendingAccepted)

        val recViewRequested= getView()?.findViewById<RecyclerView>(R.id.recyclerViewRequested)
        recViewRequested?.layoutManager = LinearLayoutManager(getView()?.context)
        val noPendingRequested = getView()?.findViewById<TextView>(R.id.noPendingRequested)

        //Access the current user profile
        userViewModel.user.observe(viewLifecycleOwner){

            //ACCEPTED TIME SLOTS
            if(it.acceptedTimeSlots.isNotEmpty()) {

                userAccepted = it.acceptedTimeSlots.split(",") as MutableList<String>
                var myAccepted: MutableList<Offer> = mutableListOf()

                if (userAccepted.isNotEmpty()) {
                    noPendingAccepted?.visibility = View.GONE
                    for (offId in userAccepted) {
                        if (offId == "") continue
                        val off = offerViewModel.loadByOfferId(offId)
                        if (off != null && off.status== OfferStatus.ACCEPTED) myAccepted.add(off)
                    }
                    val adapter = OffersAdapter(myAccepted, requireContext())
                    recViewAccepted?.adapter = adapter
                }

            }else {
                noPendingAccepted?.visibility = View.VISIBLE
            }


            //REQUESTED TIME SLOTS
            if(it.assignedTimeSlots.isNotEmpty()) {

                userRequested = it.assignedTimeSlots.split(",") as MutableList<String>
                var myRequested: MutableList<Offer> = mutableListOf()

                if (userRequested.isNotEmpty()) {
                    noPendingRequested?.visibility = View.GONE
                    for (offId in userRequested) {
                        if (offId == "") continue
                        val off = offerViewModel.loadByOfferId(offId)
                        if (off != null && off.status== OfferStatus.ACCEPTED) myRequested.add(off)
                    }
                    val adapter = OffersAdapter(myRequested, requireContext())
                    recViewRequested?.adapter = adapter
                }

            }else {
                noPendingRequested?.visibility = View.VISIBLE
            }


        }


    }



}

