package com.daniel.mobilepauker2.statistics;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import com.daniel.mobilepauker2.R;
import com.db.chart.model.BarSet;
import com.db.chart.renderer.AxisRenderer;
import com.db.chart.view.StackBarChartView;

/**
 * Created by Daniel on 28.02.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */

class ChartBar {
    private final StackBarChartView chart;
    private final TextView abglValue;
    private final TextView gelValue;
    private final TextView sumValue;
    private final ChartBarCallback callback;

    ChartBar(View view, ChartBarCallback callback) {
        chart = view.findViewById(R.id.chartBar);
        abglValue = view.findViewById(R.id.abgelaufenValue);
        gelValue = view.findViewById(R.id.gelerntValue);
        sumValue = view.findViewById(R.id.sumValue);
        this.callback = callback;
    }

    /**
     * Erstellt einen Balken mit den jeweiligen Farben und setzt die Textfelder darunter.
     * -1 bedeutet kein Balken und kein Label.
     * Ist aktuell keine Lektion geöffnet, wird kein Balken angezeigt und nur die Labels.
     * @param context     Context der aufrufenden Aktivity um Farben zu setzen
     * @param titel       Der Titel des Balken
     * @param sum         Gesamtzahl aller Karten in diesem Stapel. Wird nur als Label angezeigt
     * @param gel         Anzahl der gelernten Karten im Stapel. Wird als grüner Teil im Balken
     *                    angezeigt und als Label
     * @param ungel       Anzahl der ungelernten Karten im Stapel. Wird nur als roter Teil Teil im
     *                    Balken angezeigt
     * @param abgl        Anzahl der abgelaufenen Karten im Stapel. Wird als Blauer Teil im Balken
     * @param numAllCards Gesamtzahl Aller Karten in der Lektion. Ist die gleiche Zahl wie
     *                    <b>sum</b> vom ersten Balken. Wird benötigt, um die Gesamthöhe des Balken
     */
    void show(Context context, String titel, int sum, int gel, int ungel, int abgl, int numAllCards) {
        // String zu einem Array ändern
        String[] titelLabel = new String[]{titel};

        // Labels setzen
        String text = String.valueOf(abgl == -1 ? "" : abgl);
        abglValue.setText(text);
        text = String.valueOf(gel == -1 ? "" : gel);
        gelValue.setText(text);
        text = String.valueOf(sum == -1 ? "" : sum);
        sumValue.setText(text);

        // Bar initialisieren und zum DataSet hinzufügen
        BarSet stackBarSet;
        if (ungel != -1) {
            stackBarSet = new BarSet(titelLabel, new float[]{ungel});
            stackBarSet.setColor(context.getColor(R.color.unlearned));
            chart.addData(stackBarSet);
        }
        if (gel != -1) {
            stackBarSet = new BarSet(titelLabel, new float[]{gel});
            stackBarSet.setColor(context.getColor(R.color.learned));
            chart.addData(stackBarSet);
        }
        if (abgl != -1) {
            stackBarSet = new BarSet(titelLabel, new float[]{abgl});
            stackBarSet.setColor(context.getColor(R.color.colorPrimary));
            chart.addData(stackBarSet);
        }

        if (sum > -1 && sum != numAllCards) {
            int diff = numAllCards - sum;

            stackBarSet = new BarSet(titelLabel, new float[]{diff});
            stackBarSet.setColor(context.getColor(R.color.defaultBackground));
            chart.addData(stackBarSet);
        }

        // Sind alle Werte null, wird ein Mindestwert gesetzt, da sonst eine Exception geworfen wird
        if (sum == 0) {
            chart.reset();
            stackBarSet = new BarSet(titelLabel, new float[]{1});
            stackBarSet.setColor(context.getColor(R.color.defaultBackground));
            chart.addData(stackBarSet);
        }

        // Details setzen und anzeigen
        chart.setXLabels(AxisRenderer.LabelPosition.OUTSIDE)
                .setYLabels(AxisRenderer.LabelPosition.NONE)
                .setTypeface(Typeface.DEFAULT_BOLD)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (callback != null) callback.onClick();
                    }
                });
        chart.show();
    }

    public interface ChartBarCallback {
        void onClick();
    }
}
