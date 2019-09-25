package com.daniel.mobilepauker2.dropbox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.model.ModelManager;
import com.daniel.mobilepauker2.utils.Constants;
import com.daniel.mobilepauker2.utils.ErrorReporter;
import com.daniel.mobilepauker2.utils.Log;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.DeleteResult;
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
    public static final String SYNC_ALL_ACTION = "SYNC_ALL_ACTION";
    public static final String UPLOAD_FILE_ACTION = "UPLOAD_FILE_ACTION";
    public static final String SYNC_FILE_ACTION = "SYNC_FILE_ACTION";
    private final Context context = this;
    private final ModelManager modelManager = ModelManager.instance();
    private final PaukerManager paukerManager = PaukerManager.instance();
    private File[] files;
    private Timer timeout;
    private TimerTask timerTask;
    private NetworkStateReceiver networkStateReceiver;

    private Button cancelbutton;

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
            finishDialog(RESULT_CANCELED);
            return;
        }

        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            final NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni == null || !ni.isConnected()) {
                showToast((Activity) context, "Internetverbindung prüfen!", Toast.LENGTH_LONG);
                finishDialog(RESULT_CANCELED);
                return;
            }
        }

        networkStateReceiver = new NetworkStateReceiver(new NetworkStateReceiver.ReceiverCallback() {
            @Override
            public void connectionLost() {
                showToast((Activity) context, "Internetverbindung prüfen!", Toast.LENGTH_LONG);
                finishDialog(RESULT_CANCELED);
            }
        });
        registerReceiver(networkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        DropboxClientFactory.init(accessToken);
        Serializable serializableExtra = intent.getSerializableExtra(FILES);
        startSync(intent, serializableExtra);
    }

    private void startSync(Intent intent, Serializable serializableExtra) {
        String action = intent.getAction();
        if (SYNC_ALL_ACTION.equals(action) && serializableExtra instanceof File[]) {
            syncAllFiles((File[]) serializableExtra);
        } else if (serializableExtra instanceof File) {
            syncFile((File) serializableExtra, action);
        } else {
            Log.d("SyncDialog::OnCreate", "Synchro mit falschem Extra gestartet");
            finishDialog(RESULT_CANCELED);
        }
    }

    /**
     * Synchronisiert alle Files mit denen auf Dropbox.
     * @param serializableExtra Lokale Files die synchronisiert werden sollen
     */
    private void syncAllFiles(File[] serializableExtra) {
        showProgressbar();
        files = serializableExtra;
        startTimer();
        loadData();
    }

    /**
     * Bei {@link SyncDialog#UPLOAD_FILE_ACTION UPLOAD_ACTION} wird das File hochgeladen.
     * Dabei wird nicht überprüft ob das neuer oder älter ist, als das File auf Dropbox. <br>
     * Bei {@link SyncDialog#SYNC_FILE_ACTION SYNC_FILE_ACTION} wird das File mit dem gleichnamigen
     * auf Dropbox verglichen. Ist das auf Dropbox neuer, wird dieses heruntergeladen.
     * @param serializableExtra File, das synchronisiert werden soll
     * @param action Spezifiziert was mit dem File geschehen soll
     */
    private void syncFile(final File serializableExtra, String action) {
        List<File> list = new ArrayList<>();
        if (serializableExtra.exists()) {
            Log.d("SyncDialog:syncFile", "File exists");
            Log.d("SyncDialog:syncFile", "Syncaction: " + action);
            if (UPLOAD_FILE_ACTION.equals(action)) {
                Log.d("SyncDialog:syncFile", "Upload just one file");
                list.add(serializableExtra);
                uploadFiles(list);
                finishDialog(RESULT_OK);
            } else if (SYNC_FILE_ACTION.equals(action)) {
                Log.d("SyncDialog:syncFile", "Download just one file");
                showProgressbar();
                showCancelButton();
                AsyncTask<File, Void, Metadata> getFileMetadataTask =
                        new GetFileMetadataTask(DropboxClientFactory.getClient(), new GetFileMetadataTask.Callback() {
                    @Override
                    public void onDataLoaded(Metadata metadata) {
                        Log.d("SyncDialog:syncFile::onDataLoaded", "Data loaded");
                        if (metadata instanceof FileMetadata) {
                            FileMetadata fileMetadata = (FileMetadata) metadata;
                            if (serializableExtra.lastModified() < fileMetadata.getClientModified().getTime()) {
                                Log.d("SyncDialog:syncFile::onDataLoaded", "File wird runtergeladen");
                                List<FileMetadata> metadataList = new ArrayList<>();
                                metadataList.add(fileMetadata);
                                downloadFiles(metadataList, (int) fileMetadata.getSize());
                            } else {
                                Log.d("SyncDialog:syncFile::onDataLoaded", "File wird NICHT runtergeladen");
                                finishDialog(RESULT_CANCELED);
                            }
                        } else if (metadata instanceof DeletedMetadata) {
                            showToast((Activity) context, "Datei ist nicht länger verfügbar auf Dropbox", Toast.LENGTH_LONG);
                            finishDialog(RESULT_CANCELED);
                        }
                    }

                    @Override
                    public void onError(DbxException e) {
                        showToast((Activity) context, R.string.error_synchronizing, Toast.LENGTH_LONG);
                        finishDialog(RESULT_CANCELED);
                    }
                }).execute(serializableExtra);
                tasks.add(getFileMetadataTask);
            } else {
                Log.d("SyncDialog:syncFile", "File does not exist");
                showToast((Activity) context, R.string.error_synchronizing, Toast.LENGTH_LONG);
                finishDialog(RESULT_CANCELED);
            }
        } else {
            Log.d("SyncDialog:syncFile", "File does not exist");
            showToast((Activity) context, R.string.error_file_not_found, Toast.LENGTH_LONG);
            finishDialog(RESULT_CANCELED);
        }
    }

    private void showProgressbar() {
        RelativeLayout progressBar = findViewById(R.id.pFrame);
        progressBar.setVisibility(View.VISIBLE);
        TextView title = findViewById(R.id.pTitle);
        title.setText(R.string.synchronizing);
    }

    private void showCancelButton() {
        cancelbutton = findViewById(R.id.cancel_button);
        cancelbutton.setVisibility(View.VISIBLE);
        Log.d("SyncDialog::showCancelButton", "Button is enabled: " + cancelbutton.isEnabled());
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
            public void onError(final DbxException e) {
                Log.d("LessonImportActivity::loadData::onError"
                        , "Error loading Files: " + e.getMessage());
                showToast((Activity) context,
                        R.string.simple_error_message,
                        Toast.LENGTH_SHORT);
                cancelTasks();
                if (e.getRequestId() != null && e.getRequestId().equals("401")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Dropbox token is invalid!")
                            .setMessage("There is something wrong with the dropbox token. Maybe it is " +
                                    "solved by the next try. If this doesn't work" +
                                    "please get in contact.")
                            .setPositiveButton(R.string.ok, null)
                            .setNeutralButton("Send E-Mail", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ErrorReporter reporter = ErrorReporter.instance();
                                    reporter.init(context);
                                    reporter.uncaughtException(null, e);
                                    reporter.CheckErrorAndSendMail();
                                }
                            })
                            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    PreferenceManager.getDefaultSharedPreferences(context).edit()
                                            .putString(Constants.DROPBOX_ACCESS_TOKEN, null).apply();
                                    finishDialog(RESULT_CANCELED);
                                }
                            })
                            .setCancelable(false);
                    builder.create().show();
                } else {
                    finishDialog(RESULT_CANCELED);
                }
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

        AsyncTask<String, Void, List<DeleteResult>> deleteTask;
        deleteTask = new DeleteFileTask(DropboxClientFactory.getClient(), new DeleteFileTask.Callback() {
            @Override
            public void onDeleteComplete(List<DeleteResult> result) {
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
                        Log.d("SyncDialog:downloadFiles", "Download startet");
                        progressBar.setMax(downloadSize);
                        progressBar.setIndeterminate(false);
                    }

                    @Override
                    public void onProgressUpdate(FileMetadata metadata) {
                        Log.d("SyncDialog:downloadFiles", "Download update: " + progressBar.getProgress() + metadata.getSize());
                        progressBar.setProgress((int) (progressBar.getProgress() + metadata.getSize()));
                    }

                    @Override
                    public void onDownloadComplete(File[] result) {
                        Log.d("SyncDialog:downloadFiles", "Download complete");
                        progressBar.setIndeterminate(true);
                        finishDialog(RESULT_OK);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("LessonImportActivity::downloadFiles",
                                "Failed to download file.", e);

                        showToast((Activity) context,
                                R.string.simple_error_message,
                                Toast.LENGTH_SHORT)
                        ;
                        finishDialog(RESULT_CANCELED);
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
                Log.d("SyncDialog:uploadFiles", "upload success");
            }

            @Override
            public void onError(final Exception e) {
                Log.e("SyncDialog::uploadFiles",
                        "upload error: ", e);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showToast((Activity) context, e.getMessage(), Toast.LENGTH_LONG);
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
        Log.d("SyncDialog::TouchEvent", "Touched");
        if(cancelbutton != null) {
            int[] pos = new int[2];
            cancelbutton.getLocationInWindow(pos);
            if ((ev.getY() <= (pos[1] + cancelbutton.getHeight()) && ev.getX() > pos[0])
                    && (ev.getY() > pos[1] && ev.getX() <= (pos[0] + cancelbutton.getWidth()))
                    && ev.getAction() == MotionEvent.ACTION_UP) {
                cancelClicked(cancelbutton);
            }
        }
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
                        showToast((Activity) context, R.string.synchro_timeout, Toast.LENGTH_SHORT);
                    }
                });

                finishDialog(RESULT_CANCELED);
            }
        };
        timeout.schedule(timerTask, 60000);
    }

    public void cancelClicked(View view) {
        Log.d("SyncDialog::cancelClicked", "Cancel Sync");
        view.setEnabled(false);
        PaukerManager.showToast((Activity) context, R.string.synchro_canceled_by_user, Toast.LENGTH_LONG);
        finishDialog(RESULT_CANCELED);
    }

    private void cancelTasks() {
        for (AsyncTask task : tasks) {
            if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
                task.cancel(false);
                tasks.remove(task);
            }
        }
    }

    private void finishDialog(int result) {
        setResult(result);
        finish();
    }
}
