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

        // Bereite die Stack-Werte in der gewünschten Reihenfolge vor.
        // Wir verwenden die Reihenfolge: ungelernt (rot), gelernt (grün), abgelaufen (blau), diff (background)
        // Falls ein Wert -1 ist setzen wir 0f (MPAndroidChart zeichnet dann nichts für diesen Stackteil).
        val ungelerntF = if (ungelernt <= 0) 0f else ungelernt.toFloat()
        val gelerntF = if (gelernt <= 0) 0f else gelernt.toFloat()
        val abgelaufenF = if (abgelaufen <= 0) 0f else abgelaufen.toFloat()
        val diff = if (sum > -1 && sum != numAllCards) (numAllCards - sum) else 0
        val diffF = if (diff <= 0) 0f else diff.toFloat()

        // Fallback - wenn sum == 0 (wie früher) damit Chart nicht leer bleibt
        val hasAny = (ungelerntF + gelerntF + abgelaufenF + diffF) > 0f
        val entryValues = if (hasAny) {
            floatArrayOf(ungelerntF, gelerntF, abgelaufenF, diffF)
        } else {
            floatArrayOf(1f) // minimaler Balken (Hintergrundfarbe)
        }

        val entries = ArrayList<BarEntry>()
        // x-index 0 (ein Balken). Wenn du mehrere Balken brauchst, passe x entsprechend an.
        entries.add(BarEntry(0f, entryValues))

        // Erzeuge DataSet
        val set = BarDataSet(entries, "")
        // Farben – genau in derselben Reihenfolge wie die Werte oben
        val colors = ArrayList<Int>()
        colors.add(ContextCompat.getColor(context, R.color.unlearned))      // ungelernt
        colors.add(ContextCompat.getColor(context, R.color.learned))        // gelernt
        colors.add(ContextCompat.getColor(context, R.color.colorPrimary))   // abgelaufen
        colors.add(ContextCompat.getColor(context, R.color.defaultBackground)) // diff / filler
        set.colors = colors

        // Wir zeichnen keine Werte/Legende innerhalb des Charts (du nutzt externe TextViews)
        set.setDrawValues(false)
        // Optional: Ecken abrunden / andere Darstellungen sind mit MPAndroidChart möglich aber aufwändiger

        val data = BarData(set)
        // Bar-Breite (verkleinern/vergrößern nach Geschmack)
        data.barWidth = 0.6f

        chart.data = data

        // Chart optisch vereinfachen – Achsen/Legende/Description ausblenden wie beim alten StackBarChart
        chart.description.isEnabled = false
        chart.legend.isEnabled = false

        val left = chart.axisLeft
        left.isEnabled = false
        val right = chart.axisRight
        right.isEnabled = false

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawLabels(false)
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)

        chart.setFitBars(true)
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.setDrawValueAboveBar(false)

        // Refresh
        chart.invalidate()

        Log.d("ChartBar::show", "Chart updated")
    }
}
