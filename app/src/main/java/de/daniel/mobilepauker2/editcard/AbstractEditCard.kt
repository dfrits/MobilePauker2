package de.daniel.mobilepauker2.editcard

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog.Builder
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.rarepebble.colorpicker.ColorPickerView
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.lesson.card.FlashCard
import de.daniel.mobilepauker2.models.Font
import de.daniel.mobilepauker2.models.view.MPEditText
import de.daniel.mobilepauker2.models.view.TextDrawable
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.Toaster
import de.daniel.mobilepauker2.utils.Utility.Companion.hideKeyboard
import de.daniel.mobilepauker2.utils.Utility.Companion.showKeyboard
import javax.inject.Inject

abstract class AbstractEditCard : AppCompatActivity(R.layout.edit_card) {
    private var fontChanged = false
    protected val context: Context = this
    protected lateinit var flashCard: FlashCard

    //SideA
    protected lateinit var sideAEditText: MPEditText
    protected var initSideAText = ""
    protected var initSideATSize = 0
    protected var initSideATColor = 0
    protected var initSideABColor = 0
    protected var initSideABold = false
    protected var initSideAItalic = false
    protected var initIsRepeatedByTyping = false

    //SideB
    protected lateinit var sideBEditText: MPEditText
    protected var initSideBText = ""
    protected var initSideBTSize = 0
    protected var initSideBTColor = 0
    protected var initSideBBColor = 0
    protected var initSideBBold = false
    protected var initSideBItalic = false

    @Inject
    lateinit var toaster: Toaster

    @Inject
    lateinit var lessonManager: LessonManager

    @Inject
    lateinit var dataManager: DataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)

        sideAEditText = findViewById(R.id.eTSideA)
        sideBEditText = findViewById(R.id.eTSideB)
        fontChanged = false
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard()
    }

    override fun onBackPressed() {
        resetCardAndFinish()
    }

    open fun okClicked(view: View?) {}

    open fun resetCardSides(view: View? = null) {
        fontChanged = false
        flashCard.setRepeatByTyping(initIsRepeatedByTyping)
        sideAEditText.setText(initSideAText)
        sideBEditText.setText(initSideBText)

        flashCard.frontSide.font?.let { font ->
            font.setSize(initSideATSize)
            font.setBackground(initSideABColor)
            font.textColor = initSideATColor
            font.isBold = initSideABold
            font.isItalic = initSideAItalic
            sideAEditText.setFont(font)
        }

        flashCard.reverseSide.font?.let { sideBEditText.setFont(it) }
        sideAEditText.requestFocus()
        sideAEditText.setSelection(initSideAText.length, initSideAText.length)
    }

    fun editFontA(view: View?) {
        val popupMenu: PopupMenu = createPopupMenu(view, flashCard.frontSide.font, true)
        popupMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
            var font: Font? = flashCard.frontSide.font
            if (font == null) {
                font = Font()
                flashCard.frontSide.font = font
                fontChanged = true
            }
            setNewFontDetails(item, font, sideAEditText)
        })
    }

    fun editFontB(view: View?) {
        val popupMenu: PopupMenu = createPopupMenu(view, flashCard.reverseSide.font, false)
        popupMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
            var font: Font? = flashCard.reverseSide.font
            if (font == null) {
                font = Font()
                flashCard.reverseSide.font = font
                fontChanged = true
            }
            setNewFontDetails(item, font, sideBEditText)
        })
    }

    protected open fun resetCardAndFinish() {
        resetCardSides()
        finish()
    }

    protected open fun detectChanges(): Boolean {
        return (sideAEditText.text.toString() != initSideAText
                || sideBEditText.text.toString() != initSideBText
                || fontChanged)
    }

    private fun setNewFontDetails(item: MenuItem, font: Font, cardSide: MPEditText): Boolean {
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
                        .getInt(Constants.LAST_BACK_COLOR_CHOICE, font.backgroundColor)
                )
                bcPicker.showAlpha(false)
                val bcBuilder: Builder = Builder(context)
                bcBuilder.setView(bcPicker)
                    .setTitle(R.string.background)
                    .setPositiveButton(R.string.ok,
                        DialogInterface.OnClickListener { _, _ ->
                            val color: Int = bcPicker.getColor()
                            PreferenceManager.getDefaultSharedPreferences(context).edit()
                                .putInt(Constants.LAST_BACK_COLOR_CHOICE, color).apply()
                            font.setBackground(color)
                            cardSide.setFont(font)
                            fontChanged = true
                        })
                    .setNeutralButton(R.string.cancel, null)
                bcBuilder.create().show()
            }
            R.id.mTextColor -> {
                val tcPicker = ColorPickerView(context)
                tcPicker.showHex(false)
                tcPicker.setOriginalColor(font.textColor)
                tcPicker.setCurrentColor(
                    PreferenceManager.getDefaultSharedPreferences(context)
                        .getInt(Constants.LAST_TEXT_COLOR_CHOICE, font.textColor)
                )
                tcPicker.showAlpha(false)
                val tcBuilder: Builder = Builder(context)
                tcBuilder.setView(tcPicker)
                    .setTitle(R.string.text_color)
                    .setPositiveButton(R.string.ok,
                        DialogInterface.OnClickListener { _, _ ->
                            val color: Int = tcPicker.getColor()
                            PreferenceManager.getDefaultSharedPreferences(context).edit()
                                .putInt(Constants.LAST_TEXT_COLOR_CHOICE, color).apply()
                            font.textColor = color
                            cardSide.setFont(font)
                            fontChanged = true
                        })
                    .setNeutralButton(R.string.cancel, null)
                tcBuilder.create().show()
            }
            R.id.mTextSize -> editTextSize(cardSide, font)
            R.id.mRepeatType -> {
                fontChanged = true
                flashCard.setRepeatByTyping(!flashCard.isRepeatedByTyping)
            }
            else -> return false
        }
        cardSide.setFont(font)
        return true
    }

    @SuppressLint("InflateParams")
    private fun editTextSize(cardSide: MPEditText, font: Font) {
        val view = layoutInflater.inflate(R.layout.edit_text_size_dialog, null) as EditText
        val oldSize: String = java.lang.String.valueOf(font.textSize)
        view.setText(oldSize)
        view.setSelection(0, oldSize.length)
        val builder = Builder(context)
        builder.setView(view)
            .setTitle(getString(R.string.text_size))
            .setPositiveButton(R.string.ok) { dialog, which ->
                val s = view.text.toString()
                try {
                    val newSize = s.toInt()
                    if (newSize != font.textSize) {
                        font.setSize(s.toInt())
                    }
                    cardSide.setFont(font)
                    fontChanged = true
                } catch (e: NumberFormatException) {
                    toaster.showToast(
                        context as Activity, R.string.number_format_error, Toast.LENGTH_SHORT
                    )
                }
            }
            .setNeutralButton(
                R.string.cancel
            ) { dialog, which -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.setOnShowListener {
            showKeyboard()
        }
        dialog.show()
    }

    private fun createPopupMenu(view: View?, font: Font?, showRepeatTypeMenu: Boolean): PopupMenu {
        val dropdownMenu = PopupMenu(context, view)
        val menu = dropdownMenu.menu
        dropdownMenu.menuInflater.inflate(R.menu.edit_font_pop_up, menu)
        setIcons(menu, font, showRepeatTypeMenu)
        try {
            val fields = dropdownMenu.javaClass.declaredFields
            for (field in fields) {
                if ("mPopup" == field.name) {
                    field.isAccessible = true
                    val menuPopupHelper = field[dropdownMenu]
                    val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
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

    private fun setIcons(menu: Menu, font: Font?, showRepeatTypeMenu: Boolean) {
        val font1 = font ?: Font()

        // Size
        var circle = TextDrawable(java.lang.String.valueOf(font1.textSize))
        menu.findItem(R.id.mTextSize).icon = circle

        // Background
        circle = TextDrawable(font1.backgroundColor)
        menu.findItem(R.id.mBackground).icon = circle

        // TextColor
        circle = TextDrawable(font1.textColor)
        menu.findItem(R.id.mTextColor).icon = circle

        // Bold
        circle = TextDrawable("B")
        circle.setBold(font1.isBold)
        menu.findItem(R.id.mBold).icon = circle

        // Italic
        circle = TextDrawable("I")
        circle.setItalic(font1.isItalic)
        menu.findItem(R.id.mItalic).icon = circle

        // Repeat Type --> Nur bei der Vorderseite
        if (showRepeatTypeMenu) {
            val item = menu.findItem(R.id.mRepeatType)
            item.isVisible = true
            val icon =
                if (flashCard.isRepeatedByTyping) ContextCompat.getDrawable(context, R.drawable.rt_typing)
                else ContextCompat.getDrawable(context, R.drawable.rt_thinking)
            item.icon = icon
        }
    }
}