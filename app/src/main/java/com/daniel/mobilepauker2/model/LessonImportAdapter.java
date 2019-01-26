package com.daniel.mobilepauker2.model;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Daniel on 05.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */

public class LessonImportAdapter extends ArrayAdapter<String> {
    private final ArrayList<String> data;

    public LessonImportAdapter(@NonNull Context context, ArrayList<String> data) {
        super(context, android.R.layout.simple_list_item_1, data);
        this.data = data;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        if (view.isSelected()) {
            view.setBackgroundColor(Color.LTGRAY);
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
        }

        TextView tv = view.findViewById(android.R.id.text1);
        String name = data.get(position);

        int index = name.endsWith(".xml.gz") ? name.indexOf(".xml") : name.indexOf(".pau");
        if (index != -1) {
            name = name.substring(0, index);
            tv.setText(name.substring(0, index));
        }

        return view;
    }
}
