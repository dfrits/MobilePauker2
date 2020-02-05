package com.daniel.mobilepauker2.activities;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.EditText;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.model.ModelManager;

/**
 * Created by Daniel on 06.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */

public class EditDescrptionActivity extends AppCompatActivity {
    private EditText editText;
    private final ModelManager modelManager = ModelManager.instance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_description);

        editText = findViewById(R.id.editField);
        editText.setText(modelManager.getDescription());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (editText != null) editText.setText(modelManager.getDescription());
    }

    @Override
    protected void onPause() {
        super.onPause();
        String text = null;
        if (editText != null) {
            text = editText.getText().toString().trim();
        }
        if (text != null && !modelManager.getDescription().equals(text)) {
            modelManager.setDescription(text);
            PaukerManager.instance().setSaveRequired(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom);
    }
}
