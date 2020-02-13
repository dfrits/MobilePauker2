package com.daniel.mobilepauker2.lessonexport

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.daniel.mobilepauker2.core.PaukerManager
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.dropbox.SyncDialog
import com.daniel.mobilepauker2.pauker_native.ModelManager
import com.daniel.mobilepauker2.settings.SettingsManager
import com.daniel.mobilepauker2.settings.SettingsManager.Keys
import com.daniel.mobilepauker2.core.Constants
import java.io.File

/**
 * Created by dfritsch on 20.03.2018.
 * veesy.de
 * hs-augsburg
 */
@Suppress("UNUSED_PARAMETER")
class SaveDialog : Activity() {
    private val context: Context = this
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.progress_dialog)
        val progressBar = findViewById<RelativeLayout>(R.id.pFrame)
        progressBar.visibility = View.VISIBLE
        val title = findViewById<TextView>(R.id.pTitle)
        title.setText(R.string.saving_title)
        if (PaukerManager.instance().readableFileName == Constants.DEFAULT_FILE_NAME) {
            openDialog()
        } else {
            saveLesson()
        }
    }

    // Touchevents und Backbutton blockieren, dass er nicht minimiert werden kann
    override fun onBackPressed() {}

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        return false
    }

    private fun openDialog() {
        @SuppressLint("InflateParams") val view =
            layoutInflater.inflate(R.layout.give_lesson_name_dialog, null)
        val textField = view.findViewById<EditText>(R.id.eTGiveLessonName)
        val builder =
            AlertDialog.Builder(context, R.style.NamingDialogTheme)
        val paukerManager: PaukerManager = PaukerManager.instance()
        builder.setView(view)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
                if (paukerManager.setCurrentFileName(textField.text.toString())) {
                    ModelManager.instance().addLesson(context)
                    saveLesson()
                } else {
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
            .setNeutralButton(R.string.not_now) { dialog, _ ->
                dialog.dismiss()
                setResult(RESULT_CANCELED)
                finish()
            }
            .setOnDismissListener {
                val imm =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                if (currentFocus != null && imm.isAcceptingText) {
                    imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
                }
            }
            .setOnCancelListener { finish() }
        val dialog = builder.create()
        textField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                var newName = s.toString()
                val isEmptyString = newName.isNotEmpty()
                if (!newName.endsWith(".pau.gz")) newName = "$newName.pau.gz"
                val isValidName = paukerManager.isNameValid(newName)
                val isExisting = paukerManager.isFileExisting(context, newName)
                view.findViewById<View>(R.id.tFileExistingHint).visibility =
                    if (isExisting) View.VISIBLE else View.GONE
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = (isEmptyString
                        && isValidName
                        && !isExisting)
            }
        })
        dialog.show()
        textField.setText("")
    }

    private fun saveLesson() {
        val saveThread =
            SaveLessonThreaded(
                Handler(
                    Handler.Callback { msg ->
                        val result = msg.data
                            .getBoolean(Constants.MESSAGE_BOOL_KEY)
                        if (result) {
                            setResult(RESULT_OK)
                            if (SettingsManager.instance().getBoolPreference(
                                    context,
                                    Keys.AUTO_UPLOAD
                                )
                            ) {
                                uploadCurrentFile()
                            }
                        } else {
                            PaukerManager.showToast(
                                context as Activity,
                                R.string.saving_error,
                                Toast.LENGTH_SHORT
                            )
                            setResult(RESULT_CANCELED)
                        }
                        finish()
                        result
                    })
            )
        saveThread.run()
    }

    private fun uploadCurrentFile() {
        val accessToken = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Constants.DROPBOX_ACCESS_TOKEN, null)
        val intent = Intent(context, SyncDialog::class.java)
        intent.putExtra(SyncDialog.ACCESS_TOKEN, accessToken)
        val path: String? = PaukerManager.instance().fileAbsolutePath
        val file =
            path?.let { File(it) } ?: ModelManager.instance().filePath
        intent.putExtra(SyncDialog.FILES, file)
        intent.action = SyncDialog.UPLOAD_FILE_ACTION
        startActivity(intent)
    }

    fun cancelClicked(view: View?) {}
}