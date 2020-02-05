package com.daniel.mobilepauker2.activities

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.daniel.mobilepauker2.PaukerManager
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.model.ModelManager
import com.daniel.mobilepauker2.model.pauker_native.Font
import com.daniel.mobilepauker2.utils.Constants
import com.daniel.mobilepauker2.utils.Log

/**
 * Created by dfritsch on 22.03.2018.
 */
class EditCardActivity : AEditCardActivity() {
    private var cardPosition = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cardPosition =
            intent.getIntExtra(Constants.CURSOR_POSITION, -1)
        if (cardPosition < 0) {
            Log.w(
                "EditCardsActivity::OnCreate",
                "Card Position null $cardPosition"
            )
        } else {
            flashCard = ModelManager.Companion.instance()!!.getCard(cardPosition)
        }
        if (flashCard == null) {
            Log.w(
                "EditCardsActivity::OnCreate",
                "Flash Card set to null"
            )
            PaukerManager.Companion.showToast(
                context as Activity,
                getString(R.string.edit_cards_no_card_available),
                Toast.LENGTH_SHORT
            )
            finish()
        } else {
            init()
        }
    }

    private fun init() { //SideA
        var text = flashCard.sideAText
        var font =
            flashCard.frontSide.font
        font = font ?: Font()
        initSideAText = text!!
        initSideATSize = font.textSize
        initSideATColor = font.textColor
        initSideABColor = font.backgroundColor
        initSideABold = font.isBold
        initSideAItalic = font.isItalic
        sideAEditText!!.setCard(flashCard.frontSide)
        initIsRepeatedByTyping = flashCard!!.isRepeatedByTyping
        //SideB
        text = flashCard.sideBText
        font = flashCard.reverseSide.font
        font = font ?: Font()
        initSideBText = text!!
        initSideBTSize = font.textSize
        initSideBTColor = font.textColor
        initSideBBColor = font.backgroundColor
        initSideBBold = font.isBold
        initSideBItalic = font.isItalic
        sideBEditText!!.setCard(flashCard.reverseSide)
    }

    override fun okClicked(view: View?) {
        if (sideAEditText!!.text.toString().trim { it <= ' ' }.isEmpty() || sideBEditText!!.text.toString().trim { it <= ' ' }.isEmpty()) {
            PaukerManager.Companion.showToast(
                context as Activity,
                R.string.add_card_sides_empty_error,
                Toast.LENGTH_SHORT
            )
            return
        }
        if (cardPosition >= 0) {
            ModelManager.Companion.instance()!!.editCard(
                cardPosition,
                sideAEditText!!.text.toString(),
                sideBEditText!!.text.toString()
            )
            if (detectChanges()) {
                PaukerManager.Companion.instance().setSaveRequired(true)
                setResult(Activity.RESULT_OK)
            }
            finish()
        }
    }
}