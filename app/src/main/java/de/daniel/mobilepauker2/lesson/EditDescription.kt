package de.daniel.mobilepauker2.lesson

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.data.DataManager
import javax.inject.Inject

class EditDescription : AppCompatActivity() {
    private var editText: EditText? = null

    @Inject
    lateinit var lessonManager: LessonManager

    @Inject
    lateinit var dataManager: DataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)

        setContentView(R.layout.edit_description)
        editText = findViewById(R.id.editField)
        editText?.setText(lessonManager.lessonDescription)
    }

    override fun onResume() {
        super.onResume()
        editText?.setText(lessonManager.lessonDescription)
    }

    override fun onPause() {
        super.onPause()
        val text: String = editText?.text.toString().trim { it <= ' ' }

        if (lessonManager.lessonDescription != text) {
            lessonManager.lessonDescription = text
            dataManager.saveRequired = true
        }
    }

    override fun onStop() {
        super.onStop()
        overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom)
    }
}