package com.roy.downloader.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.roy.downloader.core.utils.Utils;
import com.roy.downloader.service.DownloadService;
import com.roy.downloader.ui.main.MainActivity;

/*
 * The receiver for actions of foreground notification, added by service.
 */

public class NotificationReceiver extends BroadcastReceiver {
    public static final String NOTIFY_ACTION_SHUTDOWN_APP = "com.roy.downloader.receiver.NotificationReceiver.NOTIFY_ACTION_SHUTDOWN_APP";
    public static final String NOTIFY_ACTION_PAUSE_ALL = "com.roy.downloader.receiver.NotificationReceiver.NOTIFY_ACTION_PAUSE_ALL";
    public static final String NOTIFY_ACTION_RESUME_ALL = "com.roy.downloader.receiver.NotificationReceiver.NOTIFY_ACTION_RESUME_ALL";
    public static final String NOTIFY_ACTION_PAUSE_RESUME = "com.roy.downloader.receiver.NotificationReceiver.NOTIFY_ACTION_PAUSE_RESUME";
    public static final String NOTIFY_ACTION_CANCEL = "com.roy.downloader.receiver.NotificationReceiver.NOTIFY_ACTION_CANCEL";
    public static final String NOTIFY_ACTION_REPORT_APPLYING_PARAMS_ERROR = "com.roy.downloader.receiver.NotificationReceiver.NOTIFY_ACTION_REPORT_APPLYING_PARAMS_ERROR";
    public static final String TAG_ID = "id";
    public static final String TAG_ERR = "err";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null)
            return;
        Intent mainIntent, serviceIntent;
        switch (action) {
            /* Send action to the already running service */
            case NOTIFY_ACTION_SHUTDOWN_APP -> {
                mainIntent = new Intent(context.getApplicationContext(), MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mainIntent.setAction(NOTIFY_ACTION_SHUTDOWN_APP);
                context.startActivity(mainIntent);
                serviceIntent = new Intent(context.getApplicationContext(), DownloadService.class);
                serviceIntent.setAction(NOTIFY_ACTION_SHUTDOWN_APP);
                context.startService(serviceIntent);
            }
            case NOTIFY_ACTION_PAUSE_ALL -> {
                serviceIntent = new Intent(context.getApplicationContext(), DownloadService.class);
                serviceIntent.setAction(NOTIFY_ACTION_PAUSE_ALL);
                context.startService(serviceIntent);
            }
            case NOTIFY_ACTION_RESUME_ALL -> {
                serviceIntent = new Intent(context.getApplicationContext(), DownloadService.class);
                serviceIntent.setAction(NOTIFY_ACTION_RESUME_ALL);
                context.startService(serviceIntent);
            }
            case NOTIFY_ACTION_PAUSE_RESUME -> {
                serviceIntent = new Intent(context.getApplicationContext(), DownloadService.class);
                serviceIntent.setAction(NOTIFY_ACTION_PAUSE_RESUME);
                serviceIntent.putExtra(TAG_ID, intent.getSerializableExtra(TAG_ID));
                context.startService(serviceIntent);
            }
            case NOTIFY_ACTION_CANCEL -> {
                serviceIntent = new Intent(context.getApplicationContext(), DownloadService.class);
                serviceIntent.setAction(NOTIFY_ACTION_CANCEL);
                serviceIntent.putExtra(TAG_ID, intent.getSerializableExtra(TAG_ID));
                context.startService(serviceIntent);
            }
            case NOTIFY_ACTION_REPORT_APPLYING_PARAMS_ERROR -> {
                Throwable e = (Throwable) intent.getSerializableExtra(TAG_ERR);
                if (e != null)
                    Utils.reportError(e, null);
            }
        }
    }
}
