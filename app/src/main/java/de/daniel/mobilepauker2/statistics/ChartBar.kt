package de.daniel.mobilepauker2.statistics

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import com.db.chart.model.BarSet
import com.db.chart.renderer.AxisRenderer
import com.db.chart.view.StackBarChartView
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.utils.Log

internal class ChartBar(view: View) {
    private val chart: StackBarChartView = view.findViewById(R.id.chartBar)
    private val abgelaufenValue: TextView = view.findViewById(R.id.abgelaufenValue)
    private val gelerntValue: TextView = view.findViewById(R.id.gelerntValue)
    private val sumValue: TextView = view.findViewById(R.id.sumValue)

    /**
     * Erstellt einen Balken mit den jeweiligen Farben und setzt die Textfelder darunter.
     * -1 bedeutet kein Balken und kein Label.
     * Ist aktuell keine Lektion geöffnet, wird kein Balken angezeigt und nur die Labels.
     * @param context     Context der aufrufenden Aktivity um Farben zu setzen
     * @param titel       Der Titel des Balken
     * @param sum         Gesamtzahl aller Karten in diesem Stapel. Wird nur als Label angezeigt
     * @param gelernt         Anzahl der gelernten Karten im Stapel. Wird als grüner Teil im Balken
     * angezeigt und als Label
     * @param ungelernt       Anzahl der ungelernten Karten im Stapel. Wird nur als roter Teil Teil im
     * Balken angezeigt
     * @param abgelaufen        Anzahl der abgelaufenen Karten im Stapel. Wird als Blauer Teil im Balken
     * @param numAllCards Gesamtzahl Aller Karten in der Lektion. Ist die gleiche Zahl wie
     * **sum** vom ersten Balken. Wird benötigt, um die Gesamthöhe des Balken
     */
    fun show(
        context: Context,
        titel: String,
        sum: Int,
        gelernt: Int,
        ungelernt: Int,
        abgelaufen: Int,
        numAllCards: Int
    ) {
        val titelLabel = arrayOf(titel)

        var text = if (abgelaufen == -1) "" else abgelaufen.toString()
        abgelaufenValue.text = text
        text = if (gelernt == -1) "" else gelernt.toString()
        gelerntValue.text = text
        text = if (sum == -1) "" else sum.toString()
        sumValue.text = text

        var stackBarSet: BarSet
        if (ungelernt != -1) {
            stackBarSet = BarSet(titelLabel, floatArrayOf(ungelernt.toFloat()))
            stackBarSet.color = context.getColor(R.color.unlearned)
            chart.addData(stackBarSet)
        }
        if (gelernt != -1) {
            stackBarSet = BarSet(titelLabel, floatArrayOf(gelernt.toFloat()))
            stackBarSet.color = context.getColor(R.color.learned)
            chart.addData(stackBarSet)
        }
        if (abgelaufen != -1) {
            stackBarSet = BarSet(titelLabel, floatArrayOf(abgelaufen.toFloat()))
            stackBarSet.color = context.getColor(R.color.colorPrimary)
            chart.addData(stackBarSet)
        }
        if (sum > -1 && sum != numAllCards) {
            val diff = numAllCards - sum
            stackBarSet = BarSet(titelLabel, floatArrayOf(diff.toFloat()))
            stackBarSet.color = context.getColor(R.color.defaultBackground)
            chart.addData(stackBarSet)
        }

        // Sind alle Werte null, wird ein Mindestwert gesetzt, da sonst eine Exception geworfen wird
        if (sum == 0) {
            chart.reset()
            stackBarSet = BarSet(titelLabel, floatArrayOf(1f))
            stackBarSet.color = context.getColor(R.color.defaultBackground)
            chart.addData(stackBarSet)
        }

        chart.setXLabels(AxisRenderer.LabelPosition.OUTSIDE)
            .setYLabels(AxisRenderer.LabelPosition.NONE)
            .setTypeface(Typeface.DEFAULT_BOLD)

        //chart.isClickable = true
        /*chart.setOnClickListener {
            Log.d("ChartBar::showBar", "On Bar clicked")
            callback.onClick()
        }*/
        Log.d("ChartBar::showBar", "Set Callback")

        chart.show()
    }

    interface ChartBarCallback {
        fun onClick()
    }
}