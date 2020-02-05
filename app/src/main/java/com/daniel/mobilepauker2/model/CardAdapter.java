package com.daniel.mobilepauker2.model;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.daniel.mobilepauker2.R;

import java.util.Calendar;
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
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            v = inflater.inflate(R.layout.search_result, parent, false);
        }
        FlashCard card = items.get(position);
        if (card != null) {
            MPTextView sideA = v.findViewById(R.id.tCardSideA);
            MPTextView sideB = v.findViewById(R.id.tCardSideB);
            TextView learnedAt = v.findViewById(R.id.tLearnedTime);
            TextView expireAt = v.findViewById(R.id.tExpireTime);
            TextView stackNumber = v.findViewById(R.id.tStackNumber);
            ImageView repeatType = v.findViewById(R.id.iRepeatType);
            if (sideA != null) {
                sideA.setCard(card.getFrontSide());
            }
            if (sideB != null) {
                sideB.setCard(card.getReverseSide());
            }

            // learnedAt und stackNumber
            long learnedTime = card.getLearnedTimestamp();
            if (learnedTime != 0) {
                Calendar cal = Calendar.getInstance(Locale.getDefault());
                cal.setTimeInMillis(learnedTime);
                String date = DateFormat.format("dd.MM.yyyy HH:mm", cal).toString();
                String text = context.getString(R.string.learned_at).concat(" ").concat(date);
                learnedAt.setText(text);

                int stack = card.getLongTermBatchNumber();
                stackNumber.setVisibility(View.VISIBLE);
                text = context.getString(R.string.stack).concat(" ").concat(String.valueOf(stack + 1));
                stackNumber.setText(text);
            }

            // expireAt
            long expirationTime = card.getExpirationTime();
            if (expirationTime != -1) {
                expireAt.setVisibility(View.VISIBLE);
                Calendar cal = Calendar.getInstance(Locale.getDefault());
                cal.setTimeInMillis(expirationTime);
                String date = DateFormat.format("dd.MM.yyyy HH:mm", cal).toString();

                String text;
                if (expirationTime < System.currentTimeMillis()) {
                    text = context.getString(R.string.expired_at);
                } else {
                    text = context.getString(R.string.expire_at);
                }
                text = text.concat(" ").concat(date);

                expireAt.setText(text);
            }

            // repeatType
            int drawable = card.isRepeatedByTyping() ? R.drawable.rt_typing : R.drawable.rt_thinking;
            repeatType.setImageResource(drawable);
        }
        return v;
    }
}
