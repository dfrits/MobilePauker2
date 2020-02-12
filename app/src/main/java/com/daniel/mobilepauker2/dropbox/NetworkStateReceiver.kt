package com.daniel.mobilepauker2.dropbox

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import com.daniel.mobilepauker2.pauker_native.Log

class NetworkStateReceiver(private val callback: ReceiverCallback) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(
            TAG,
            "Network connectivity change"
        )
        if (intent.extras != null) {
            val cm =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val ni = cm.activeNetworkInfo
            if (ni != null && ni.isConnectedOrConnecting) {
                Log.i(
                    TAG,
                    "Network " + ni.typeName + " connected"
                )
            } else if (intent.getBooleanExtra(
                    ConnectivityManager.EXTRA_NO_CONNECTIVITY,
                    false
                )
            ) {
                Log.d(
                    TAG,
                    "There's no network connectivity"
                )
                callback.connectionLost()
            }
        }
    }

    interface ReceiverCallback {
        fun connectionLost()
    }

    companion object {
        private const val TAG = "NetworkStateReceiver"
    }

}