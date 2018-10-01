package com.androidapps.robertsteele.photogallery;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartupReceiver extends android.content.BroadcastReceiver {

    private static final String TAG = "StartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received Broadcast Intent" + intent.getAction());

        boolean isOn = QueryPreferences.isAlarmOn(context);
        PollService.setServiceAlarm(context, isOn);
    }
}


