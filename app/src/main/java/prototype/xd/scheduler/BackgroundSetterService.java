package prototype.xd.scheduler;

import static prototype.xd.scheduler.utilities.DateManager.isDayTime;
import static prototype.xd.scheduler.utilities.DateManager.updateDate;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL_STR;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.Timer;
import java.util.TimerTask;

import prototype.xd.scheduler.utilities.LockScreenBitmapDrawer;

public class BackgroundSetterService extends Service {
    
    public LockScreenBitmapDrawer lockScreenBitmapDrawer;
    
    private Timer refreshTimer;
    
    public static void restart(Context context) {
        context.stopService(new Intent(context, BackgroundSetterService.class));
        ContextCompat.startForegroundService(context, new Intent(context, BackgroundSetterService.class));
    }
    
    public static void ping(Context context) {
        Intent intent = new Intent(context, BackgroundSetterService.class);
        intent.putExtra("update", 1);
        ContextCompat.startForegroundService(context, intent);
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    // Foreground service notification =========
    private final static int foregroundNotificationId = (int) (System.currentTimeMillis() % 10000);
    
    // Notification
    private static Notification foregroundNotification = null;
    
    public Notification getForegroundNotification() {
        if (foregroundNotification == null) {
            foregroundNotification = new NotificationCompat.Builder(getApplicationContext(), getForegroundNotificationChannelId())
                    .setSmallIcon(R.drawable.ic_settings)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setSound(null)
                    .setOngoing(true)
                    .setContentText("Background service running")
                    .build();
        }
        return foregroundNotification;
    }
    
    // Notification channel name
    private static String foregroundNotificationChannelName = null;
    
    public String getForegroundNotificationChannelName() {
        if (foregroundNotificationChannelName == null) {
            foregroundNotificationChannelName = getString(R.string.service_name);
        }
        return foregroundNotificationChannelName;
    }
    
    
    // Notification channel description
    private static String foregroundNotificationChannelDescription = null;
    
    public String getForegroundNotificationChannelDescription() {
        if (foregroundNotificationChannelDescription == null) {
            foregroundNotificationChannelDescription = getString(R.string.service_description);
        }
        return foregroundNotificationChannelDescription;
    }
    
    // Notification channel id
    private String foregroundNotificationChannelId = null;
    
    public String getForegroundNotificationChannelId() {
        if (foregroundNotificationChannelId == null) {
            foregroundNotificationChannelId = "BackgroundSetterService.NotificationChannel";
            
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            // Not exists so we create it at first time
            if (manager.getNotificationChannel(foregroundNotificationChannelId) == null) {
                NotificationChannel nc = new NotificationChannel(
                        getForegroundNotificationChannelId(),
                        getForegroundNotificationChannelName(),
                        NotificationManager.IMPORTANCE_MIN
                );
                // Discrete notification setup
                manager.createNotificationChannel(nc);
                nc.setDescription(getForegroundNotificationChannelDescription());
                nc.setLockscreenVisibility(NotificationCompat.VISIBILITY_PRIVATE);
                nc.setVibrationPattern(null);
                nc.setSound(null, null);
                nc.setShowBadge(false);
            }
            
        }
        return foregroundNotificationChannelId;
    }
    
    
    // Lifecycle ===============================
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.hasExtra("update")) {
            if (lockScreenBitmapDrawer != null) {
                lockScreenBitmapDrawer.constructBitmap(BackgroundSetterService.this);
            }
        } else {
            startForeground(foregroundNotificationId, getForegroundNotification());
            lockScreenBitmapDrawer = new LockScreenBitmapDrawer(this);
            
            refreshTimer = new Timer();
            refreshTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (isDayTime()) {
                        if (lockScreenBitmapDrawer == null) {
                            lockScreenBitmapDrawer = new LockScreenBitmapDrawer(BackgroundSetterService.this);
                        }
                        updateDate(DAY_FLAG_GLOBAL_STR, false);
                        lockScreenBitmapDrawer.constructBitmap(BackgroundSetterService.this);
                    } else {
                        if (lockScreenBitmapDrawer != null) {
                            lockScreenBitmapDrawer = null;
                            System.gc();
                        }
                    }
                }
            }, 5000, 1000 * 60 * 30); //approximately every 30 minutes if day
        }
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        refreshTimer.cancel();
    }
}