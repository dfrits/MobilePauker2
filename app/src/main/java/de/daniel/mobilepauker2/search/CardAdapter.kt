package de.daniel.mobilepauker2.search

import android.app.Activity
import android.content.Context
import android.text.format.DateFormat
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.lesson.card.FlashCard
import de.daniel.mobilepauker2.models.view.MPTextView
import java.util.*

class CardAdapter(context: Context, private val items: List<FlashCard>) :
    ArrayAdapter<FlashCard>(context, R.layout.search_result, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var v = convertView
        if (v == null) {
            val inflater = (context as Activity).layoutInflater
            v = inflater.inflate(R.layout.search_result, parent, false)
        }
        v?.let {
            val card: FlashCard = items[position]
            val sideA: MPTextView = v.findViewById(R.id.tCardSideA)
            val sideB: MPTextView = v.findViewById(R.id.tCardSideB)
            val learnedAt = v.findViewById<TextView>(R.id.tLearnedTime)
            val expireAt = v.findViewById<TextView>(R.id.tExpireTime)
            val stackNumber = v.findViewById<TextView>(R.id.tStackNumber)
            val repeatType = v.findViewById<ImageView>(R.id.iRepeatType)

            sideA.setCard(card.frontSide)
            sideB.setCard(card.reverseSide)

            // learnedAt und stackNumber
            val learnedTime: Long = card.learnedTimestamp
            if (learnedTime != 0L) {
                val cal = Calendar.getInstance(Locale.getDefault())
                cal.timeInMillis = learnedTime
                val date = DateFormat.format("dd.MM.yyyy HH:mm", cal).toString()
                var text = context.getString(R.string.learned_at) + " " + date
                learnedAt.text = text
                val stack: Int = card.longTermBatchNumber
                stackNumber.visibility = View.VISIBLE
                text = context.getString(R.string.stack) + " " + (stack + 1).toString()
                stackNumber.text = text
            }

            // expireAt
            val expirationTime = card.getExpirationTime()
            if (expirationTime != -1L) {
                expireAt.visibility = View.VISIBLE
                val cal = Calendar.getInstance(Locale.getDefault())
                cal.timeInMillis = expirationTime
                val date = DateFormat.format("dd.MM.yyyy HH:mm", cal).toString()
                var text: String = if (expirationTime < System.currentTimeMillis()) {
                    context.getString(R.string.expired_at)
                } else {
                    context.getString(R.string.expire_at)
                }
                text = "$text $date"
                expireAt.text = text
            }

            // repeatType
            val drawable =
                if (card.isRepeatedByTyping) R.drawable.rt_typing else R.drawable.rt_thinking
            repeatType.setImageResource(drawable)

            return v
        }

        return View(context)
    }
}