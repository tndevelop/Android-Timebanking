package it.polito.timebanking.fragments

import android.os.Bundle
import android.text.TextUtils.isEmpty
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.polito.timebanking.R
import it.polito.timebanking.adapters.OffersAdapter
import it.polito.timebanking.entities.Offer
import it.polito.timebanking.viewmodels.OfferViewModel
import it.polito.timebanking.viewmodels.UserViewModel

class UserFavouriteFragment : Fragment(R.layout.fragment_user_favourites) {

    val userViewModel by activityViewModels<UserViewModel>()
    val offerViewModel by activityViewModels<OfferViewModel>()
    var userFavourites : MutableList<String> = mutableListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)
        val recView = getView()?.findViewById<RecyclerView>(R.id.recyclerView)
        recView?.layoutManager = LinearLayoutManager(getView()?.context)

        val noFavMsg = getView()?.findViewById<TextView>(R.id.noFavouritesMessage)

        //Access the current user profile
        userViewModel.user.observe(viewLifecycleOwner){

            if(it.favourites.isNotEmpty()) {

                userFavourites= it.favourites.split(",") as MutableList<String>

                var myFavourites: MutableList<Offer> = mutableListOf()
                if(userFavourites.isNotEmpty()) {

                    noFavMsg?.visibility= View.GONE

                    for(offId in userFavourites){
                        if(offId== "") continue
                        val off= offerViewModel.loadByOfferId(offId)
                        if (off!=null) myFavourites.add(off)
                    }

                    val adapter = OffersAdapter(myFavourites, requireContext())
                    recView?.adapter = adapter

                }

            }else{
                noFavMsg?.visibility= View.VISIBLE
            }
        }

    }

}