package com.daniel.mobilepauker2.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.daniel.mobilepauker2.PaukerManager
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.model.FlashCard
import com.daniel.mobilepauker2.model.MPEditText
import com.daniel.mobilepauker2.model.ModelManager
import com.daniel.mobilepauker2.model.TextDrawable
import com.daniel.mobilepauker2.model.pauker_native.Font
import com.daniel.mobilepauker2.utils.Constants
import com.rarepebble.colorpicker.ColorPickerView

/**
 * Created by dfritsch on 21.11.2018.
 * MobilePauker++
 */
abstract class AEditCardActivity : AppCompatActivity() {
    private var fontChanged = false
    protected val context: Context = this
    protected val modelManager: ModelManager? = ModelManager.Companion.instance()
    protected var flashCard: FlashCard? = null
    //SideA
    protected var sideAEditText: MPEditText? = null
    protected var initSideAText = ""
    protected var initSideATSize = 0
    protected var initSideATColor = 0
    protected var initSideABColor = 0
    protected var initSideABold = false
    protected var initSideAItalic = false
    protected var initIsRepeatedByTyping = false
    //SideB
    protected var sideBEditText: MPEditText? = null
    protected var initSideBText = ""
    protected var initSideBTSize = 0
    protected var initSideBTColor = 0
    protected var initSideBBColor = 0
    protected var initSideBBold = false
    protected var initSideBItalic = false
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_card)
        sideAEditText = findViewById(R.id.eTSideA)
        sideBEditText = findViewById(R.id.eTSideB)
        fontChanged = false
    }

    override fun onPause() {
        super.onPause()
        val imm =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (imm != null && currentFocus != null && imm.isAcceptingText) {
            imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
    }

    override fun onBackPressed() {
        resetCardAndFinish()
    }

    protected fun resetCardAndFinish() {
        resetCardSides(null)
        finish()
    }

    /**
     * Überprüft beide Felder ob Änderungen zum Initialstatus vorhanden sind. Die Schriftart wird
     * dabei nicht überprüft.
     * @return True, wenn Unterschiede gefunden wurden
     */
    protected fun detectChanges(): Boolean {
        return (sideAEditText!!.text.toString() != initSideAText
                || sideBEditText!!.text.toString() != initSideBText
                || fontChanged)
    }

    open fun okClicked(view: View?) {}
    open fun resetCardSides(view: View?) {
        fontChanged = false
        flashCard?.setRepeatByTyping(initIsRepeatedByTyping)
        sideAEditText?.setText(initSideAText)
        sideBEditText?.setText(initSideBText)
        var font = flashCard?.frontSide?.font
        if (font != null) {
            font.setSize(initSideATSize)
            font.setBackground(initSideABColor)
            font.textColor = initSideATColor
            font.isBold = initSideABold
            font.isItalic = initSideAItalic
        }
        sideAEditText?.setFont(font)
        font = flashCard?.reverseSide?.font
        if (font != null) {
            font.setSize(initSideBTSize)
            font.setBackground(initSideBBColor)
            font.textColor = initSideBTColor
            font.isBold = initSideBBold
            font.isItalic = initSideBItalic
        }
        sideBEditText!!.setFont(font)
        sideAEditText!!.requestFocus()
        sideAEditText!!.setSelection(initSideAText.length, initSideAText.length)
    }

    fun editFontA(view: View) {
        val popupMenu =
            createPopupMenu(view, flashCard?.frontSide?.font, true)
        popupMenu.setOnMenuItemClickListener { item ->
            var font =
                flashCard?.frontSide?.font
            if (font == null) {
                font = Font()
                flashCard?.frontSide?.font = font
                fontChanged = true
            }
            setNewFontDetails(item, font, sideAEditText)
        }
    }

    fun editFontB(view: View) {
        val popupMenu =
            createPopupMenu(view, flashCard?.reverseSide?.font, false)
        popupMenu.setOnMenuItemClickListener { item ->
            var font =
                flashCard?.reverseSide?.font
            if (font == null) {
                font = Font()
                flashCard?.reverseSide?.font = font
                fontChanged = true
            }
            setNewFontDetails(item, font, sideBEditText)
        }
    }

    private fun setNewFontDetails(
        item: MenuItem,
        font: Font,
        cardSide: MPEditText?
    ): Boolean {
        when (item.itemId) {
            R.id.mBold -> {
                font.isBold = !font.isBold
                fontChanged = true
            }
            R.id.mItalic -> {
                font.isItalic = !font.isItalic
                fontChanged = true
            }
            R.id.mBackground -> {
                val bcPicker = ColorPickerView(context)
                bcPicker.showHex(false)
                bcPicker.setOriginalColor(font.backgroundColor)
                bcPicker.setCurrentColor(
                    PreferenceManager.getDefaultSharedPreferences(context)
                        .getInt(
                            Constants.LAST_BACK_COLOR_CHOICE,
                            font.backgroundColor
                        )
                )
                bcPicker.showAlpha(false)
                val bcBuilder =
                    AlertDialog.Builder(context)
                bcBuilder.setView(bcPicker)
                    .setTitle(R.string.background)
                    .setPositiveButton(R.string.ok) { dialog, which ->
                        val color = bcPicker.color
                        PreferenceManager.getDefaultSharedPreferences(context).edit()
                            .putInt(
                                Constants.LAST_BACK_COLOR_CHOICE,
                                color
                            ).apply()
                        font.setBackground(color)
                        cardSide!!.setFont(font)
                        fontChanged = true
                    }
                    .setNeutralButton(R.string.cancel, null)
                bcBuilder.create().show()
            }
            R.id.mTextColor -> {
                val tcPicker = ColorPickerView(context)
                tcPicker.showHex(false)
                tcPicker.setOriginalColor(font.textColor)
                tcPicker.setCurrentColor(
                    PreferenceManager.getDefaultSharedPreferences(context)
                        .getInt(
                            Constants.LAST_TEXT_COLOR_CHOICE,
                            font.textColor
                        )
                )
                tcPicker.showAlpha(false)
                val tcBuilder =
                    AlertDialog.Builder(context)
                tcBuilder.setView(tcPicker)
                    .setTitle(R.string.text_color)
                    .setPositiveButton(R.string.ok) { dialog, which ->
                        val color = tcPicker.color
                        PreferenceManager.getDefaultSharedPreferences(context).edit()
                            .putInt(
                                Constants.LAST_TEXT_COLOR_CHOICE,
                                color
                            ).apply()
                        font.textColor = color
                        cardSide!!.setFont(font)
                        fontChanged = true
                    }
                    .setNeutralButton(R.string.cancel, null)
                tcBuilder.create().show()
            }
            R.id.mTextSize -> editTextSize(cardSide, font)
            R.id.mRepeatType -> {
                fontChanged = true
                flashCard!!.setRepeatByTyping(!flashCard!!.isRepeatedByTyping)
            }
            else -> return false
        }
        cardSide!!.setFont(font)
        return true
    }

    @SuppressLint("InflateParams")
    private fun editTextSize(
        cardSide: MPEditText?,
        font: Font
    ) {
        val view = layoutInflater
            .inflate(R.layout.edit_text_size_dialog, null) as EditText
        val oldSize = font.textSize.toString()
        view.setText(oldSize)
        view.setSelection(0, oldSize.length)
        val builder =
            AlertDialog.Builder(context)
        builder.setView(view)
            .setTitle(getString(R.string.text_size))
            .setPositiveButton(R.string.ok) { dialog, which ->
                val s = view.text.toString()
                try {
                    val newSize = s.toInt()
                    if (newSize != font.textSize) {
                        font.setSize(s.toInt())
                    }
                    cardSide!!.setFont(font)
                    fontChanged = true
                } catch (e: NumberFormatException) {
                    PaukerManager.Companion.showToast(
                        context as Activity, R.string.number_format_error
                        , Toast.LENGTH_SHORT
                    )
                }
            }
            .setNeutralButton(R.string.cancel) { dialog, which -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.setOnShowListener {
            val imm =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm?.showSoftInput(view, 0)
        }
        dialog.show()
    }

    private fun createPopupMenu(
        view: View,
        font: Font?,
        showRepeatTypeMenu: Boolean
    ): PopupMenu {
        val dropdownMenu = PopupMenu(context, view)
        val menu = dropdownMenu.menu
        dropdownMenu.menuInflater.inflate(R.menu.edit_font_pop_up, menu)
        setIcons(menu, font, showRepeatTypeMenu)
        try {
            val fields =
                dropdownMenu.javaClass.declaredFields
            for (field in fields) {
                if ("mPopup" == field.name) {
                    field.isAccessible = true
                    val menuPopupHelper = field[dropdownMenu]
                    val classPopupHelper =
                        Class.forName(menuPopupHelper.javaClass.name)
                    val setForceIcons = classPopupHelper.getMethod(
                        "setForceShowIcon",
                        Boolean::class.javaPrimitiveType
                    )
                    setForceIcons.invoke(menuPopupHelper, true)
                    break
                }
            }
        } catch (e: Exception) {
            return dropdownMenu
        }
        dropdownMenu.show()
        return dropdownMenu
    }

    private fun setIcons(
        menu: Menu,
        font: Font?,
        showRepeatTypeMenu: Boolean
    ) {
        var font = font
        font = font ?: Font()
        // Size
        var circle = TextDrawable(font.textSize.toString())
        menu.findItem(R.id.mTextSize).icon = circle
        // Background
        circle = TextDrawable(font.backgroundColor)
        menu.findItem(R.id.mBackground).icon = circle
        // TextColor
        circle = TextDrawable(font.textColor)
        menu.findItem(R.id.mTextColor).icon = circle
        // Bold
        circle = TextDrawable("B")
        circle.setBold(font.isBold)
        menu.findItem(R.id.mBold).icon = circle
        // Italic
        circle = TextDrawable("I")
        circle.setItalic(font.isItalic)
        menu.findItem(R.id.mItalic).icon = circle
        // Repeat Type --> Nur bei der Vorderseite
        if (showRepeatTypeMenu) {
            val item = menu.findItem(R.id.mRepeatType)
            item.isVisible = true
            val icon =
                if (flashCard!!.isRepeatedByTyping) getDrawable(R.drawable.rt_typing) else getDrawable(
                    R.drawable.rt_thinking
                )
            item.icon = icon
        }
    }
}