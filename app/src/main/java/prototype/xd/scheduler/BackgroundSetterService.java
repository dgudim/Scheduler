package prototype.xd.scheduler;

import static prototype.xd.scheduler.utilities.DateManager.getCurrentTime;
import static prototype.xd.scheduler.utilities.DateManager.getCurrentTimestamp;
import static prototype.xd.scheduler.utilities.DateManager.updateDate;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL_STR;
import static prototype.xd.scheduler.utilities.Keys.PREFERENCES_SERVICE;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.Timer;
import java.util.TimerTask;

import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.LockScreenBitmapDrawer;

public class BackgroundSetterService extends Service {
    
    private SharedPreferences preferences_service;
    private LockScreenBitmapDrawer lockScreenBitmapDrawer;
    
    private Timer refreshTimer;
    
    public static void ping(Context context) {
        ContextCompat.startForegroundService(context, new Intent(context, BackgroundSetterService.class));
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    // Foreground service notification =========
    private final int foregroundNotificationId = (int) (System.currentTimeMillis() % 10000);
    
    // Notification
    private NotificationCompat.Builder foregroundNotification = null;
    
    private NotificationCompat.Builder getForegroundNotification() {
        if (foregroundNotification == null) {
            foregroundNotification = new NotificationCompat.Builder(getApplicationContext(), getForegroundNotificationChannelId())
                    .setSmallIcon(R.drawable.ic_settings)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setSound(null)
                    .setOngoing(true)
                    .setSilent(true)
                    .setShowWhen(false)
                    .setContentText(getString(R.string.background_service_persistent_message));
        }
        return foregroundNotification;
    }
    
    // Notification channel name
    private String foregroundNotificationChannelName = null;
    
    private String getForegroundNotificationChannelName() {
        if (foregroundNotificationChannelName == null) {
            foregroundNotificationChannelName = getString(R.string.service_name);
        }
        return foregroundNotificationChannelName;
    }
    
    
    // Notification channel description
    private String foregroundNotificationChannelDescription = null;
    
    private String getForegroundNotificationChannelDescription() {
        if (foregroundNotificationChannelDescription == null) {
            foregroundNotificationChannelDescription = getString(R.string.service_description);
        }
        return foregroundNotificationChannelDescription;
    }
    
    // Notification channel id
    private String foregroundNotificationChannelId = null;
    private NotificationManager notificationManager;
    
    public String getForegroundNotificationChannelId() {
        if (foregroundNotificationChannelId == null) {
            foregroundNotificationChannelId = "BackgroundSetterService.NotificationChannel";
            
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            // Not exists so we create it at first time
            if (notificationManager.getNotificationChannel(foregroundNotificationChannelId) == null) {
                NotificationChannel nc = new NotificationChannel(
                        getForegroundNotificationChannelId(),
                        getForegroundNotificationChannelName(),
                        NotificationManager.IMPORTANCE_MIN
                );
                // Discrete notification setup
                notificationManager.createNotificationChannel(nc);
                nc.setDescription(getForegroundNotificationChannelDescription());
                nc.setLockscreenVisibility(NotificationCompat.VISIBILITY_PRIVATE);
                nc.setVibrationPattern(null);
                nc.setSound(null, null);
                nc.setShowBadge(false);
            }
            
        }
        return foregroundNotificationChannelId;
    }
    
    private void updateNotification() {
        preferences_service.edit().putLong(Keys.LAST_UPDATE_TIME, getCurrentTimestamp()).apply();
        getForegroundNotification().setContentTitle(getString(R.string.last_update_time, getCurrentTime()));
        notificationManager.notify(foregroundNotificationId, getForegroundNotification().build());
    }
    
    // Lifecycle ===============================
    
    private volatile boolean lastUpdateSucceeded = false;
    private boolean initialized = false;
    private BroadcastReceiver screenOnOffReceiver;
    private BroadcastReceiver pingReceiver;
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || initialized) {
            updateDate(DAY_FLAG_GLOBAL_STR, false);
            lastUpdateSucceeded = lockScreenBitmapDrawer.constructBitmap(BackgroundSetterService.this);
            updateNotification();
        } else {
            initialized = true;
            preferences_service = getSharedPreferences(PREFERENCES_SERVICE, Context.MODE_PRIVATE);
            //register receivers
            screenOnOffReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!lastUpdateSucceeded || preferences_service.getBoolean(SERVICE_UPDATE_SIGNAL, false)) {
                        ping(context);
                        preferences_service.edit().putBoolean(SERVICE_UPDATE_SIGNAL, false).apply();
                    }
                }
            };
            pingReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    ping(context);
                }
            };
            registerReceiver(screenOnOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
            registerReceiver(screenOnOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
            registerReceiver(pingReceiver, new IntentFilter(Intent.ACTION_DATE_CHANGED));
            
            startForeground(foregroundNotificationId, getForegroundNotification().build());
            lockScreenBitmapDrawer = new LockScreenBitmapDrawer(this);
            
            refreshTimer = new Timer();
            refreshTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    preferences_service.edit().putBoolean(SERVICE_UPDATE_SIGNAL, true).apply();
                }
            }, 5000, 1000 * 60 * 10); //approximately every 10 minutes if day
        }
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        lockScreenBitmapDrawer = null;
        foregroundNotification = null;
        notificationManager = null;
        unregisterReceiver(screenOnOffReceiver);
        unregisterReceiver(pingReceiver);
        if (refreshTimer != null) refreshTimer.cancel();
    }
}