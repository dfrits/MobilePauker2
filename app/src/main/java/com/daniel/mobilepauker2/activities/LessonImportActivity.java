package com.daniel.mobilepauker2.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.format.DateFormat;
import android.util.SparseLongArray;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.dropbox.DropboxAccDialog;
import com.daniel.mobilepauker2.dropbox.SyncDialog;
import com.daniel.mobilepauker2.model.LessonImportAdapter;
import com.daniel.mobilepauker2.model.ModelManager;
import com.daniel.mobilepauker2.model.xmlsupport.FlashCardXMLPullFeedParser;
import com.daniel.mobilepauker2.utils.Constants;
import com.daniel.mobilepauker2.utils.ErrorReporter;
import com.daniel.mobilepauker2.utils.Log;
import com.dropbox.core.android.Auth;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Locale;


/**
 * Created by Daniel on 04.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */

public class LessonImportActivity extends AppCompatActivity {
    protected static final int CONTEXT_DELETE = 0;
    protected static final int CONTEXT_OPEN = 1;
    private static String errorMessage = null;
    private final ModelManager modelManager = ModelManager.instance();
    private final PaukerManager paukerManager = PaukerManager.instance();
    private final Context context = this;
    private String accessToken;
    private ArrayList<String> fileNames = new ArrayList<>();
    private ListView listView;
    private SharedPreferences preferences;
    private File[] files = new File[0];

    /**
     * Speichert die letzte Selektion in der Liste.
     */
    private int lastSelection = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        setContentView(R.layout.open_lesson);
        init();
    }

    private void init() {
        if (readFlashCardFiles()) {
            findViewById(R.id.tNothingFound).setVisibility(View.GONE);
            errorMessage = null;
        } else {
            findViewById(R.id.tNothingFound).setVisibility(View.VISIBLE);
        }
        initListView();
    }

    private void initListView() {
        listView = findViewById(R.id.lvLessons);
        listView.setAdapter(new LessonImportAdapter(context, fileNames));
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                itemClicked(position);
            }
        });
        registerForContextMenu(listView);
        listView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {

            public void onCreateContextMenu(ContextMenu menu, View v,
                                            ContextMenu.ContextMenuInfo menuInfo) {
                menu.add(0, CONTEXT_DELETE, 0, "Delete");
                menu.add(0, CONTEXT_OPEN, 0, "Open");
            }
        });
    }

    private void itemClicked(int position) {
        TextView infoText = findViewById(R.id.infoText);
        if (lastSelection != position) {
            listView.getChildAt(position).setSelected(true);
            lastSelection = position;
            String text = getString(R.string.next_expire_date);
            try {
                URI uri = getFilePath((String) listView.getItemAtPosition(position)).toURI();
                FlashCardXMLPullFeedParser parser = new FlashCardXMLPullFeedParser(uri.toURL());
                SparseLongArray map = parser.getNextExpireDate();

                if (map.get(0) > Long.MIN_VALUE) {
                    if (map.get(1, 0) > 0) {
                        long numberOfCards = map.get(1);
                        text = getString(R.string.expired_cards).concat(" ")
                                .concat(String.valueOf(numberOfCards));
                    } else {
                        long dateL = map.get(0);
                        Calendar cal = Calendar.getInstance(Locale.getDefault());
                        cal.setTimeInMillis(dateL);
                        String date = DateFormat.format("dd.MM.yyyy HH:mm", cal).toString();
                        text = text.concat(" ").concat(date);
                    }
                } else {
                    text = text.concat(" ").concat(getString(R.string.nothing_learned_yet));
                }
            } catch (IOException | RuntimeException ignored) {
                PaukerManager.showToast((Activity) context, R.string.error_reading_from_xml, Toast.LENGTH_SHORT);
                resetSelection(null);
                init();
                text = null;
            }
            if (text != null) {
                infoText.setText(text);
                infoText.setVisibility(View.VISIBLE);
            }
        } else {
            listView.getChildAt(position).setSelected(false);
            lastSelection = -1;
            infoText.setVisibility(View.GONE);
        }
        ((ArrayAdapter) listView.getAdapter()).notifyDataSetChanged();
        invalidateOptionsMenu();
    }

    /**
     * Setzt die Auswahl zurück. Ausgelöst, wenn außerhalb der Liste gelickt wird.
     * @param view Wird nicht benötigt
     */
    public void resetSelection(@Nullable View view) {
        if (lastSelection != -1) {
            listView.clearChoices();
            lastSelection = -1;
            findViewById(R.id.infoText).setVisibility(View.GONE);
            ((ArrayAdapter) listView.getAdapter()).notifyDataSetChanged();
            invalidateOptionsMenu();
        }
    }

    /**
     * Liest die Lektionen aus dem Ordner aus und zeigt sie in einer Liste an.
     * @return <b>True</b>, wenn Lektionen vorhanden sind und erfolgreich ausgelesen werden konnten.
     * Sonst <b>false</b>
     */
    private boolean readFlashCardFiles() {
        try {
            // Dateien auslesen
            files = paukerManager.listFiles(context);

            // Sortieren
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            // Liste füllen und Endungen abschneiden
            fileNames.clear();

            if (fileNames == null || files.length == 0) {
                return false;
            }

            for (File aFile : files) {
                fileNames.add(aFile.getName());
            }

            return true;

        } catch (Exception e) {
            Log.d("ImportFlashCardFile::onCreate", "Unable to read directory from flash card " + e.toString());
            return false;
        }
    }

    /**
     * Loads a lesson from a file
     * @param filename Name der Datei, die importiert werden soll
     * @return <b>True</b> if lesson loaded ok
     */
    private File getFilePath(String filename) throws IOException {
        // Validate the filename
        if (!paukerManager.validateFilename(filename)) {
            PaukerManager.showToast((Activity) context, R.string.error_filename_invalid, Toast.LENGTH_LONG);
            throw new IOException("Filename invalid");
        }

        String filePath = Environment.getExternalStorageDirectory() + paukerManager.getApplicationDataDirectory() + filename;
        return new File(filePath);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.open_lesson, menu);

        // Wurde auf ein Item geklickt Buttons ändern auf Lesson öffnen
        // Wurde Selektion aufgehoben, dann wieder zruckändern
        menu.findItem(R.id.mSyncFilesWithDropbox).setVisible(lastSelection == -1);
        menu.findItem(R.id.mOpenLesson).setVisible(lastSelection != -1);

        return true;
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final AdapterView.AdapterContextMenuInfo menuInfo =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final int position = menuInfo.position;
        switch (item.getItemId()) {
            case CONTEXT_DELETE:
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(R.string.delete_lesson_message)
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                Log.d("Import flash card file activity",
                                        "list pos:" + position + " id:" + menuInfo.id);
                                String filename = listView.getItemAtPosition(position).toString();
                                String filePath = Environment.getExternalStorageDirectory() +
                                        paukerManager.getApplicationDataDirectory() + filename;
                                File file = new File(filePath);

                                if (file.isFile()) {
                                    if (modelManager.deleteLesson(context, file)) {
                                        init();
                                        resetSelection(null);

                                        if (!fileNames.contains(paukerManager.getCurrentFileName())) {
                                            paukerManager.setupNewApplicationLesson();
                                            paukerManager.setSaveRequired(false);
                                        }
                                    } else {
                                        PaukerManager.showToast((Activity) context, R.string.delete_lesson_error, Toast.LENGTH_SHORT);
                                    }
                                }
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.create().show();
                break;
            case CONTEXT_OPEN:
                openLesson(position);
                break;
            default:
                return super.onContextItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        String uid = Auth.getUid();
        String storedUid = preferences.getString(Constants.DROPBOX_USER_ID, null);
        if (uid != null && !uid.equals(storedUid)) {
            preferences.edit().putString(Constants.DROPBOX_USER_ID, uid).apply();
        }
    }

    @Override
    public void finish() {
        if (errorMessage != null) {
            PaukerManager.showToast(this, errorMessage, Toast.LENGTH_LONG);
        }

        super.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CODE_SYNC_DIALOG) {
            if (resultCode == RESULT_OK) {
                Log.d("OpenLesson", "Synchro erfolgreich");
            } else {
                Log.d("OpenLesson", "Synchro nicht erfolgreich");
                PaukerManager.showToast((Activity) context, R.string.error_synchronizing, Toast.LENGTH_SHORT);
            }
            init();
            if (modelManager.isLessonNotNew())
                if (fileNames.contains(paukerManager.getCurrentFileName())) {
                    try {
                        paukerManager.loadLessonFromFile(getFilePath(paukerManager.getCurrentFileName()));
                        paukerManager.setSaveRequired(false);
                    } catch (IOException ignored) {
                        PaukerManager.showToast((Activity) context, R.string.reopen_lesson_error, Toast.LENGTH_LONG);
                        ErrorReporter.instance().AddCustomData("ImportThread", "IOException?");
                    }
                } else {
                    paukerManager.setupNewApplicationLesson();
                    paukerManager.setSaveRequired(false);
                }
        }
        if (requestCode == Constants.REQUEST_CODE_DB_ACC_DIALOG && resultCode == RESULT_OK) {
            accessToken = preferences.getString(Constants.DROPBOX_ACCESS_TOKEN, null);
            if (accessToken != null) {
                startSync();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (lastSelection > -1) {
            resetSelection(null);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Syncronisiert die Dateien. Wird vom Nutzer ausgelöst.
     * <p>
     * Bei Erststart wird Authentifizierung gestartet. Vorher findet keine automatische
     * Syncronisation statt.
     */
    public void syncManuallyClicked(MenuItem item) {
        accessToken = preferences.getString(Constants.DROPBOX_ACCESS_TOKEN, null);
        if (accessToken == null) {
            Intent assIntent = new Intent(context, DropboxAccDialog.class);
            assIntent.putExtra(DropboxAccDialog.AUTH_MODE, true);
            startActivityForResult(assIntent, Constants.REQUEST_CODE_DB_ACC_DIALOG);
        } else {
            startSync();
        }
    }

    private void startSync() {
        Intent syncIntent = new Intent(context, SyncDialog.class);
        syncIntent.putExtra(SyncDialog.ACCESS_TOKEN, accessToken);
        syncIntent.putExtra(SyncDialog.FILES, files);
        startActivityForResult(syncIntent, Constants.REQUEST_CODE_SYNC_DIALOG);
    }

    /**
     * Öffnet eine Lektion und beendet bei Erfolg die Activity.
     * @param ignored Wird nicht benötigt
     */
    public void mOpenLessonClicked(@Nullable MenuItem ignored) {
        openLesson(lastSelection);
    }

    private void openLesson(int position) {
        String filename = (String) listView.getItemAtPosition(position);
        try {
            PaukerManager.showToast((Activity) context, R.string.open_lesson_hint, Toast.LENGTH_SHORT);
            paukerManager.loadLessonFromFile(getFilePath(filename));
            paukerManager.setSaveRequired(false);
            finish();
        } catch (IOException e) {
            resetSelection(null);
            PaukerManager.showToast((Activity) context, getString(R.string.error_reading_from_xml), Toast.LENGTH_SHORT);
            ErrorReporter.instance().AddCustomData("ImportThread", "IOException?");
        }
    }

    public void downloadNewLesson(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(Html.fromHtml(getString(R.string.download_file_dialog_message), Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openBrowserForDownload();
                    }
                })
                .setNegativeButton(R.string.cancel, null);
        builder.create().show();
    }

    private void openBrowserForDownload() {
        String url = "http://pauker.sourceforge.net/pauker.php?page=lessons";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }
}
