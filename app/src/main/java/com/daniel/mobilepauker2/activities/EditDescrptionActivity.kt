package com.daniel.mobilepauker2.activities

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.daniel.mobilepauker2.PaukerManager
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.model.ModelManager

/**
 * Created by Daniel on 06.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */
class EditDescrptionActivity : AppCompatActivity() {
    private var editText: EditText? = null
    private val modelManager: ModelManager? = ModelManager.instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_description)
        editText = findViewById(R.id.editField)
        editText?.setText(modelManager?.description)
    }

    override fun onResume() {
        super.onResume()
        if (editText != null) editText?.setText(modelManager?.description)
    }

    override fun onPause() {
        super.onPause()
        var text: String? = null
        if (editText != null) {
            text = editText!!.text.toString().trim { it <= ' ' }
        }
        if (text != null && modelManager?.description != text) {
            modelManager?.description = text
            PaukerManager.instance().isSaveRequired = true
        }
    }

    override fun onStop() {
        super.onStop()
        overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom)
    }
}