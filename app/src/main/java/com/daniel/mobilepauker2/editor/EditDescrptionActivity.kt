package com.daniel.mobilepauker2.editor

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.core.PaukerManager
import com.daniel.mobilepauker2.pauker_native.ModelManager
import org.koin.core.KoinComponent
import org.koin.core.get

/**
 * Created by Daniel on 06.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */
class EditDescrptionActivity : AppCompatActivity(), KoinComponent {
    private var editText: EditText? = null
    private val modelManager: ModelManager? = get()

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