package com.daniel.mobilepauker2.model;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import com.daniel.mobilepauker2.R;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Daniel on 07.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */

public class CardAdapter extends ArrayAdapter<FlashCard> {
    private final Context context;
    private final List<FlashCard> items;

    public CardAdapter(Context context, List<FlashCard> items) {
        super(context, R.layout.search_result, items);
        this.items = items;
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            v = inflater.inflate(R.layout.search_result, parent, false);
        }
        FlashCard card = items.get(position);
        if (card != null) {
            TextView sideA = v.findViewById(R.id.tCardSideA);
            TextView sideB = v.findViewById(R.id.tCardSideB);
            TextView learnedAt = v.findViewById(R.id.tLearnedTime);
            TextView expireAt = v.findViewById(R.id.tExpireTime);
            if (sideA != null) {
                sideA.setText(card.getSideAText());
            }
            if (sideB != null) {
                sideB.setText(card.getSideBText());
            }
            if (learnedAt != null) {
                long learnedTime = card.getLearnedTimestamp();
                String text;
                if (learnedTime == 0) {
                    text = context.getString(R.string.not_learned_yet);
                } else {
                    Calendar cal = Calendar.getInstance(Locale.getDefault());
                    cal.setTimeInMillis(learnedTime);
                    String date = DateFormat.format("dd MMMM yyyy HH:mm", cal).toString();
                    text = context.getString(R.string.learned_at).concat(" ").concat(date);
                }
                learnedAt.setText(text);
            }
            if (expireAt != null) {
                long expirationTime = card.getExpirationTime();
                if (expirationTime == -1) {
                    expireAt.setVisibility(View.GONE);
                } else {
                    expireAt.setVisibility(View.VISIBLE);
                    Calendar cal = Calendar.getInstance(Locale.getDefault());
                    cal.setTimeInMillis(expirationTime);
                    String date = DateFormat.format("dd MMMM yyyy HH:mm", cal).toString();

                    String text;
                    if (expirationTime < System.currentTimeMillis()) {
                        text = context.getString(R.string.expired_at);
                    } else {
                        text = context.getString(R.string.expire_at);
                    }
                    text = text.concat(" ").concat(date);

                    expireAt.setText(text);
                }
            }
        }
        return v;
    }
}
