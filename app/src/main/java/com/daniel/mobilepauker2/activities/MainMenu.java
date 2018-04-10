package com.daniel.mobilepauker2.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.dropbox.SyncDialog;
import com.daniel.mobilepauker2.model.PaukerModelManager;
import com.daniel.mobilepauker2.model.SettingsManager;
import com.daniel.mobilepauker2.statistics.ChartAdapter;
import com.daniel.mobilepauker2.statistics.ChartAdapter.ChartAdapterCallback;
import com.daniel.mobilepauker2.utils.Constants;
import com.daniel.mobilepauker2.utils.ErrorReporter;
import com.daniel.mobilepauker2.utils.Log;

import java.io.File;

import static com.daniel.mobilepauker2.model.PaukerModelManager.LearningPhase.FILLING_USTM;
import static com.daniel.mobilepauker2.model.PaukerModelManager.LearningPhase.SIMPLE_LEARNING;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.AUTO_SYNC;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.HIDE_TIMES;

/**
 * Created by Daniel on 24.02.2018.
 * Masterarbeit++ - innovativ, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */

public class MainMenu extends AppCompatActivity {
    private static final int RQ_WRITE_EXT = 99;
    private final PaukerModelManager modelManager = PaukerModelManager.instance();
    private final PaukerManager paukerManager = PaukerManager.instance();
    private final SettingsManager settingsManager = SettingsManager.instance();
    private final Context context = this;
    private boolean firstStart = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ErrorReporter.instance().init(context);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        setContentView(R.layout.main_menu);

        if (!modelManager.isLessonSetup()) {
            modelManager.createNewLesson(Constants.DEFAULT_FILE_NAME);
        }

        initButtons();
        initView();
        initChartList();

        if (settingsManager.getBoolPreference(context, AUTO_SYNC)) {
            String accessToken = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(Constants.DROPBOX_ACCESS_TOKEN, null);
            if (accessToken != null) {
                File[] files = paukerManager.listFiles(context);
                Intent syncIntent = new Intent(context, SyncDialog.class);
                syncIntent.putExtra(SyncDialog.ACCESS_TOKEN, accessToken);
                syncIntent.putExtra(SyncDialog.FILES, files);
                startActivityForResult(syncIntent, Constants.REQUEST_CODE_SYNC_DIALOG);
            }
        }
    }

    public void initButtons() {
        boolean hasCardsToLearn = modelManager.getUnlearnedBatchSize() != 0;
        boolean hasExpiredCards = modelManager.getExpiredCardsSize() != 0;

        RelativeLayout button;
        Drawable disabledImg = getDrawable(R.drawable.button_disabled_foreground);
        button = findViewById(R.id.bLearnNewCardFrame);
        button.setClickable(hasCardsToLearn);
        button.setForeground(button.isClickable() ? null : disabledImg);
        button = findViewById(R.id.bRepeatExpiredCardsFrame);
        button.setClickable(hasExpiredCards);
        button.setForeground(button.isClickable() ? null : disabledImg);
    }

    private void initView() {
        invalidateOptionsMenu();

        String description = modelManager.getDescription();
        TextView descriptionView = findViewById(R.id.infoText);
        descriptionView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                editInfoText(v);
                return true;
            }
        });
        descriptionView.setText(description);
        if (!description.isEmpty()) {
            descriptionView.setMovementMethod(new ScrollingMovementMethod());
        }

        if (!modelManager.isLessonNew()) {
            setTitle(paukerManager.getReadableFileName());
        }
    }

    private void initChartList() {
        final RecyclerView chartView = findViewById(R.id.chartListView);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context,
                LinearLayoutManager.HORIZONTAL, false);
        ChartAdapterCallback onClickListener = new ChartAdapterCallback() {
            @Override
            public void onClick(int position) {
                showBatchDetails(position);
            }
        };
        final ChartAdapter adapter = new ChartAdapter(context, onClickListener);
        chartView.setLayoutManager(layoutManager);
        chartView.setAdapter(adapter);
        chartView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        chartView.setScrollContainer(true);
        chartView.setNestedScrollingEnabled(true);
    }

    private void editInfoText() {
        startActivity(new Intent(context, EditDescrptionActivity.class));
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.stay);
    }

    /**
     * Startet die Permissionanfrage
     */
    private void requestPermission() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                RQ_WRITE_EXT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem save = menu.findItem(R.id.mSaveFile);
        MenuItem search = menu.findItem(R.id.mSearch);
        MenuItem open = menu.findItem(R.id.mOpenLesson);
        FloatingActionButton floatingOpen = findViewById(R.id.fbOpenLesson);
        if (!modelManager.isLessonNew()) {
            menu.setGroupEnabled(R.id.mGroup, true);
        } else {
            menu.setGroupEnabled(R.id.mGroup, false);
        }

        if (modelManager.getLessonSize() > 0) {
            search.setVisible(true);
            open.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            floatingOpen.setVisibility(View.GONE);
        } else {
            search.setVisible(false);
            open.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            floatingOpen.setVisibility(View.VISIBLE);
        }

        if (paukerManager.isSaveRequired()) {
            save.setEnabled(true);
            save.setIcon(R.drawable.menu_save_enabled);
            //            save.setIconTintList(ColorStateList.valueOf(Color.DKGRAY));
        } else {
            save.setEnabled(false);
            save.setIcon(R.drawable.menu_save_disabled);
            //             save.setIconTintList(ColorStateList.valueOf(Color.WHITE));
        }

        if (search.isVisible()) {
            final SearchView searchView = (SearchView) search.getActionView();
            searchView.setIconifiedByDefault(false);
            searchView.setQueryHint(getString(R.string.search_hint));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    Intent browseIntent = new Intent(Intent.ACTION_SEARCH);
                    browseIntent.setClass(context, SearchActivity.class);
                    browseIntent.putExtra(SearchManager.QUERY, query);
                    startActivity(browseIntent);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });
            searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) searchView.clearFocus();
                }
            });
        }

        return true;
    }

    @Override
    public void onResume() {
        Log.d("MainMenuActivity::onResume", "ENTRY");
        super.onResume();

        modelManager.resetLesson();

        if (!firstStart) {
            initButtons();
            initView();
            initChartList();
        }
        firstStart = false;

        if (paukerManager.isSaveRequired()) {
            setTitle(getTitle() + "*");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == RQ_WRITE_EXT) {
            if ((grantResults.length > 0) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openLesson();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CODE_SAVE_DIALOG) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(context, R.string.saving_success, Toast.LENGTH_SHORT).show();
                paukerManager.setSaveRequired(false);
                invalidateOptionsMenu();
            } else {
                Toast.makeText(context, R.string.saving_error, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == Constants.REQUEST_CODE_SYNC_DIALOG) {
            if (resultCode == RESULT_OK) {
                Log.d("OpenLesson", "Synchro erfolgreich");
            } else {
                Log.d("OpenLesson", "Synchro nicht erfolgreich");
                Toast.makeText(context, R.string.error_synchronizing, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void addNewCard(View view) {
        startActivity(new Intent(context, AddCardActivity.class));
    }

    public void learnNewCard(View view) {
        if (settingsManager.getBoolPreference(context, HIDE_TIMES)) {
            modelManager.setLearningPhase(context, SIMPLE_LEARNING);
        } else {
            modelManager.setLearningPhase(context, FILLING_USTM);
        }

        startActivity(new Intent(context, LearnCardsActivity.class));
    }

    public void repeatCards(View view) {
        modelManager.setLearningPhase(context, PaukerModelManager.LearningPhase.REPEATING_LTM);
        Intent importActivity = new Intent(context, LearnCardsActivity.class);
        startActivity(importActivity);
    }

    public void saveFile(@Nullable MenuItem ignored) {
        if (paukerManager.getReadableFileName().equals(Constants.DEFAULT_FILE_NAME)) {
            final LayoutInflater inflater = getLayoutInflater();

            @SuppressLint("InflateParams")
            View view = inflater.inflate(R.layout.give_lesson_name_dialog, null);
            final EditText textField = view.findViewById(R.id.eTGiveLessonName);

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.give_lesson_name_dialog_title)
                    .setView(view)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            String newLessonName = textField.getText().toString();

                            if (!newLessonName.endsWith(".pau.gz"))
                                newLessonName = newLessonName + ".pau.gz";

                            if (paukerManager.setCurrentFileName(newLessonName))
                                startActivityForResult(new Intent(context, SaveDialog.class), Constants.REQUEST_CODE_SAVE_DIALOG);
                            else
                                Toast.makeText(context, R.string.error_filename_invalid, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.not_now, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            final AlertDialog dialog = builder.create();

            textField.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(s.length() > 0);
                }
            });

            dialog.show();
            textField.setText("");
        } else {
            startActivityForResult(new Intent(context, SaveDialog.class), Constants.REQUEST_CODE_SAVE_DIALOG);
        }
    }

    /**
     * Aktion des Floatingbuttons.
     * @param ignored Nicht benötigt
     */
    public void fbClicked(View ignored) {
        openLesson();
    }

    /**
     * Aktion des Menubuttons
     * @param ignored Nicht benötigt
     */
    public void openLesson(@Nullable MenuItem ignored) {
        openLesson();
    }

    /**
     * Fragt, wenn notwendig, die Permission ab und zeigt davor einen passenden Infodialog an.
     */
    private void openLesson() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.app_name)
                .setNegativeButton(R.string.not_now, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermission();
                        dialog.dismiss();
                    }
                });
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                builder.setMessage(R.string.write_permission_rational_message);
            } else {
                if (pref.getBoolean("FirstTime", true)) {
                    builder.setMessage(R.string.write_permission_info_message);
                    pref.edit().putBoolean("FirstTime", false).apply();
                } else {
                    builder.setMessage(R.string.write_permission_rational_message)
                            .setPositiveButton(R.string.settings, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                                    intent.setData(uri);
                                    startActivity(intent);
                                    dialog.dismiss();
                                }
                            });
                }
            }
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            startActivity(new Intent(context, LessonImportActivity.class));
        }
    }

    public void resetLesson(MenuItem item) {
        Toast.makeText(context, "Lesson reseted", Toast.LENGTH_SHORT).show();
    }

    public void newLesson(MenuItem item) {
        Toast.makeText(context, "New Lesson created", Toast.LENGTH_SHORT).show();
    }

    public void editInfoText(@Nullable MenuItem ignored) {
        editInfoText();
    }

    public void settings(MenuItem item) {
        startActivity(new Intent(context, SettingsActivity.class));
    }

    public void editInfoText(@Nullable View ignored) {
        editInfoText();
    }

    private void showBatchDetails(int index) {
        if (modelManager.getLessonSize() == 0) return;

        Intent browseIntent = new Intent(Intent.ACTION_SEARCH);
        browseIntent.setClass(context, SearchActivity.class);
        browseIntent.putExtra(SearchManager.QUERY, "");


        if ((index > 1 && modelManager.getBatchStatistics().get(index - 2).getBatchSize() == 0)
                || (index == 1 && modelManager.getUnlearnedBatchSize() == 0)) {
            return;
        }

        browseIntent.putExtra(Constants.STACK_INDEX, index);
        startActivity(browseIntent);
    }

    public void openSearch(MenuItem item) {
        SearchView searchView = (SearchView) item.getActionView();
        searchView.setIconified(false);
    }
}