package prototype.xd.scheduler.utilities.services;

import static prototype.xd.scheduler.utilities.DateManager.getCurrentTime;
import static prototype.xd.scheduler.utilities.DateManager.getCurrentTimestamp;
import static prototype.xd.scheduler.utilities.DateManager.updateDate;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL_STR;
import static prototype.xd.scheduler.utilities.Keys.LAST_KEEPALIVE_TIME;
import static prototype.xd.scheduler.utilities.Keys.PREFERENCES_SERVICE;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_KEEP_ALIVE_SIGNAL;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;
import static prototype.xd.scheduler.utilities.Logger.ContentType.INFO;
import static prototype.xd.scheduler.utilities.Logger.ContentType.WARNING;
import static prototype.xd.scheduler.utilities.Logger.log;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.LockScreenBitmapDrawer;

public class BackgroundSetterService extends Service {
    
    private SharedPreferences preferences_service;
    private LockScreenBitmapDrawer lockScreenBitmapDrawer;
    
    public static void ping(Context context) {
        ContextCompat.startForegroundService(context, new Intent(context, BackgroundSetterService.class));
    }
    
    public static void keepAlive(Context context) {
        Intent keepAliveIntent = new Intent(context, BackgroundSetterService.class);
        keepAliveIntent.putExtra(SERVICE_KEEP_ALIVE_SIGNAL, 1);
        ContextCompat.startForegroundService(context, keepAliveIntent);
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
        if (intent != null && initialized) {
            if (intent.hasExtra(SERVICE_KEEP_ALIVE_SIGNAL)) {
                preferences_service.edit()
                        .putBoolean(SERVICE_UPDATE_SIGNAL, true)
                        .putLong(LAST_KEEPALIVE_TIME, getCurrentTimestamp()).apply();
                log(INFO, "received ping (keep alive job)");
            } else {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    updateDate(DAY_FLAG_GLOBAL_STR, false);
                    lastUpdateSucceeded = lockScreenBitmapDrawer.constructBitmap(BackgroundSetterService.this);
                    log(INFO, "received general ping");
                    if (preferences_service.getLong(LAST_KEEPALIVE_TIME, getCurrentTimestamp()) < 60 * 60 * 1000) {
                        log(WARNING, "keepalive job died, restarting");
                        scheduleRestartJob();
                    }
                    updateNotification();
                }
            }
        } else {
            initialized = true;
            log(INFO, "received ping (initial)");
            
            preferences_service = getSharedPreferences(PREFERENCES_SERVICE, Context.MODE_PRIVATE);
            
            screenOnOffReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!lastUpdateSucceeded || preferences_service.getBoolean(SERVICE_UPDATE_SIGNAL, false)) {
                        ping(context);
                        preferences_service.edit().putBoolean(SERVICE_UPDATE_SIGNAL, false).apply();
                        log(INFO, "sent ping (on - off receiver)");
                    }
                    log(INFO, "receiver state: " + intent.getAction());
                }
            };
            pingReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    ping(context);
                    log(INFO, "sent ping (date changed receiver)");
                }
            };
            
            IntentFilter onOffFilter = new IntentFilter();
            onOffFilter.addAction(Intent.ACTION_SCREEN_ON);
            onOffFilter.addAction(Intent.ACTION_SCREEN_OFF);
            
            registerReceiver(screenOnOffReceiver, onOffFilter);
            registerReceiver(pingReceiver, new IntentFilter(Intent.ACTION_DATE_CHANGED));
            scheduleRestartJob();
            startForeground(foregroundNotificationId, getForegroundNotification().build());
            lockScreenBitmapDrawer = new LockScreenBitmapDrawer(this);
        }
        return START_STICKY;
    }
    
    private void scheduleRestartJob() {
        log(INFO, "restart job scheduled");
        getSystemService(JobScheduler.class).schedule(new JobInfo.Builder(0,
                new ComponentName(getApplicationContext(), KeepAliveService.class))
                .setPeriodic(15 * 60 * 1000, 5 * 60 * 1000).build());
    }
    
    @Override
    public void onDestroy() {
        log(INFO, "service destroyed");
        scheduleRestartJob();
        if (screenOnOffReceiver != null) {
            unregisterReceiver(screenOnOffReceiver);
        }
        if (pingReceiver != null) {
            unregisterReceiver(pingReceiver);
        }
        lockScreenBitmapDrawer = null;
    }
}