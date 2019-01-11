package com.daniel.mobilepauker2.dropbox;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.daniel.mobilepauker2.PaukerManager.showToast;

/**
 * Created by dfritsch on 21.11.2018.
 * MobilePauker++
 */

public class SyncDialog extends Activity {
    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String FILES = "FILES";
    private final Context context = this;
    private final ModelManager modelManager = ModelManager.instance();
    private final PaukerManager paukerManager = PaukerManager.instance();
    private File[] files;
    private Timer timeout;
    private TimerTask timerTask;
    private NetworkStateReceiver networkStateReceiver;

    // Hintergrundtasks
    private List<AsyncTask> tasks = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.progress_dialog);

        Intent intent = getIntent();
        String accessToken = intent.getStringExtra(ACCESS_TOKEN);
        if (accessToken == null) {
            Log.d("SyncDialog::OnCreate", "Synchro mit accessToken = null gestartet");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            final NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni == null || !ni.isConnected()) {
                showToast((Activity) context, "Internetverbindung prüfen!", Toast.LENGTH_LONG);
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }

        networkStateReceiver = new NetworkStateReceiver(new NetworkStateReceiver.ReceiverCallback() {
            @Override
            public void connectionLost() {
                showToast((Activity) context, "Internetverbindung prüfen!", Toast.LENGTH_LONG);
                finish();
            }
        });
        registerReceiver(networkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        DropboxClientFactory.init(accessToken);
        Serializable serializableExtra = intent.getSerializableExtra(FILES);
        if (serializableExtra instanceof File[]) {
            RelativeLayout progressBar = findViewById(R.id.pFrame);
            progressBar.setVisibility(View.VISIBLE);
            TextView title = findViewById(R.id.pTitle);
            title.setText(R.string.synchronizing);
            files = (File[]) serializableExtra;
            startTimer();
            loadData();
        } else if (serializableExtra instanceof File) {
            List<File> list = new ArrayList<>();
            File file = (File) serializableExtra;
            if (file.exists()) {
                list.add((File) serializableExtra);
                uploadFiles(list);
                setResult(RESULT_OK);
                finish();
            } else {
                showToast((Activity)context, R.string.error_file_not_found, Toast.LENGTH_LONG);
                setResult(RESULT_CANCELED);
                finish();
            }
        } else {
            Log.d("SyncDialog::OnCreate", "Synchro mit falschem Extra gestartet");
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    /**
     * Sucht nach allen Dateien im Dropboxordner, validiert sie und ruft dann
     * {@link SyncDialog#syncWithFiles(List, Map) synWithFiles()} auf.
     */
    private void loadData() {
        AsyncTask<String, Void, ListFolderResult> listFolderTask;
        listFolderTask = new ListFolderTask(DropboxClientFactory.getClient(), new ListFolderTask.Callback() {
            @Override
            public void onDataLoaded(ListFolderResult result) {
                List<Metadata> dbFiles = new ArrayList<>(); // Dateien in Dropbox mit der passenden Endung
                List<Metadata> dbDeletedFiles = new ArrayList<>(); // Dateien, die in Dropbox gelöscht wurden
                List<String> lokalAddedFiles = modelManager.getLokalAddedFiles(context);

                while (true) {
                    List<Metadata> entries = result.getEntries();

                    for (Metadata entry : entries) {
                        if (paukerManager.validateFilename(entry.getName())) {
                            if (entry instanceof DeletedMetadata) {
                                if (!lokalAddedFiles.contains(entry.getName())) {
                                    dbDeletedFiles.add(entry);
                                }
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
                showToast((Activity)context,
                        R.string.simple_error_message,
                        Toast.LENGTH_SHORT)
                        ;
            }
        }).execute(Constants.DROPBOX_PATH);
        tasks.add(listFolderTask);
    }

    /**
     * Löscht die Dateien auf Dropbox und Lokal.
     * @param deletedFiles   Liste von Metadaten der Dateien auf Dropbox, welche lokal gelöscht
     *                       werden sollen
     * @param validatedFiles Files, die anschließend syncronisiert werden sollen
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteFiles(List<Metadata> deletedFiles, final List<Metadata> validatedFiles) {
        final Map<String, String> lokalDeletedFiles = modelManager.getLokalDeletedFiles(context); // Dateien, die lokal gelöscht wurden

        if (files != null) {
            List<File> filesTMP = new ArrayList<>(Arrays.asList(files));
            for (int i = 0; i < deletedFiles.size(); i++) {
                if (deletedFiles.get(i) instanceof DeletedMetadata) {
                    DeletedMetadata metadata = (DeletedMetadata) deletedFiles.get(i);
                    int index = getFileIndex(new File(metadata.getName()), filesTMP);
                    if (index > -1) {
                        filesTMP.get(index).delete();
                    }
                }
            }
        }

        String[] data = new String[lokalDeletedFiles.keySet().size()];
        data = lokalDeletedFiles.keySet().toArray(data);

        AsyncTask<String, Void, List<Metadata>> deleteTask;
        deleteTask = new DeleteFileTask(DropboxClientFactory.getClient(), new DeleteFileTask.Callback() {
            @Override
            public void onDeleteComplete(List<Metadata> result) {
                modelManager.resetDeletedFilesData(context);
                syncWithFiles(validatedFiles, lokalDeletedFiles);
            }

            @Override
            public void onError(Exception e) {
                System.out.println();
            }
        }).execute(data);
        tasks.add(deleteTask);
    }

    /**
     * Aktualisiert die Files und ladet gegebenenfalls runter bzw hoch.
     * @param metadataList      Liste von Metadaten der Dateien auf Dropbox, welche mit den lokalen
     *                          Dateien verglichen werden sollen
     * @param lokalDeletedFiles Liste von Files, die im lokalen Speicher gelöscht wurden
     */
    private void syncWithFiles(List<Metadata> metadataList, Map<String, String> lokalDeletedFiles) {
        // Lokale Files kopieren, damit nichts verloren geht
        List<File> filesTMP;
        if (files != null) {
            filesTMP = new ArrayList<>(Arrays.asList(files));
        } else {
            filesTMP = new ArrayList<>();
        }

        List<File> lokal = new ArrayList<>(); // Zum Hochladen
        List<FileMetadata> dropB = new ArrayList<>(); // Zum Runterladen
        int downloadSize = 0; // Die Gesamtgröße zum runterladen

        for (int i = 0; i < metadataList.size(); i++) {
            if (metadataList.get(i) instanceof FileMetadata) {
                FileMetadata metadata = (FileMetadata) metadataList.get(i);

                if (paukerManager.validateFilename(metadata.getName())) {
                    int fileIndex = getFileIndex(new File(metadata.getName()), filesTMP);

                    if (fileIndex == -1) {
                        if (getFileIndex(metadata.getName(), lokalDeletedFiles.keySet()) == -1) {
                            dropB.add(metadata);
                            downloadSize += metadata.getSize();
                        }
                    } else {
                        if (metadata.getClientModified().getTime() < filesTMP.get(fileIndex).lastModified()) {
                            lokal.add(filesTMP.get(fileIndex));
                        } else {
                            dropB.add(metadata);
                            downloadSize += metadata.getSize();
                        }
                        filesTMP.remove(fileIndex);
                    }
                }
            }
        }

        if (!filesTMP.isEmpty()) lokal.addAll(filesTMP);

        if (!lokal.isEmpty()) {
            uploadFiles(lokal);
        }

        if (!dropB.isEmpty()) {
            downloadFiles(dropB, downloadSize);
        } else {
            setResult(modelManager.resetIndexFiles(context) ? RESULT_OK : RESULT_CANCELED);
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
        AsyncTask<FileMetadata, FileMetadata, File[]> downloadTask;
        downloadTask = new DownloadFileTask(DropboxClientFactory.getClient(),
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

                        showToast((Activity)context,
                                R.string.simple_error_message,
                                Toast.LENGTH_SHORT)
                                ;
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }).execute(data);
        tasks.add(downloadTask);
    }

    /**
     * Lädt die Dateien in den Dropboxordner hoch.
     * @param list Liste mit Dateien, die hochgeladen werden sollen
     */
    private void uploadFiles(List<File> list) {
        File[] data = new File[list.size()];
        for (int i = 0; i < list.size(); i++) {
            File file = list.get(i);
            if (file.exists()) data[i] = file;
        }
        AsyncTask<File, Void, List<Metadata>> uploadTask;
        uploadTask = new UploadFileTask(DropboxClientFactory.getClient(), new UploadFileTask.Callback() {
            @Override
            public void onUploadComplete(List<Metadata> result) {
                modelManager.resetAddedFilesData(context);
            }

            @Override
            public void onError(final Exception e) {
                Log.e("LessonImportActivity::uploadFiles",
                        "Failed to upload file.", e);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showToast((Activity)context, e.getMessage(), Toast.LENGTH_LONG);
                    }
                });
            }
        }).execute(data);
        tasks.add(uploadTask);
    }

    /**
     * Sucht nach dem ersten Index des Files mit dem übergebenen Namen.
     * @param file Name des Files
     * @param list Liste in der gesucht werden soll
     * @return Index. -1, falls File nicht vorhanden ist
     */
    private int getFileIndex(File file, Collection<File> list) {
        int i = 0;
        for (File item : list) {
            if (item.getName().equals(file.getName())) return i;
            i++;
        }

        return -1;
    }

    /**
     * Sucht nach dem ersten Index des Files mit dem übergebenen Namen.
     * @param fileName Name des Files
     * @param list     Liste in der gesucht werden soll
     * @return Index. -1, falls File nicht vorhanden ist
     */
    private int getFileIndex(String fileName, Collection<String> list) {
        int i = 0;
        for (String name : list) {
            if (name.equals(fileName)) return i;
            i++;
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

    @Override
    protected void onDestroy() {
        if (timeout != null && timerTask != null) {
            timeout.cancel();
        }
        if (networkStateReceiver != null) {
            unregisterReceiver(networkStateReceiver);
        }
        cancelTasks();
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return false;
    }

    private void startTimer() {
        timeout = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showToast((Activity)context, R.string.synchro_timeout, Toast.LENGTH_SHORT);
                    }
                });

                setResult(RESULT_CANCELED);
                finish();
            }
        };
        timeout.schedule(timerTask, 60000);
    }

    private void cancelTasks() {
        for (AsyncTask task : tasks) {
            if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
                task.cancel(true);
            }
        }
    }
}
