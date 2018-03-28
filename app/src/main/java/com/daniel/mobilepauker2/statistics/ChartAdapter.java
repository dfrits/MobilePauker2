package com.daniel.mobilepauker2.statistics;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.model.PaukerModelManager;
import com.daniel.mobilepauker2.statistics.ChartBar.ChartBarCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Daniel on 27.02.2018.
 * Masterarbeit:
 * MobilePauker++ - innovativ, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */

public class ChartAdapter extends RecyclerView.Adapter<ChartAdapter.ViewHolder> {
    private final PaukerModelManager modelManager = PaukerModelManager.instance();

    private final List<BatchStatistics> batchStatistics;
    private final ChartAdapterCallback callback;
    private final Context context;
    private final int lessonSize;
    private final List<ChartBar> chartBars;

    public ChartAdapter(Context context, ChartAdapterCallback callback) {
        this.context = context;
        batchStatistics = modelManager.getBatchStatistics();
        lessonSize = modelManager.getLessonSize();
        this.callback = callback;
        chartBars = new ArrayList<>(getItemCount());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int position) {
        View v = LayoutInflater.from(context).inflate(R.layout.chart_bar, parent, false);
        String titel;
        int abgl;
        int ungel;
        int gel;
        final int cposition = position;
        ChartBarCallback cbc = new ChartBarCallback() {
            @Override
            public void onClick() {
                if (callback != null) callback.onClick(cposition);
            }
        };
        ChartBar chartBar = new ChartBar(v, cbc);
        switch (position) {
            case 0:
                titel = context.getResources().getString(R.string.sum);
                abgl = modelManager.getExpiredCardsSize();
                ungel = modelManager.getUnlearnedBatchSize();
                gel = lessonSize - abgl - ungel;
                chartBar.show(context, titel, lessonSize, gel, ungel, abgl, lessonSize);
                break;
            case 1:
                titel = context.getResources().getString(R.string.untrained);
                ungel = modelManager.getUnlearnedBatchSize();
                chartBar.show(context, titel, ungel, -1, ungel, -1, lessonSize);
                break;
            default:
                titel = context.getString(R.string.stack) + (position - 1);
                int sum = batchStatistics.get(position - 2).getBatchSize();
                abgl = batchStatistics.get(position - 2).getExpiredCardsSize();
                gel = sum - abgl;
                chartBar.show(context, titel, sum, gel, -1, abgl, lessonSize);
        }
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    }

    @Override
    public int getItemCount() {
        return batchStatistics.size() + 2;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View view;

        ViewHolder(View itemView) {
            super(itemView);
            view = itemView;
        }
    }

    public interface ChartAdapterCallback {
        void onClick(int position);
    }
}
