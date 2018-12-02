package com.daniel.mobilepauker2.activities;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.model.SaveLessonThreaded;
import com.daniel.mobilepauker2.utils.Constants;

/**
 * Created by dfritsch on 20.03.2018.
 * veesy.de
 * hs-augsburg
 */

public class SaveDialog extends Activity {
    final Context context = this;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.progress_dialog);

        RelativeLayout progressBar = findViewById(R.id.pFrame);
        progressBar.setVisibility(View.VISIBLE);
        TextView title = findViewById(R.id.pTitle);
        title.setText(R.string.saving_title);

        SaveLessonThreaded saveThread = new SaveLessonThreaded(new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                boolean result = msg.getData().getBoolean(Constants.MESSAGE_BOOL_KEY);
                if (result) {
                    setResult(RESULT_OK);
                } else {
                    Toast.makeText(context, R.string.saving_error, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                }
                finish();
                return result;
            }
        }));
        saveThread.run();
    }

    // Touchevents und Backbutton blockieren, dass er nicht minimiert werden kann

    @Override
    public void onBackPressed() {}

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return false;
    }
}
