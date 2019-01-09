package com.daniel.mobilepauker2.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.model.SaveLessonThreaded;
import com.daniel.mobilepauker2.utils.Constants;

/**
 * Created by dfritsch on 20.03.2018.
 * veesy.de
 * hs-augsburg
 */

public class SaveDialog extends Activity {
    private final Context context = this;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.progress_dialog);

        RelativeLayout progressBar = findViewById(R.id.pFrame);
        progressBar.setVisibility(View.VISIBLE);
        TextView title = findViewById(R.id.pTitle);
        title.setText(R.string.saving_title);

        if (PaukerManager.instance().getReadableFileName().equals(Constants.DEFAULT_FILE_NAME)) {
            openDialog();
        } else {
            saveLesson();
        }
    }

    // Touchevents und Backbutton blockieren, dass er nicht minimiert werden kann

    @Override
    public void onBackPressed() {
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return false;
    }

    private void openDialog() {
        @SuppressLint("InflateParams") final View view = getLayoutInflater().inflate(R.layout.give_lesson_name_dialog, null);
        final EditText textField = view.findViewById(R.id.eTGiveLessonName);
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.NamingDialogTheme);
        final PaukerManager paukerManager = PaukerManager.instance();
        builder.setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (paukerManager.setCurrentFileName(textField.getText().toString())) {
                            saveLesson();
                        } else {
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                    }
                })
                .setNegativeButton(R.string.not_now, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null && getCurrentFocus() != null && imm.isAcceptingText()) {
                            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                        }
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                });

        final AlertDialog dialog = builder.create();

        textField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String newName = s.toString();
                boolean isEmptyString = newName.length() > 0;
                if (!newName.endsWith(".pau.gz"))
                    newName = newName + ".pau.gz";
                boolean isValidName = paukerManager.isNameValid(newName);
                boolean isExisting = paukerManager.isFileExisting(context, newName);
                view.findViewById(R.id.tFileExistingHint).setVisibility(isExisting ? View.VISIBLE : View.GONE);
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(isEmptyString
                        && isValidName
                        && !isExisting);
            }
        });

        dialog.show();
        textField.setText("");
    }

    private void saveLesson() {
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
}
