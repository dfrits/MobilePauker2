package com.daniel.mobilepauker2.dropbox;

import android.os.AsyncTask;

import com.daniel.mobilepauker2.utils.Constants;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DeleteResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Daniel on 14.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 * <p>
 * Löscht die Dateien auf Dropbox.
 */

public class DeleteFileTask extends AsyncTask<String, Void, List<DeleteResult>> {
    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;

    public interface Callback {
        void onDeleteComplete(List<DeleteResult> result);

        void onError(Exception e);
    }

    DeleteFileTask(DbxClientV2 dbxClient, Callback callback) {
        mDbxClient = dbxClient;
        mCallback = callback;
    }

    @Override
    protected void onPostExecute(List<DeleteResult> result) {
        super.onPostExecute(result);
        mCallback.onDeleteComplete(result);
    }

    @Override
    protected List<DeleteResult> doInBackground(String... params) {
        File remoteFolderPath = new File(Constants.DROPBOX_PATH);
        List<DeleteResult> data = new ArrayList<>();

        for (String localFile : params) {
            if (localFile != null && !localFile.isEmpty()) {
                try {
                    data.add(mDbxClient.files().deleteV2(remoteFolderPath + "/" + localFile));
                } catch (DbxException e) {
                    mCallback.onError(e);
                }
            }
        }

        return data;
    }
}
