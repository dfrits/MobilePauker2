package de.daniel.mobilepauker2.statistics

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.utils.Log

internal class ChartBar(view: View) {
    private val chart: BarChart = view.findViewById(R.id.chartBar)
    private val abgelaufenValue: TextView = view.findViewById(R.id.abgelaufenValue)
    private val gelerntValue: TextView = view.findViewById(R.id.gelerntValue)
    private val sumValue: TextView = view.findViewById(R.id.sumValue)

    /**
     * titel: Label (nicht sichtbar im Chart, du hast eigene TextViews)
     * sum, gelernt, ungelernt, abgelaufen: wie bisher (-1 = absent)
     * numAllCards: Gesamthöhe zur Berechnung diff (wie vorher)
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
        // Labels unter dem Chart wie bisher
        abgelaufenValue.text = if (abgelaufen == -1) "" else abgelaufen.toString()
        gelerntValue.text = if (gelernt == -1) "" else gelernt.toString()
        sumValue.text = if (sum == -1) "" else sum.toString()

        // Rohwerte (keine -1-Konvertierung zu >0 hier noch, wir prüfen separat)
        val ungelerntF = if (ungelernt > 0) ungelernt.toFloat() else 0f
        val gelerntF = if (gelernt > 0) gelernt.toFloat() else 0f
        val abgelaufenF = if (abgelaufen > 0) abgelaufen.toFloat() else 0f
        val diff = if (sum > -1 && sum != numAllCards) (numAllCards - sum) else 0
        val diffF = if (diff > 0) diff.toFloat() else 0f

        // echte Daten vorhanden? (mindestens ein >0 Wert)
        val hasAny = (ungelerntF + gelerntF + abgelaufenF + diffF) > 0f

        val entryValues: FloatArray
        val setColors = ArrayList<Int>()

        if (hasAny) {
            // normale Reihenfolge: ungelernt, gelernt, abgelaufen, diff
            entryValues = floatArrayOf(ungelerntF, gelerntF, abgelaufenF, diffF)

            // Farben in derselben Reihenfolge bereitstellen
            setColors.add(ContextCompat.getColor(context, R.color.unlearned))      // ungelernt
            setColors.add(ContextCompat.getColor(context, R.color.learned))        // gelernt
            setColors.add(ContextCompat.getColor(context, R.color.colorPrimary))   // abgelaufen
            setColors.add(ContextCompat.getColor(context, R.color.defaultBackground)) // diff / filler
        } else {
            // Kein Dateninhalt: minimaler Balken in Hintergrundfarbe (kein roter Balken!)
            entryValues = floatArrayOf(1f)
            setColors.add(ContextCompat.getColor(context, R.color.defaultBackground))
        }

        // Ein Balken (x = 0)
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, entryValues))

        // Dataset erstellen
        val set = BarDataSet(entries, "")
        set.colors = setColors
        set.setDrawValues(false)

        val data = BarData(set)
        data.barWidth = 0.6f

        // clear & set data
        chart.clear()
        chart.data = data

        // Chart Styling wie vorher
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.axisLeft.isEnabled = false
        chart.axisRight.isEnabled = false

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawLabels(false)
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)

        chart.setFitBars(true)
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.setDrawValueAboveBar(false)

        chart.invalidate()
        Log.d("ChartBar::show", "Chart updated")
    }
}
