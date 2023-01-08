package it.polito.timebanking.adapters

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import it.polito.timebanking.R

class CustomAdapter(var context: Context, var skills: ArrayList<String>) : BaseAdapter() {


    //Auto Generated Method
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var myView = convertView
        var holder: ViewHolder

        if (myView == null) {
            //If our View is Null than we Inflater view using Layout Inflater
            val mInflater = (context as Activity).layoutInflater

            //Inflating our skill_item_edit.
            myView = mInflater.inflate(R.layout.skill_item_edit, parent, false)

            //Create Object of ViewHolder Class and set our View to it
            holder = ViewHolder()

            //Find view By Id For all our Widget taken in grid_item.
            /*
             Here !! are use for non-null asserted two prevent From null.
             you can also use Only Safe (?.)
            */
            holder.xButton = myView!!.findViewById<TextView>(R.id.xButton) as TextView
            holder.mTextView = myView!!.findViewById<TextView>(R.id.textview_edit) as TextView

            //Set A Tag to Identify our view.
            myView.setTag(holder)
        } else {
            //If Our View in not Null than Just get View using Tag and pass to holder Object.
            holder = myView.getTag() as ViewHolder
        }

        //Setting Image to ImageView by position
        holder.xButton!!.setOnClickListener {
            skills.removeAt(position)
            notifyDataSetChanged()
        }

        //Setting name to TextView by position
        holder.mTextView!!.setText(skills.get(position))

        return myView
    }

    //Auto Generated Method
    override fun getItem(p0: Int): Any {
        return skills.get(p0)
    }

    //Auto Generated Method
    override fun getItemId(p0: Int): Long {
        return 0
    }

    //Auto Generated Method
    override fun getCount(): Int {
        return skills.size
    }

    class ViewHolder {
        var xButton: TextView? = null
        var mTextView: TextView? = null
    }
}