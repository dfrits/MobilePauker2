package com.daniel.mobilepauker2.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.model.ModelManager;
import com.daniel.mobilepauker2.model.SettingsManager;
import com.daniel.mobilepauker2.model.notification.NotificationService;
import com.daniel.mobilepauker2.statistics.ChartAdapter;
import com.daniel.mobilepauker2.statistics.ChartAdapter.ChartAdapterCallback;
import com.daniel.mobilepauker2.utils.Constants;
import com.daniel.mobilepauker2.utils.ErrorReporter;
import com.daniel.mobilepauker2.utils.Log;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState;

import static com.daniel.mobilepauker2.PaukerManager.showToast;
import static com.daniel.mobilepauker2.model.ModelManager.LearningPhase.FILLING_USTM;
import static com.daniel.mobilepauker2.model.ModelManager.LearningPhase.SIMPLE_LEARNING;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.HIDE_TIMES;
import static com.daniel.mobilepauker2.utils.Constants.NOTIFICATION_CHANNEL_ID;
import static com.daniel.mobilepauker2.utils.Constants.REQUEST_CODE_SAVE_DIALOG_NORMAL;
import static com.daniel.mobilepauker2.utils.Constants.TIMER_BAR_CHANNEL_ID;

/**
 * Created by Daniel on 24.02.2018.
 * Masterarbeit++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */

public class MainMenu extends AppCompatActivity {
    private static final int RQ_WRITE_EXT_SAVE = 98;
    private static final int RQ_WRITE_EXT_OPEN = 99;
    public static boolean isPaukerActive = false;
    private final ModelManager modelManager = ModelManager.instance();
    private final PaukerManager paukerManager = PaukerManager.instance();
    private final SettingsManager settingsManager = SettingsManager.instance();
    private final Context context = this;
    private boolean firstStart = true;
    private MenuItem search;
    private RecyclerView chartView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Channel erstellen, falls noch nicht vorhanden
        Log.d("AlamNotificationReceiver::onReceive", "Create Channels");
        createNotificationChannels();

        String action = getIntent().getAction();
        if (action != null && action.equals("Open Lesson")) {
            startActivity(new Intent(context, LessonImportActivity.class));
        }

        ErrorReporter.instance().init(context);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        setContentView(R.layout.main_menu);

        checkErrors();

        if (!modelManager.isLessonSetup()) {
            modelManager.createNewLesson();
        }

        initButtons();
        initView();
        initChartList();
    }

    /**
     * Prüft ob Errors vorliegen und fragt gegebenenfalls ob diese gesendet werden sollen.
     */
    private void checkErrors() {
        final ErrorReporter errorReporter = ErrorReporter.instance();
        if (errorReporter.isThereAnyErrorsToReport()) {
            AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);

            alt_bld.setTitle(getString(R.string.crash_report_title))
                    .setMessage(getString(R.string.crash_report_message))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            errorReporter.CheckErrorAndSendMail();
                        }
                    })
                    .setNeutralButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            errorReporter.deleteErrorFiles();
                            dialog.cancel();
                        }
                    });

            AlertDialog alert = alt_bld.create();
            alert.setIcon(R.mipmap.ic_launcher);
            alert.show();
        }
    }

    public void initButtons() {
        boolean hasCardsToLearn = modelManager.getUnlearnedBatchSize() != 0;
        boolean hasExpiredCards = modelManager.getExpiredCardsSize() != 0;

        findViewById(R.id.bLearnNewCard).setEnabled(hasCardsToLearn);
        findViewById(R.id.bLearnNewCard).setClickable(hasCardsToLearn);
        findViewById(R.id.tLearnNewCardDesc).setEnabled(hasCardsToLearn);

        findViewById(R.id.bRepeatExpiredCards).setEnabled(hasExpiredCards);
        findViewById(R.id.bRepeatExpiredCards).setClickable(hasExpiredCards);
        findViewById(R.id.tRepeatExpiredCardsDesc).setEnabled(hasExpiredCards);
    }

    private void initView() {
        invalidateOptionsMenu();

        String description = modelManager.getDescription();
        TextView descriptionView = findViewById(R.id.infoText);
        descriptionView.setText(description);
        if (!description.isEmpty()) {
            descriptionView.setMovementMethod(new ScrollingMovementMethod());
        }

        SlidingUpPanelLayout drawer = findViewById(R.id.drawerPanel);
        if (drawer != null) {
            drawer.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
                @Override
                public void onPanelSlide(View panel, float slideOffset) {

                }

                @Override
                public void onPanelStateChanged(View panel, PanelState previousState, PanelState newState) {
                    if (newState == PanelState.EXPANDED)
                        findViewById(R.id.drawerImage).setRotation(180);
                    if (newState == PanelState.COLLAPSED)
                        findViewById(R.id.drawerImage).setRotation(0);
                }
            });
            drawer.setPanelState(PanelState.COLLAPSED);
        }

        String title = getString(R.string.app_name);
        if (modelManager.isLessonNotNew()) {
            title = paukerManager.getReadableFileName();
        }
        setTitle(title);
    }

    private void initChartList() {
        // Im Thread laufen lassen um MainThread zu entlasten
        Thread initthread = new Thread(new Runnable() {
            @Override
            public void run() {
                chartView = findViewById(R.id.chartListView);
                final LinearLayoutManager layoutManager = new LinearLayoutManager(context,
                        LinearLayoutManager.HORIZONTAL, false);
                chartView.setLayoutManager(layoutManager);
                chartView.setOverScrollMode(View.OVER_SCROLL_NEVER);
                chartView.setScrollContainer(true);
                chartView.setNestedScrollingEnabled(true);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ChartAdapterCallback onClickListener = new ChartAdapterCallback() {
                            @Override
                            public void onClick(int position) {
                                showBatchDetails(position);
                            }
                        };
                        final ChartAdapter adapter = new ChartAdapter(context, onClickListener);
                        chartView.setAdapter(adapter);
                    }
                });
            }
        });
        initthread.run();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem save = menu.findItem(R.id.mSaveFile);
        search = menu.findItem(R.id.mSearch);
        MenuItem open = menu.findItem(R.id.mOpenLesson);

        menu.setGroupEnabled(R.id.mGroup, modelManager.isLessonNotNew() || !modelManager.isLessonEmpty());

        if (modelManager.getLessonSize() > 0) {
            search.setVisible(true);
            open.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        } else {
            search.setVisible(false);
            open.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        save.setVisible(paukerManager.isSaveRequired());

        if (search.isVisible()) {
            final SearchView searchView = (SearchView) search.getActionView();
            searchView.setIconifiedByDefault(false);
            searchView.setIconified(false);
            searchView.setQueryHint(getString(R.string.search_hint));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    Intent browseIntent = new Intent(Intent.ACTION_SEARCH);
                    browseIntent.setClass(context, SearchActivity.class);
                    browseIntent.putExtra(SearchManager.QUERY, query);
                    startActivity(browseIntent);
                    searchView.setQuery("", false);
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
    protected void onPause() {
        chartView = null;
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.d("MainMenuActivity::onResume", "ENTRY");
        super.onResume();

        modelManager.resetLesson();

        if (search != null) {
            search.collapseActionView();
        }

        if (!firstStart) {
            initButtons();
            initView();
            initChartList();
            invalidateOptionsMenu();
        }
        firstStart = false;

        isPaukerActive = true;

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancelAll();
    }

    @Override
    public void onBackPressed() {
        if (paukerManager.isSaveRequired()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(R.string.close_without_saving_dialog_msg)
                    .setPositiveButton(R.string.cancel, null)
                    .setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(getColor(R.color.unlearned));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.learned));
        } else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        NotificationService.enqueueWork(context);
        isPaukerActive = false;
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == RQ_WRITE_EXT_OPEN
                && (grantResults.length > 0) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openLesson();
        }
        if (requestCode == RQ_WRITE_EXT_SAVE
                && (grantResults.length > 0) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            saveLesson(REQUEST_CODE_SAVE_DIALOG_NORMAL);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SAVE_DIALOG_NORMAL) {
            if (resultCode == RESULT_OK) {
                showToast((Activity) context, R.string.saving_success, Toast.LENGTH_SHORT);
                paukerManager.setSaveRequired(false);
                modelManager.showExpireToast(context);
            }
            invalidateOptionsMenu();
        } else if (requestCode == Constants.REQUEST_CODE_SAVE_DIALOG_NEW_LESSON && resultCode == RESULT_OK) {
            createNewLesson();
        } else if (requestCode == Constants.REQUEST_CODE_SAVE_DIALOG_OPEN && resultCode == RESULT_OK) {
            startActivity(new Intent(context, LessonImportActivity.class));
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
        modelManager.setLearningPhase(context, ModelManager.LearningPhase.REPEATING_LTM);
        Intent importActivity = new Intent(context, LearnCardsActivity.class);
        startActivity(importActivity);
    }

    /**
     * Speichert die Lektion.
     * @param requestCode Wird für onActivityResult benötigt
     */
    private void saveLesson(final int requestCode) {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            checkPermission(RQ_WRITE_EXT_SAVE);
        } else {
            startActivityForResult(new Intent(context, SaveDialog.class), requestCode);
        }
    }

    /**
     * Fragt, wenn notwendig, die Permission ab und zeigt davor einen passenden Infodialog an.
     */
    private void openLesson() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            checkPermission(RQ_WRITE_EXT_OPEN);
        } else {
           if (paukerManager.isSaveRequired()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.lesson_not_saved_dialog_title)
                        .setMessage(R.string.save_lesson_before_question)
                        .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                saveLesson(Constants.REQUEST_CODE_SAVE_DIALOG_OPEN);
                            }
                        })
                        .setNeutralButton(R.string.open_lesson, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(context, LessonImportActivity.class));
                                dialog.dismiss();
                            }
                        });
                builder.create().show();
            } else
                startActivity(new Intent(context, LessonImportActivity.class));
        }
    }

    /**
     * Erstellt eine neue Lektion
     */
    private void createNewLesson() {
        paukerManager.setupNewApplicationLesson();
        paukerManager.setSaveRequired(false);
        initButtons();
        initChartList();
        initView();
        showToast((Activity) context, R.string.new_lession_created, Toast.LENGTH_SHORT);
    }

    /**
     * Fragt den Nutzer nach der Erlaubnis.
     * @param requestCode Welche Funktion danach ausgeführt werden soll.
     */
    private void checkPermission(final int requestCode) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.app_name)
                .setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pref.edit().putBoolean("FirstTime", false).apply();
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                requestCode);
                        dialog.dismiss();
                    }
                })
                .setNeutralButton(R.string.not_now, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            builder.setMessage(R.string.write_permission_rational_message);
        } else {
            if (pref.getBoolean("FirstTime", true)) {
                builder.setMessage(R.string.write_permission_info_message);
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
    }

    public void mSaveFileClicked(@Nullable MenuItem ignored) {
        saveLesson(REQUEST_CODE_SAVE_DIALOG_NORMAL);
    }

    /**
     * Aktion des Menubuttons
     * @param ignored Nicht benötigt
     */
    public void mOpenLessonClicked(@Nullable MenuItem ignored) {
        openLesson();
    }

    public void mResetLessonClicked(MenuItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.reset_lesson_dialog_title)
                .setMessage(R.string.reset_lesson_dialog_info)
                .setPositiveButton(R.string.reset, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        modelManager.forgetAllCards();
                        paukerManager.setSaveRequired(true);
                        initButtons();
                        initChartList();
                        initView();
                        showToast((Activity) context, R.string.lektion_zurückgesetzt, Toast.LENGTH_SHORT);
                        dialog.cancel();
                    }
                })
                .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.create().show();
    }

    public void mNewLessonClicked(MenuItem item) {
        if (paukerManager.isSaveRequired()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.lesson_not_saved_dialog_title)
                    .setMessage(R.string.save_lesson_before_question)
                    .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            saveLesson(Constants.REQUEST_CODE_SAVE_DIALOG_NEW_LESSON);
                        }
                    })
                    .setNeutralButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            createNewLesson();
                        }
                    });
            builder.create().show();
        } else createNewLesson();
    }

    public void mEditInfoTextClicked(@Nullable MenuItem ignored) {
        startActivity(new Intent(context, EditDescrptionActivity.class));
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.stay);
    }

    public void mSettingsClicked(MenuItem item) {
        startActivity(new Intent(context, SettingsActivity.class));
    }

    public void mOpenSearchClicked(MenuItem item) {
        SearchView searchView = (SearchView) item.getActionView();
        searchView.setIconified(false);
    }

    public void mFlipSidesClicked(MenuItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.reverse_sides_dialog_title)
                .setMessage(R.string.reverse_sides_dialog_info)
                .setPositiveButton(R.string.flip_cards, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        modelManager.flipAllCards();
                        paukerManager.setSaveRequired(true);
                        initButtons();
                        initChartList();
                        initView();
                        showToast((Activity) context, R.string.flip_sides_complete, Toast.LENGTH_SHORT);
                        dialog.cancel();
                    }
                })
                .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.create().show();
    }

    /**
     * Erstellt alle Channels
     */
    public void createNotificationChannels() {
        // Für Notification
        createNotificationChannel(getString(R.string.channel_notify_name_other),
                null,
                NotificationManager.IMPORTANCE_DEFAULT,
                NOTIFICATION_CHANNEL_ID,
                true);

        // Timerbar
        createNotificationChannel(getString(R.string.channel_timerbar_name),
                getString(R.string.channel_timerbar_description),
                NotificationManager.IMPORTANCE_LOW,
                TIMER_BAR_CHANNEL_ID,
                false);

        // Falls der TimerChannel noch existiert
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.deleteNotificationChannel("Timers");
            Log.d("MainMenu::createNotificationChannel", "Timer channel deleted");
        }
    }

    /**
     * Hilfsmethode zum Erstellen der Channels
     */
    private void createNotificationChannel(String channelName, String description, int importance, String ID, boolean playSound) {
        NotificationChannel channel = new NotificationChannel(ID, channelName, importance);

        if (description != null) {
            channel.setDescription(description);
        }

        if (!playSound) {
            channel.setSound(null, null);
        }

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
        Log.d("AlamNotificationReceiver::createNotificationChannel", "Channel created: " + channelName);
    }
}