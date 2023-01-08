package it.polito.timebanking.adapters

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.RatingBar
import android.widget.TextView
import it.polito.timebanking.R
import it.polito.timebanking.entities.Rating

class RatingsAdapter  // invoke the suitable constructor of the ArrayAdapter class
    (var context: Context, var ratings: ArrayList<Rating>): BaseAdapter() {

    //Auto Generated Method
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var myView = convertView
        var holder: ViewHolder

        if (myView == null) {
            //If our View is Null than we Inflater view using Layout Inflater
            val mInflater = (context as Activity).layoutInflater

            //Inflating our skill_item_edit.
            myView = mInflater.inflate(R.layout.review_item, parent, false)

            //Create Object of ViewHolder Class and set our View to it
            holder = ViewHolder()

            holder.commentTextView = myView!!.findViewById<TextView>(R.id.commentTextView)
            holder.ratingTextView = myView!!.findViewById<TextView>(R.id.ratingTextView)
            holder.ratingBar = myView!!.findViewById<RatingBar>(R.id.ratingBar)

            //Set A Tag to Identify our view.
            myView.setTag(holder)
        } else {
            //If Our View in not Null than Just get View using Tag and pass to holder Object.
            holder = myView.getTag() as ViewHolder
        }

        holder.commentTextView!!.setText(ratings.get(position).comment)
        holder.ratingTextView!!.setText("${ratings.get(position).rate} stars")
        holder.ratingBar!!.rating = ratings.get(position).rate
        return myView
    }

    //Auto Generated Method
    override fun getItem(p0: Int): Any {
        return ratings.get(p0)
    }

    //Auto Generated Method
    override fun getItemId(p0: Int): Long {
        return 0
    }

    //Auto Generated Method
    override fun getCount(): Int {
        return ratings.size
    }

    class ViewHolder {
        var commentTextView: TextView? = null
        var ratingTextView: TextView? = null
        var ratingBar: RatingBar? = null
    }
}