package com.daniel.mobilepauker2.statistics

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import com.daniel.mobilepauker2.R
import com.db.chart.model.BarSet
import com.db.chart.renderer.AxisRenderer
import com.db.chart.view.StackBarChartView

/**
 * Created by Daniel on 28.02.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */
internal class ChartBar(view: View, callback: ChartBarCallback?) {
    private val chart: StackBarChartView
    private val abglValue: TextView
    private val gelValue: TextView
    private val sumValue: TextView
    private val callback: ChartBarCallback?
    /**
     * Erstellt einen Balken mit den jeweiligen Farben und setzt die Textfelder darunter.
     * -1 bedeutet kein Balken und kein Label.
     * Ist aktuell keine Lektion geöffnet, wird kein Balken angezeigt und nur die Labels.
     * @param context     Context der aufrufenden Aktivity um Farben zu setzen
     * @param titel       Der Titel des Balken
     * @param sum         Gesamtzahl aller Karten in diesem Stapel. Wird nur als Label angezeigt
     * @param gel         Anzahl der gelernten Karten im Stapel. Wird als grüner Teil im Balken
     * angezeigt und als Label
     * @param ungel       Anzahl der ungelernten Karten im Stapel. Wird nur als roter Teil Teil im
     * Balken angezeigt
     * @param abgl        Anzahl der abgelaufenen Karten im Stapel. Wird als Blauer Teil im Balken
     * @param numAllCards Gesamtzahl Aller Karten in der Lektion. Ist die gleiche Zahl wie
     * **sum** vom ersten Balken. Wird benötigt, um die Gesamthöhe des Balken
     */
    fun show(
        context: Context,
        titel: String,
        sum: Int,
        gel: Int,
        ungel: Int,
        abgl: Int,
        numAllCards: Int
    ) { // String zu einem Array ändern
        val titelLabel = arrayOf(titel)
        // Labels setzen
        var text = if (abgl == -1) "" else abgl.toString()
        abglValue.text = text
        text = if (gel == -1) "" else gel.toString()
        gelValue.text = text
        text = if (sum == -1) "" else sum.toString()
        sumValue.text = text
        // Bar initialisieren und zum DataSet hinzufügen
        var stackBarSet: BarSet
        if (ungel != -1) {
            stackBarSet = BarSet(titelLabel, floatArrayOf(ungel.toFloat()))
            stackBarSet.color = context.getColor(R.color.unlearned)
            chart.addData(stackBarSet)
        }
        if (gel != -1) {
            stackBarSet = BarSet(titelLabel, floatArrayOf(gel.toFloat()))
            stackBarSet.color = context.getColor(R.color.learned)
            chart.addData(stackBarSet)
        }
        if (abgl != -1) {
            stackBarSet = BarSet(titelLabel, floatArrayOf(abgl.toFloat()))
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
        // Details setzen und anzeigen
        chart.setXLabels(AxisRenderer.LabelPosition.OUTSIDE)
            .setYLabels(AxisRenderer.LabelPosition.NONE)
            .setTypeface(Typeface.DEFAULT_BOLD)
            .setOnClickListener { callback?.onClick() }
        chart.show()
    }

    interface ChartBarCallback {
        fun onClick()
    }

    init {
        chart = view.findViewById(R.id.chartBar)
        abglValue = view.findViewById(R.id.abgelaufenValue)
        gelValue = view.findViewById(R.id.gelerntValue)
        sumValue = view.findViewById(R.id.sumValue)
        this.callback = callback
    }
}