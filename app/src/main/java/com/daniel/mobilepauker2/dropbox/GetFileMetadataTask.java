package com.daniel.mobilepauker2.dropbox;

import android.os.AsyncTask;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.Metadata;

import java.io.File;

public class GetFileMetadataTask extends AsyncTask<File, Void, Metadata> {
    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private DbxException mException;

    public interface Callback {
        void onDataLoaded(Metadata result);

        void onError(DbxException e);
    }

    GetFileMetadataTask(DbxClientV2 dbxClient, Callback callback) {
        mDbxClient = dbxClient;
        mCallback = callback;
    }

    @Override
    protected void onPostExecute(Metadata result) {
        super.onPostExecute(result);

        if (mException != null) {
            mCallback.onError(mException);
        } else {
            mCallback.onDataLoaded(result);
        }
    }

    @Override
    protected Metadata doInBackground(File... files) {
        try {
            return mDbxClient.files().getMetadata("/" + files[0].getName());
        } catch (DbxException e) {
            mException = e;
        }

        return null;
    }
}
