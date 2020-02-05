package com.daniel.mobilepauker2.statistics

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.model.ModelManager
import com.daniel.mobilepauker2.statistics.ChartBar.ChartBarCallback
import java.util.*

/**
 * Created by Daniel on 27.02.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */
class ChartAdapter(private val context: Context, callback: ChartAdapterCallback?) :
    RecyclerView.Adapter<ChartAdapter.ViewHolder>() {
    private val modelManager: ModelManager? = ModelManager.Companion.instance()
    private val batchStatistics: List<BatchStatistics?>?
    private val callback: ChartAdapterCallback?
    private val lessonSize: Int
    private val chartBars: List<ChartBar>
    override fun onCreateViewHolder(
        parent: ViewGroup,
        position: Int
    ): ViewHolder {
        val v =
            LayoutInflater.from(context).inflate(R.layout.chart_bar, parent, false)
        val titel: String
        val abgl: Int
        val ungel: Int
        val gel: Int
        val cbc = ChartBarCallback { callback?.onClick(position) }
        val chartBar = ChartBar(v, cbc)
        when (position) {
            0 -> {
                titel = context.resources.getString(R.string.sum)
                abgl = modelManager.getExpiredCardsSize()
                ungel = modelManager.getUnlearnedBatchSize()
                gel = lessonSize - abgl - ungel
                chartBar.show(context, titel, lessonSize, gel, ungel, abgl, lessonSize)
            }
            1 -> {
                titel = context.resources.getString(R.string.untrained)
                ungel = modelManager.getUnlearnedBatchSize()
                chartBar.show(context, titel, ungel, -1, ungel, -1, lessonSize)
            }
            else -> {
                titel = context.getString(R.string.stack) + (position - 1)
                val sum = batchStatistics!![position - 2].getBatchSize()
                abgl = batchStatistics[position - 2].getExpiredCardsSize()
                gel = sum - abgl
                chartBar.show(context, titel, sum, gel, -1, abgl, lessonSize)
            }
        }
        return ViewHolder(v)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
    }

    override fun getItemCount(): Int {
        return batchStatistics!!.size + 2
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    class ViewHolder(val view: View) :
        RecyclerView.ViewHolder(view)

    interface ChartAdapterCallback {
        fun onClick(position: Int)
    }

    init {
        batchStatistics = modelManager.getBatchStatistics()
        lessonSize = modelManager.getLessonSize()
        this.callback = callback
        chartBars = ArrayList(itemCount)
    }
}