package de.daniel.mobilepauker2.editcard

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.models.Font
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.Log

class EditCard : AbstractEditCard() {
    private var cardPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cardPosition = intent.getIntExtra(Constants.CURSOR_POSITION, -1)

        if (cardPosition < 0) {
            Log.w("EditCardsActivity::OnCreate", "Card Position null $cardPosition")
        } else {
            val card = lessonManager.getCardFromCurrentPack(cardPosition)
            if (card == null) {
                Log.w("EditCardsActivity::OnCreate", "Flash Card set to null")
                toaster.showToast(
                    context as Activity,
                    getString(R.string.edit_cards_no_card_available),
                    Toast.LENGTH_SHORT
                )
                finish()
            } else {
                flashCard = card
                initActivity()
            }
        }
    }

    private fun initActivity() {
        //SideA
        var text: String = flashCard.sideAText
        var font: Font? = flashCard.frontSide.font
        font = font ?: Font()
        initSideAText = text
        initSideATSize = font.textSize
        initSideATColor = font.textColor
        initSideABColor = font.backgroundColor
        initSideABold = font.isBold
        initSideAItalic = font.isItalic
        sideAEditText.setCard(flashCard.frontSide)
        initIsRepeatedByTyping = flashCard.isRepeatedByTyping

        //SideB
        text = flashCard.sideBText
        font = flashCard.reverseSide.font
        font = font ?: Font()
        initSideBText = text
        initSideBTSize = font.textSize
        initSideBTColor = font.textColor
        initSideBBColor = font.backgroundColor
        initSideBBold = font.isBold
        initSideBItalic = font.isItalic
        sideBEditText.setCard(flashCard.reverseSide)
    }

    override fun okClicked(view: View?) {
        if (sideAEditText.text.toString().trim().isEmpty()
            || sideBEditText.text.toString().trim().isEmpty()
        ) {
            toaster.showToast(
                context as Activity,
                R.string.add_card_sides_empty_error,
                Toast.LENGTH_SHORT
            )
            return
        }
        if (cardPosition >= 0) {
            lessonManager.editCard(
                cardPosition,
                sideAEditText.text.toString(),
                sideBEditText.text.toString()
            )
            if (detectChanges()) {
                dataManager.saveRequired = true
                setResult(RESULT_OK)
            }
            finish()
        }
    }
}