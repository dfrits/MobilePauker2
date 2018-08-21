package com.daniel.mobilepauker2.dropbox;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.model.ModelManager;
import com.daniel.mobilepauker2.utils.Constants;
import com.daniel.mobilepauker2.utils.Log;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.DeletedMetadata;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by dfritsch on 21.03.2018.
 * veesy.de
 * hs-augsburg
 */

public class SyncDialog extends Activity {
    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String FILES = "FILES";
    private final Context context = this;
    private final ModelManager modelManager = ModelManager.instance();
    private final PaukerManager paukerManager = PaukerManager.instance();
    private String accessToken;
    private File[] files;
    private Timer timeout;
    private TimerTask timerTask;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.progress_dialog);

        RelativeLayout progressBar = findViewById(R.id.pFrame);
        progressBar.setVisibility(View.VISIBLE);
        TextView title = findViewById(R.id.pTitle);
        title.setText(R.string.synchronizing);

        Intent intent = getIntent();
        accessToken = intent.getStringExtra(ACCESS_TOKEN);
        if (accessToken == null) {
            Log.d("SyncDialog::OnCreate", "Synchro mit accessToken = null gestartet");
            setResult(RESULT_CANCELED);
            finish();
        }

        Serializable serializableExtra = intent.getSerializableExtra(FILES);
        if (serializableExtra instanceof File[]) {
            files = (File[]) serializableExtra;
        } else {
            Log.d("SyncDialog::OnCreate", "Synchro mit falschem Extra gestartet");
            setResult(RESULT_CANCELED);
            finish();
        }

        startTimer();

        loadData();
    }

    /**
     * Sucht nach allen Dateien im Dropboxordner, validiert sie und ruft dann
     * {@link SyncDialog#syncWithFiles(List, List) synWithFiles()} auf.
     */
    private void loadData() {
        DropboxClientFactory.init(accessToken);

        new ListFolderTask(DropboxClientFactory.getClient(), new ListFolderTask.Callback() {
            @Override
            public void onDataLoaded(ListFolderResult result) {
                List<Metadata> dbFiles = new ArrayList<>(); // Dateien in Dropbox mit der passenden Endung
                List<Metadata> dbDeletedFiles = new ArrayList<>(); // Dateien, die in Dropbox gelöscht wurden

                while (true) {
                    List<Metadata> entries = result.getEntries();

                    for (Metadata entry : entries) {
                        if (paukerManager.validateFilename(context, entry.getName())) {
                            if (entry instanceof DeletedMetadata) {
                                dbDeletedFiles.add(entry);
                            } else {
                                dbFiles.add(entry);
                            }
                        }
                    }

                    if (!result.getHasMore()) break;

                    try {
                        result = DropboxClientFactory.getClient().files()
                                .listFolderContinue(result.getCursor());
                    } catch (DbxException e) {
                        e.printStackTrace();
                    }
                }

                deleteFiles(dbDeletedFiles, dbFiles);
            }

            @Override
            public void onError(Exception e) {
                setResult(RESULT_CANCELED);
                finish();

                Log.d("LessonImportActivity::loadData::onError"
                        , "Error loading Files: " + e.getMessage());
                Toast.makeText(context,
                        "An error has occurred",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }).execute(Constants.DROPBOX_PATH);
    }

    /**
     * Löscht die Dateien auf Dropbox und Lokal.
     * @param deletedFiles   Liste von Metadaten der Dateien auf Dropbox, welche lokal gelöscht
     *                       werden sollen
     * @param validatedFiles Files, die anschließend syncronisiert werden sollen
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteFiles(List<Metadata> deletedFiles, final List<Metadata> validatedFiles) {
        final List<File> localDeletedFiles = getLokalDeletedFiles(); // Dateien, die lokal gelöscht wurden

        if (files != null) {
            List<File> localFilesTMP = new ArrayList<>(Arrays.asList(files));
            for (int i = 0; i < deletedFiles.size(); i++) {
                Metadata metadata = deletedFiles.get(i);
                if (metadata instanceof DeletedMetadata) {
                    DeletedMetadata deletedMetadata = (DeletedMetadata) metadata;
                    int index = getFileIndex(deletedMetadata.getName(), localFilesTMP);
                    if (index > -1) {
                        localFilesTMP.get(index).delete();
                    }
                }
            }
        }

        File[] data = new File[localDeletedFiles.size()];
        data = localDeletedFiles.toArray(data);

        new DeleteFileTask(DropboxClientFactory.getClient(), new DeleteFileTask.Callback() {
            @Override
            public void onDeleteComplete(List<Metadata> result) {
                syncWithFiles(validatedFiles, localDeletedFiles);
            }

            @Override
            public void onError(Exception e) {
                System.out.println();
            }
        }).execute(data);
    }

    @NonNull
    private List<File> getLokalDeletedFiles() {
        final List<File> filesToDelete = new ArrayList<>();
        try {
            FileInputStream fis = openFileInput(Constants.DELETED_FILES_NAMES_FILE_NAME);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String fileName = reader.readLine();
            while (fileName != null) {
                if (!fileName.trim().isEmpty()) {
                    filesToDelete.add(new File(fileName));
                }
                fileName = reader.readLine();
            }
            modelManager.resetDeletedFilesData(context);
        } catch (IOException e) {
            // TODO Error
        }
        return filesToDelete;
    }

    /**
     * Aktualisiert die Files und ladet gegebenenfalls runter bzw hoch.
     * @param metadataList      Liste von Metadaten der Dateien auf Dropbox, welche mit den lokalen
     *                          Dateien verglichen werden sollen
     * @param localDeletedFiles Liste von Files, die im lokalen Speicher gelöscht wurden
     */
    private void syncWithFiles(List<Metadata> metadataList, List<File> localDeletedFiles) {
        // Lokale Files in Liste umwandeln
        List<File> localFilesTMP;
        if (files != null) {
            localFilesTMP = new ArrayList<>(Arrays.asList(files));
        } else {
            localFilesTMP = new ArrayList<>();
        }

        List<File> lokal = new ArrayList<>(); // Zum Hochladen
        List<FileMetadata> dropB = new ArrayList<>(); // Zum Runterladen
        int downloadSize = 0; // Die Gesamtgröße zum runterladen

        for (int i = 0; i < metadataList.size(); i++) {
            if (metadataList.get(i) instanceof FileMetadata) {
                FileMetadata metadata = (FileMetadata) metadataList.get(i);

                if (paukerManager.validateFilename(context, metadata.getName())) {
                    int fileIndex = getFileIndex(metadata.getName(), localFilesTMP);

                    if (fileIndex == -1) {
                        if (getFileIndex(metadata.getName(), localDeletedFiles) == -1) {
                            dropB.add(metadata);
                            downloadSize += metadata.getSize();
                        }
                    } else {
                        long serverTime = metadata.getClientModified().getTime();
                        long localTime = localFilesTMP.get(fileIndex).lastModified();
                        if (serverTime < localTime) {
                            lokal.add(localFilesTMP.get(fileIndex));
                        } else if (serverTime > localTime){
                            dropB.add(metadata);
                            downloadSize += metadata.getSize();
                        }
                        localFilesTMP.remove(fileIndex);
                    }
                }
            }
        }

        // Alle übrigen Dateien in der Liste sind lokal neu hinzugefügt worden und werden hochgeladen.
        if (!localFilesTMP.isEmpty()) lokal.addAll(localFilesTMP);

        if (!lokal.isEmpty()) {
            uploadFiles(lokal);
        }

        if (!dropB.isEmpty()) {
            downloadFiles(dropB, downloadSize);
        } else {
            setResult(RESULT_OK);
            finish();
        }
    }

    /**
     * Ladet die Dateien in den Ordner herunter und aktualisiert die Liste.
     * @param list Liste mit den Dateien, die heruntergeladen werden sollen
     */
    private void downloadFiles(final List<FileMetadata> list, final int downloadSize) {
        FileMetadata[] data = new FileMetadata[list.size()];
        data = list.toArray(data);
        final ProgressBar progressBar = findViewById(R.id.pBar);
        new DownloadFileTask(DropboxClientFactory.getClient(),
                new DownloadFileTask.Callback() {
                    @Override
                    public void onDownloadStartet() {
                        progressBar.setMax(downloadSize);
                        progressBar.setIndeterminate(false);
                    }

                    @Override
                    public void onProgressUpdate(FileMetadata metadata) {
                        progressBar.setProgress((int) (progressBar.getProgress() + metadata.getSize()));
                    }

                    @Override
                    public void onDownloadComplete(File[] result) {
                        setResult(RESULT_OK);
                        progressBar.setIndeterminate(true);
                        finish();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("LessonImportActivity::downloadFiles",
                                "Failed to download file.", e);

                        Toast.makeText(context,
                                "An error has occurred",
                                Toast.LENGTH_SHORT)
                                .show();
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }).execute(data);
    }

    /**
     * Lädt die Dateien in den Dropboxordner hoch.
     * @param list Liste mit Dateien, die hochgeladen werden sollen
     */
    private void uploadFiles(List<File> list) {
        File[] data = new File[list.size()];
        data = list.toArray(data);
        new UploadFileTask(DropboxClientFactory.getClient(), new UploadFileTask.Callback() {
            @Override
            public void onUploadComplete(List<Metadata> result) {
                System.out.println();
            }

            @Override
            public void onError(Exception e) {
                Log.e("LessonImportActivity::uploadFiles",
                        "Failed to upload file.", e);
            }
        }).execute(data);
    }

    /**
     * Sucht nach dem ersten Index des File mit dem übergebenen Namen.
     * @param fileName Name des Files
     * @param list     Liste in der gesucht werden soll
     * @return Index. -1, falls File nicht vorhanden ist
     */
    private int getFileIndex(String fileName, List<File> list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equals(fileName)) return i;
        }

        return -1;
    }

    // Touchevents und Backbutton blockieren, dass er nicht minimiert werden kann

    @Override
    public void onBackPressed() {
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (timeout != null && timerTask != null) {
            timeout.cancel();
            startTimer();
        }
    }

    private void startTimer() {
        timeout = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, R.string.synchro_timeout, Toast.LENGTH_SHORT).show();
                    }
                });

                setResult(RESULT_CANCELED);
                finish();
            }
        };
        timeout.schedule(timerTask, 60000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timeout != null && timerTask != null) {
            timeout.cancel();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return false;
    }
}
