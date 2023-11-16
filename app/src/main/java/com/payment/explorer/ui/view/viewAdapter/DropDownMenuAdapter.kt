package com.payment.explorer.ui.view.viewAdapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filterable
import android.widget.TextView

class DropDownMenuAdapter<T>(
    context: Context, private val layoutResource: Int,
    itemFullList: List<T>, private val unAssignItem: String? = null
) : ArrayAdapter<T>(context, layoutResource, itemFullList), Filterable {
    private var itemList: List<T?> = itemFullList
    private var selectedItem: T? = null
    override fun getCount(): Int {
        return itemList.size
    }

    override fun getItem(p0: Int): T? {
        return itemList[p0]
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: TextView = convertView as TextView? ?: LayoutInflater.from(context).inflate(layoutResource, parent, false) as TextView
        view.text = itemList[position].toString().ifEmpty {
            unAssignItem
        }
        selectedItem = itemList[position]
        return view
    }
}