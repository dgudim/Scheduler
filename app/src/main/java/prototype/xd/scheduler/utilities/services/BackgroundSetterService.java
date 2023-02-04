package prototype.xd.scheduler.utilities.services;

import static prototype.xd.scheduler.utilities.DateManager.getCurrentTimeStringLocal;
import static prototype.xd.scheduler.utilities.Static.SERVICE_KEEP_ALIVE_SIGNAL;
import static prototype.xd.scheduler.utilities.Static.calendarChangedIntentFilter;
import static prototype.xd.scheduler.utilities.Static.clearBitmapUpdateFlag;
import static prototype.xd.scheduler.utilities.Static.getBitmapUpdateFlag;
import static prototype.xd.scheduler.utilities.Static.setBitmapUpdateFlag;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.utilities.BroadcastReceiverHolder;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.receivers.PingReceiver;

public final class BackgroundSetterService extends LifecycleService { // NOSONAR this is a service
    
    public static final String NAME = BackgroundSetterService.class.getSimpleName();
    
    @Nullable
    private LockScreenBitmapDrawer lockScreenBitmapDrawer;
    
    public static void ping(@NonNull Context context) {
        context.startForegroundService(new Intent(context, BackgroundSetterService.class));
    }
    
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static void keepAlive(@NonNull Context context) {
        Intent keepAliveIntent = new Intent(context, BackgroundSetterService.class);
        keepAliveIntent.putExtra(SERVICE_KEEP_ALIVE_SIGNAL, 1);
        context.startForegroundService(keepAliveIntent);
    }
    
    // Foreground service notification =========
    private final int foregroundNotificationId = (int) (System.currentTimeMillis() % 10000);
    
    private NotificationCompat.Builder foregroundNotification;
    private NotificationManager notificationManager;
    
    @NonNull
    private NotificationCompat.Builder getForegroundNotification() {
        if (foregroundNotification == null) {
            
            notificationManager = notificationManager == null ?
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE) : notificationManager;
            
            // create notification channel if it doesn't exist
            String notificationChannelId = "BackgroundSetterService.NotificationChannel";
            if (notificationManager.getNotificationChannel(notificationChannelId) == null) {
                NotificationChannel nc = new NotificationChannel(
                        notificationChannelId,
                        getString(R.string.service_name),
                        NotificationManager.IMPORTANCE_MIN
                );
                // Discrete notification setup
                notificationManager.createNotificationChannel(nc);
                nc.setDescription(getString(R.string.service_description));
                nc.setLockscreenVisibility(NotificationCompat.VISIBILITY_PRIVATE);
                nc.setVibrationPattern(null);
                nc.setSound(null, null);
                nc.setShowBadge(false);
            }
            
            foregroundNotification = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId)
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
    
    private void updateNotification() {
        getForegroundNotification().setContentTitle(getString(R.string.last_update_time, getCurrentTimeStringLocal()));
        notificationManager.notify(foregroundNotificationId, getForegroundNotification().build());
    }
    
    // Lifecycle ===============================
    
    private volatile boolean lastUpdateSucceeded; // NOSONAR
    @NonNull
    private final BroadcastReceiverHolder receiverHolder = new BroadcastReceiverHolder(this);
    
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        
        Static.init(this);
        if (lockScreenBitmapDrawer != null) {
            if (intent != null && intent.hasExtra(SERVICE_KEEP_ALIVE_SIGNAL)) {
                setBitmapUpdateFlag();
                Logger.info(NAME, "Received ping (keep alive job)");
            } else {
                Logger.info(NAME, "Received general ping");
                DateManager.updateDate();
                lastUpdateSucceeded = lockScreenBitmapDrawer.constructBitmap(this);
                updateNotification();
            }
        } else {
            Logger.info(NAME, "Received ping (initial)");
            
            receiverHolder.registerReceiver((context, brIntent) -> {
                if (!lastUpdateSucceeded || getBitmapUpdateFlag()) {
                    ping(context);
                    clearBitmapUpdateFlag();
                    Logger.info(NAME, "Sent ping (screen on/off)");
                }
                Logger.info(NAME, "Receiver state: " + brIntent.getAction());
            }, filter -> {
                filter.addAction(Intent.ACTION_SCREEN_ON);
                filter.addAction(Intent.ACTION_SCREEN_OFF);
                return filter;
            });
            
            receiverHolder.registerReceiver(
                    new PingReceiver("Date changed"),
                    new IntentFilter(Intent.ACTION_DATE_CHANGED));
            
            receiverHolder.registerReceiver(
                    new PingReceiver("Calendar changed"),
                    calendarChangedIntentFilter);
            
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
}
