package com.minichain.minichainsplayer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import androidx.annotation.LayoutRes

public class CustomArrayAdapter<T>(context: Context, @LayoutRes private val layoutResource: Int, private val array: Array<String?>)
    : ArrayAdapter<String?>(context, layoutResource, array) {

    private var mArray: Array<String?> = array

    override fun getCount(): Int {
        return mArray.size
    }

    override fun getItem(i: Int): String? {
        return mArray[i]
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: TextView = convertView as TextView? ?: LayoutInflater.from(context).inflate(layoutResource, parent, false) as TextView
        view.text = mArray[position]
        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun publishResults(charSequence: CharSequence?, filterResults: FilterResults) {
                if (filterResults.values is Array<*>) {
                    mArray = filterResults.values as Array<String?>
                } else {
                    val arrayValues = filterResults.values as List<String?>
                    mArray = arrayValues.toTypedArray()
                }
                notifyDataSetChanged()
            }

            override fun performFiltering(charSequence: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                val queryString = charSequence?.toString()

                if (queryString == null || queryString.isEmpty()) {
                    filterResults.values = array
                } else {
                    filterResults.values = array.filter {
                        it!!.contains(queryString, true)
                    }
                }

                return filterResults
            }
        }
    }
}