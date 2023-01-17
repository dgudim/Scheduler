package prototype.xd.scheduler.utilities.services;

import static prototype.xd.scheduler.utilities.DateManager.checkIfTimeSettingsChanged;
import static prototype.xd.scheduler.utilities.DateManager.getCurrentTimeStringLocal;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_KEEP_ALIVE_SIGNAL;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;
import static prototype.xd.scheduler.utilities.Keys.clearBitmapUpdateFlag;
import static prototype.xd.scheduler.utilities.Keys.setBitmapUpdateFlag;

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
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.Logger;

public final class BackgroundSetterService extends Service { // NOSONAR this is a service
    
    public static final String NAME = BackgroundSetterService.class.getSimpleName();
    
    @Nullable
    private LockScreenBitmapDrawer lockScreenBitmapDrawer;
    
    public static void ping(@NonNull Context context) {
        ContextCompat.startForegroundService(context, new Intent(context, BackgroundSetterService.class));
    }
    
    public static void keepAlive(@NonNull Context context) {
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
    private NotificationCompat.Builder foregroundNotification;
    
    @NonNull
    private NotificationCompat.Builder getForegroundNotification() {
        if (foregroundNotification == null) {
            foregroundNotification = new NotificationCompat.Builder(getApplicationContext(), getNotificationChannelId())
                    .setSmallIcon(R.drawable.ic_settings_45)
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
    private String notificationChannelName;
    
    private String getNotificationChannelName() {
        if (notificationChannelName == null) {
            notificationChannelName = getString(R.string.service_name);
        }
        return notificationChannelName;
    }
    
    
    // Notification channel description
    private String notificationChannelDescription;
    
    private String getNotificationChannelDescription() {
        if (notificationChannelDescription == null) {
            notificationChannelDescription = getString(R.string.service_description);
        }
        return notificationChannelDescription;
    }
    
    // Notification channel id
    private String notificationChannelId;
    private NotificationManager notificationManager;
    
    public String getNotificationChannelId() {
        if (notificationChannelId == null) {
            notificationChannelId = "BackgroundSetterService.NotificationChannel";
            
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager.getNotificationChannel(notificationChannelId) == null) {
                NotificationChannel nc = new NotificationChannel(
                        getNotificationChannelId(),
                        getNotificationChannelName(),
                        NotificationManager.IMPORTANCE_MIN
                );
                // Discrete notification setup
                notificationManager.createNotificationChannel(nc);
                nc.setDescription(getNotificationChannelDescription());
                nc.setLockscreenVisibility(NotificationCompat.VISIBILITY_PRIVATE);
                nc.setVibrationPattern(null);
                nc.setSound(null, null);
                nc.setShowBadge(false);
            }
            
        }
        return notificationChannelId;
    }
    
    private void updateNotification() {
        getForegroundNotification().setContentTitle(getString(R.string.last_update_time, getCurrentTimeStringLocal()));
        notificationManager.notify(foregroundNotificationId, getForegroundNotification().build());
    }
    
    // Lifecycle ===============================
    
    private volatile boolean lastUpdateSucceeded;
    private boolean initialized;
    @Nullable
    private BroadcastReceiver screenOnOffReceiver;
    @Nullable
    private BroadcastReceiver pingReceiver;
    
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        Keys.initPrefs(this);
        if (intent != null && initialized) {
            if (intent.hasExtra(SERVICE_KEEP_ALIVE_SIGNAL)) {
                setBitmapUpdateFlag();
                Logger.info(NAME, "Received ping (keep alive job)");
            } else {
                Logger.info(NAME, "Received general ping");
                if (lockScreenBitmapDrawer != null) {
                    lastUpdateSucceeded = lockScreenBitmapDrawer.constructBitmap(this, checkIfTimeSettingsChanged());
                    updateNotification();
                } else {
                    Logger.error(NAME, "lockScreenBitmapDrawer is null, huh?");
                }
            }
        } else {
            initialized = true;
            Logger.info(NAME, "Received ping (initial)");
            
            screenOnOffReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(@NonNull Context context, @NonNull Intent intent) {
                    if (!lastUpdateSucceeded || SERVICE_UPDATE_SIGNAL.get()) {
                        ping(context);
                        clearBitmapUpdateFlag();
                        Logger.info(NAME, "Sent ping (on - off receiver)");
                    }
                    Logger.info(NAME, "Receiver state: " + intent.getAction());
                }
            };
            pingReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(@NonNull Context context, Intent intent) {
                    ping(context);
                    Logger.info(NAME, "Sent ping (date changed receiver)");
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
        Logger.info(NAME, "restart job scheduled");
        getSystemService(JobScheduler.class).schedule(new JobInfo.Builder(0,
                new ComponentName(getApplicationContext(), KeepAliveService.class))
                .setPeriodic(15 * 60L * 1000, 5 * 60L * 1000).build());
    }
    
    @Override
    public void onDestroy() {
        if (screenOnOffReceiver != null) {
            unregisterReceiver(screenOnOffReceiver);
        }
        if (pingReceiver != null) {
            unregisterReceiver(pingReceiver);
        }
        // unregister receivers
        lockScreenBitmapDrawer = null;
    }
}
