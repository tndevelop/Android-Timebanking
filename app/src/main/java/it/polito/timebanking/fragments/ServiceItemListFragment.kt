package it.polito.timebanking.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.polito.timebanking.R
import it.polito.timebanking.adapters.ServiceAdapter
import it.polito.timebanking.viewmodels.AdvertisementViewModel
import it.polito.timebanking.viewmodels.OfferViewModel
import it.polito.timebanking.viewmodels.ServicesViewModel

class ServiceItemListFragment : Fragment(R.layout.fragment_service_item_list) {

    val advertisementViewModel by activityViewModels<AdvertisementViewModel>()

    val offerModel : OfferViewModel by activityViewModels<OfferViewModel>()
    val servicesViewModel : ServicesViewModel by viewModels<ServicesViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)
        val recView = getView()?.findViewById<RecyclerView>(R.id.recyclerView)

        recView?.layoutManager = GridLayoutManager(getView()?.context, 2)

        val noServMsg = getView()?.findViewById<TextView>(R.id.noServicesMessage)
        val loadingSpinner = requireView().findViewById<ProgressBar>(R.id.loadingSpinner)
        loadingSpinner?.visibility = View.VISIBLE

        var myServices: MutableList<String> = mutableListOf()

        // Pre initialising the offers list
        offerModel.load()
        offerModel.buildOffers()

        // Put a loading spinner on the page while waiting for firebase data
        servicesViewModel.loaded.observe(viewLifecycleOwner) {
            if (it == false) {
                loadingSpinner?.visibility = View.VISIBLE

            } else {
                loadingSpinner?.visibility = View.GONE
                if(myServices.isEmpty())
                    noServMsg?.visibility = View.VISIBLE
                else
                    noServMsg?.visibility = View.GONE
            }
        }

        //Set up the list of services
        servicesViewModel.services.observe(viewLifecycleOwner) {
            myServices = it.toMutableList()
            if(myServices.isEmpty() && !loadingSpinner!!.isVisible ){
                noServMsg?.visibility = View.VISIBLE
            }else{
                noServMsg?.visibility = View.GONE
            }
            val adapter = ServiceAdapter(myServices)
            recView?.adapter = adapter
        }


    }

}