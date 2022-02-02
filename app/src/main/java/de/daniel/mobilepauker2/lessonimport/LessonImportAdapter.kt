package de.daniel.mobilepauker2.lessonimport

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.Nullable
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.data.DataManager
import javax.inject.Inject

class LessonImportAdapter(context: Context, private val data: List<String>) :
    ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, data) {

    @Inject
    lateinit var dataManager: DataManager

    override fun getView(position: Int, @Nullable convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        if (view.isSelected) {
            view.setBackgroundColor(Color.LTGRAY)
        } else {
            view.setBackgroundColor(Color.TRANSPARENT)
        }
        val tv = view.findViewById<TextView>(android.R.id.text1)
        val name: String = dataManager.getReadableFileName(data[position])
        tv.text = name
        return view
    }

    init {
        (context.applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)
    }
}