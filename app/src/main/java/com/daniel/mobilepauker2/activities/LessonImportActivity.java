package com.daniel.mobilepauker2.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.dropbox.SyncDialog;
import com.daniel.mobilepauker2.model.LessonImportAdapter;
import com.daniel.mobilepauker2.model.ModelManager;
import com.daniel.mobilepauker2.model.pauker_native.Lesson;
import com.daniel.mobilepauker2.model.xmlsupport.FlashCardXMLPullFeedParser;
import com.daniel.mobilepauker2.utils.Constants;
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
        RelativeLayout progressBar = findViewById(R.id.pFrame);
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
                Toast.makeText(context, R.string.error_reading_from_xml, Toast.LENGTH_SHORT).show();
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
        if (!paukerManager.validateFilename(context, filename)) {
            Toast.makeText(context, R.string.error_filename_invalid, Toast.LENGTH_LONG).show();
            throw new IOException("Filename invalid");
        }

        String filePath = Environment.getExternalStorageDirectory() + paukerManager.getApplicationDataDirectory() + filename;
        return new File(filePath);
    }

    private void loadLessonFromFile(File file) throws IOException {
        URI uri = file.toURI();
        FlashCardXMLPullFeedParser xmlFlashCardFeedParser = new FlashCardXMLPullFeedParser(uri.toURL());
        Lesson lesson = xmlFlashCardFeedParser.parse();
        paukerManager.setCurrentFileName(file.getName());
        paukerManager.setFileAbsolutePath(file.getAbsolutePath());
        modelManager.setLesson(lesson);
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
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
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
                                    } else {
                                        Toast.makeText(context, R.string.delete_lesson_error, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
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

        accessToken = preferences.getString(Constants.DROPBOX_ACCESS_TOKEN, null);
        if (accessToken == null) {
            accessToken = Auth.getOAuth2Token();
            if (accessToken != null) {
                preferences.edit().putString(Constants.DROPBOX_ACCESS_TOKEN, accessToken).apply();
                startSync();
            }
        }

        String uid = Auth.getUid();
        String storedUid = preferences.getString(Constants.DROPBOX_USER_ID, null);
        if (uid != null && !uid.equals(storedUid)) {
            preferences.edit().putString(Constants.DROPBOX_USER_ID, uid).apply();
        }
    }

    @Override
    public void finish() {
        if (errorMessage != null) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
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
                Toast.makeText(context, R.string.error_synchronizing, Toast.LENGTH_SHORT).show();
            }
            init();
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
        if (accessToken == null) {
            Auth.startOAuth2Authentication(this, Constants.DROPBOX_APP_KEY);
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
            Toast.makeText(context, R.string.open_lesson_hint, Toast.LENGTH_SHORT).show();
            loadLessonFromFile(getFilePath(filename));
            paukerManager.setSaveRequired(false);
            finish();
        } catch (IOException e) {
            resetSelection(null);
            Toast.makeText(context, getString(R.string.error_reading_from_xml), Toast.LENGTH_SHORT).show();
        }
    }
}
