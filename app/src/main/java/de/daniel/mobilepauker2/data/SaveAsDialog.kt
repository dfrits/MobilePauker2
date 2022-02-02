package de.daniel.mobilepauker2.data

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.utils.Utility.Companion.hideKeyboard
import javax.inject.Inject

class SaveAsDialog(private val saveAsCallback: SaveAsCallback) :
    DialogFragment(R.layout.give_lesson_name_dialog) {

    init {
        setStyle(STYLE_NO_TITLE, R.style.NamingDialogTheme)
    }

    private lateinit var textField: EditText
    private lateinit var bOK: Button
    private lateinit var bCancel: Button
    private lateinit var errorHint: TextView

    @Inject
    lateinit var dataManager: DataManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireContext().applicationContext as PaukerApplication).applicationSingletonComponent
            .inject(this)

        initView(view)
    }

    private fun initView(view: View) {
        textField = view.findViewById(R.id.eTGiveLessonName)

        bCancel = view.findViewById(R.id.bCancel)
        bCancel.setOnClickListener {
            saveAsCallback.cancelClicked()
            finish()
        }

        bOK = view.findViewById(R.id.bOK)
        bOK.setOnClickListener {
            overwriteOK(textField.text.toString())
        }

        errorHint = view.findViewById(R.id.tFileExistingHint)

        addTextwatcher(textField)
    }

    private fun overwriteOK(fileName: String) {
        val testFile = dataManager.getFilePathForName(dataManager.setCorrectFileEnding(fileName))

        if (testFile.exists()) {
            AlertDialog.Builder(context)
                .setTitle(getString(R.string.warning))
                .setMessage(getString(R.string.overwrite_file_question))
                .setPositiveButton(R.string.yes) { _, _ -> fileNameChosen(fileName) }
                .setNeutralButton(R.string.no) { dialog, _ -> dialog.dismiss() }
                .create()
                .show()
        } else {
            fileNameChosen(fileName)
        }
    }

    private fun fileNameChosen(fileName: String) {
        saveAsCallback.okClicked(fileName)
        finish()
    }

    private fun finish() {
        hideKeyboard()
        activity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()
    }

    private fun addTextwatcher(textField: EditText) {
        textField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                var newName = s.toString()
                val isEmptyString = newName.isEmpty()

                newName = dataManager.setCorrectFileEnding(newName)

                val isValidName: Boolean = dataManager.isNameValid(newName)
                val isExisting: Boolean = dataManager.isFileExisting(newName)

                errorHint.visibility = if (isExisting) View.VISIBLE else View.GONE

                bOK.isEnabled = (!isEmptyString && isValidName)
            }
        })
        textField.setText("")
    }
}