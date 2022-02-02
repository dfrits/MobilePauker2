package de.daniel.mobilepauker2.statistics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.lesson.batch.BatchType
import java.util.*
import javax.inject.Inject

class ChartAdapter(private val context: PaukerApplication, val callback: ChartAdapterCallback) :
    RecyclerView.Adapter<ChartAdapter.ViewHolder>() {
    private val batchStatistics: List<BatchStatistics>
    private val lessonSize: Int
    private val chartBars: List<ChartBar>

    @Inject
    lateinit var lessonManager: LessonManager

    @NonNull
    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.chart_bar, parent, false)
        val titel: String
        val abgelaufen: Int
        val ungelernt: Int
        val gelernt: Int
        val chartBar = ChartBar(view)
        when (position) {
            0 -> {
                titel = context.resources.getString(R.string.sum)
                abgelaufen = lessonManager.getBatchSize(BatchType.EXPIRED)
                ungelernt = lessonManager.getBatchSize(BatchType.UNLEARNED)
                gelernt = lessonSize - abgelaufen - ungelernt
                chartBar.show(
                    context,
                    titel,
                    lessonSize,
                    gelernt,
                    ungelernt,
                    abgelaufen,
                    lessonSize
                )
            }
            1 -> {
                titel = context.resources.getString(R.string.untrained)
                ungelernt = lessonManager.getBatchSize(BatchType.UNLEARNED)
                chartBar.show(context, titel, ungelernt, -1, ungelernt, -1, lessonSize)
            }
            else -> {
                titel = context.getString(R.string.stack) + (position - 1)
                val sum = batchStatistics[position - 2].batchSize
                abgelaufen = batchStatistics[position - 2].expiredCardsSize
                gelernt = sum - abgelaufen
                chartBar.show(context, titel, sum, gelernt, -1, abgelaufen, lessonSize)
            }
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.view.setOnClickListener {
            callback.onClick(position)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemCount(): Int = batchStatistics.size + 2

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    interface ChartAdapterCallback {
        fun onClick(position: Int)
    }

    init {
        context.applicationSingletonComponent.inject(this)
        batchStatistics = lessonManager.getBatchStatistics()
        lessonSize = lessonManager.getBatchSize(BatchType.LESSON)
        chartBars = ArrayList(itemCount)
    }
}