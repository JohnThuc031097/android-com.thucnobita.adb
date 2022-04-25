package com.thucnobita.adb;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.thucnobita.adb.views.MainActivity;

public class Service extends android.app.Service {
    public static final String ACTION_START = "com.thucnobita.adb.ACTION_START";
    public static final String ACTION_STOP = "com.thucnobita.adb.ACTION_STOP";
    public static final String ACTION_TEST = "com.thucnobita.adb.ACTION_TEST";

    private static final String TAG = "Service";
    private static final int NOTIFICATION_ID = 0x1;

    private NotificationCompat.Builder builder;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private String createNotificationChannel(String channelId, String channelName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            service.createNotificationChannel(chan);
        }
        return channelId;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        String channelId = createNotificationChannel("monitor service", "Monitor Service");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(ACTION_STOP);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
                NOTIFICATION_ID,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        builder = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Local ADB")
                .setContentText("Touch to stop!")
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis());
        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);
    }

    public void setNotificationContentTitle(String text) {
        builder.setContentTitle(text);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.i(TAG, "OnStartCommand");
        String action = intent.getAction();

        if (ACTION_START.equals(action)) {
            Log.i(TAG, "ACTION_START");
            setNotificationContentTitle("Local ADB running");
            startActivity(new Intent(this, MainActivity.class));
        } else if (ACTION_STOP.equals(action)) {
            Log.i(TAG, "ACTION_STOP");
            stopSelf();
        }else if (ACTION_TEST.equals(action)){
            setNotificationContentTitle("Local ADB testing");
            Log.i(TAG, "ACTION_TEST");
        }
        return START_NOT_STICKY; // not start again, when killed by system
    }

    @Override
    public void onLowMemory() {
        Log.w(TAG, "Low memory");
    }
}
