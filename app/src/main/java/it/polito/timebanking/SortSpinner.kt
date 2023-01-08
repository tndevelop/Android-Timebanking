package it.polito.timebanking

import android.util.Log
import android.view.View
import android.widget.AdapterView
import it.polito.timebanking.viewmodels.OfferViewModel

class SortSpinner(val vm: OfferViewModel) : AdapterView.OnItemSelectedListener {

    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        val item = parent.getItemAtPosition(pos).toString()
        when(item) {
            "Title" -> { vm.sortTitle() }
            "Duration ascending" -> { vm.sortDurationAscending() }
            "Duration descending" -> { vm.sortDurationDescending() }
            "Date and time" -> { vm.sortDateAndTime() }
            "Location" -> { vm.sortLocation() }
            "User rating" -> { vm.sortUserRating() }
            "User name" -> { vm.sortUserName() }
            else -> {
                Log.e("Error: ", "Unknown item selected")
            }
        }

    }

    override fun onNothingSelected(parent: AdapterView<*>) {

    }

}