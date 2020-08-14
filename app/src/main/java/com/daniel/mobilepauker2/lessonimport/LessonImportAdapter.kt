package com.daniel.mobilepauker2.lessonimport

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.daniel.mobilepauker2.core.PaukerManager

/**
 * Created by Daniel on 05.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */
class LessonImportAdapter(
        context: Context,
        private val data: List<String>
) : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, data) {
    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val view = super.getView(position, convertView, parent)
        if (view.isSelected) {
            view.setBackgroundColor(Color.LTGRAY)
        } else {
            view.setBackgroundColor(Color.TRANSPARENT)
        }
        val tv = view.findViewById<TextView>(android.R.id.text1)
        val name: String? = PaukerManager.instance().getReadableFileName(data[position])
        tv.text = name
        return view
    }

}