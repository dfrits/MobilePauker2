package com.daniel.mobilepauker2.dropbox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.daniel.mobilepauker2.utils.Log;

public class NetworkStateReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkStateReceiver";
    private ReceiverCallback callback;

    public NetworkStateReceiver(ReceiverCallback callback) {
        super();
        this.callback = callback;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {

        Log.d(TAG, "Network connectivity change");

        if (intent.getExtras() != null) {
            final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                final NetworkInfo ni = cm.getActiveNetworkInfo();

                if (ni != null && ni.isConnectedOrConnecting()) {
                    Log.i(TAG, "Network " + ni.getTypeName() + " connected");
                } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
                    Log.d(TAG, "There's no network connectivity");
                    callback.connectionLost();
                }
            }
        }
    }

    public interface ReceiverCallback {
        void connectionLost();
    }
}
