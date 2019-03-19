package com.daniel.mobilepauker2.dropbox;

import android.os.AsyncTask;
import android.os.Environment;

import com.daniel.mobilepauker2.PaukerManager;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Task to download a file from Dropbox and put it in the Downloads folder
 */
public class DownloadFileTask extends AsyncTask<FileMetadata, FileMetadata, File[]> {

    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;

    DownloadFileTask(DbxClientV2 dbxClient, Callback callback) {
        mDbxClient = dbxClient;
        mCallback = callback;
    }

    @Override
    protected void onPostExecute(File[] result) {
        super.onPostExecute(result);
        mCallback.onDownloadComplete(result);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mCallback.onDownloadStartet();
    }

    @Override
    protected void onProgressUpdate(FileMetadata... values) {
        super.onProgressUpdate(values);
        mCallback.onProgressUpdate(values[0]);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected File[] doInBackground(FileMetadata... params) {
        File downloadFile = null;
        try {
            List<File> list = new ArrayList<>();

            for (FileMetadata metadata : params) {
                File path = new File(Environment.getExternalStorageDirectory()
                        + PaukerManager.instance().getApplicationDataDirectory());
                File file = new File(path, metadata.getName());

                // Make sure the Downloads directory exists.
                if (!path.exists() && !path.mkdirs()) {
                    mCallback.onError(new RuntimeException("Unable to access directory: " + path));
                    return null;
                } else if (!path.isDirectory()) {
                    mCallback.onError(new IllegalStateException("Download path is not a directory: " + path));
                    return null;
                }

                // Download the file.
                downloadFile = new File(file.getParent(), "downloadFile.pau.gz");
                try (OutputStream outputStream = new FileOutputStream(downloadFile)) {
                    mDbxClient.files().download(metadata.getPathLower(), metadata.getRev())
                            .download(outputStream);
                    outputStream.close();
                    downloadFile.renameTo(file);
                }

                list.add(file);
                publishProgress(metadata);
            }

            File[] result = new File[list.size()];
            return list.toArray(result);
        } catch (DbxException | IOException e) {
            mCallback.onError(e);
        } finally {
            if (downloadFile != null && downloadFile.exists()) {
                downloadFile.delete();
            }
        }

        return null;
    }

    public interface Callback {
        void onDownloadStartet();

        void onProgressUpdate(FileMetadata metadata);

        void onDownloadComplete(File[] result);

        void onError(Exception e);
    }
}
