package it.polito.timebanking.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import it.polito.timebanking.R
import kotlin.time.ExperimentalTime

class ServiceAdapter(val data: MutableList<String>) : RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>()
{

    class ServiceViewHolder(v: View): RecyclerView.ViewHolder(v) {

        private val servButton : Button = v.findViewById(R.id.serviceButton)

        fun bind(servName : String, pos: Int, action : (view: View) -> Unit){

            servButton.text= servName
            servButton.setOnClickListener {
                //The bundle needs the name of the service or the id?
                val bundle = bundleOf("Service" to servName)
                it.findNavController().navigate(R.id.action_serviceItemListFragment_to_userOffersFragment, bundle)
            }

        }

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val vg = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_service_item, parent, false)
        return ServiceViewHolder(vg)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {

        val servName = data[position]
        holder.bind(servName, data.indexOf(servName) ){

            val pos = data.indexOf(servName)
            if(pos != -1){
                data.removeAt(pos)
                notifyItemRemoved(pos)
            }

        }

    }


    override fun getItemCount(): Int {
        return data.size
    }


}