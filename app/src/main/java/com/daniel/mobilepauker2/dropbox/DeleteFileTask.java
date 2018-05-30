package com.daniel.mobilepauker2.dropbox;

import android.os.AsyncTask;

import com.daniel.mobilepauker2.utils.Constants;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.Metadata;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Daniel on 14.03.2018.
 * Masterarbeit:
 * MobilePauker++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 *
 * Löscht die Dateien auf Dropbox.
 */

public class DeleteFileTask extends AsyncTask<File, Void, List<Metadata>> {
    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private Exception mException;

    public interface Callback {
        void onDeleteComplete(List<Metadata> result);
        void onError(Exception e);
    }

    public DeleteFileTask(DbxClientV2 dbxClient, Callback callback) {
        mDbxClient = dbxClient;
        mCallback = callback;
    }

    @Override
    protected void onPostExecute(List<Metadata> result) {
        super.onPostExecute(result);
        if (mException != null) {
            mCallback.onError(mException);
        } else if (result == null) {
            mCallback.onError(null);
        } else {
            mCallback.onDeleteComplete(result);
        }
    }

    @Override
    protected List<Metadata> doInBackground(File... params) {
        File remoteFolderPath = new File(Constants.DROPBOX_PATH);
        List<Metadata> data = new ArrayList<>();

        for (File localFile : params) {
            if (localFile != null) {

                String remoteFileName = localFile.getName();
                remoteFileName = remoteFileName.substring(0, remoteFileName.indexOf(";*;"));

                try {
                    data.add(mDbxClient.files().delete(remoteFolderPath + "/" + remoteFileName));
                } catch (DbxException e) {
                    mException = e;
                }
            }
        }

        return data;
    }
}
