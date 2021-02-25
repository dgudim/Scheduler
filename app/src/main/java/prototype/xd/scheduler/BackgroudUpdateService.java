package prototype.xd.scheduler;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;

import static prototype.xd.scheduler.utilities.DateManager.currentDate;
import static prototype.xd.scheduler.utilities.DateManager.updateDate;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.constructBitmap;

public class BackgroudUpdateService extends Service {

    private static final String NOTIF_ID = "1";
    private static final String NOTIF_CHANNEL_ID = "Background update service";
    private SharedPreferences preferences;


    /**
     * Return the communication channel to the service.  May return null if
     * clients can not bind to the service.  The returned
     * {@link IBinder} is usually for a complex interface
     * that has been <a href="{@docRoot}guide/components/aidl.html">described using
     * aidl</a>.
     *
     * <p><em>Note that unlike other application components, calls on to the
     * IBinder interface returned here may not happen on the main thread
     * of the process</em>.  More information about the main thread can be found in
     * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html">Processes and
     * Threads</a>.</p>
     *
     * @param intent The Intent that was used to bind to this service,
     *               as given to {@link Context#bindService
     *               Context.bindService}.  Note that any extras that were included with
     *               the Intent at that point will <em>not</em> be seen here.
     * @return Return an IBinder through which clients can call on to the
     * service.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        preferences = getBaseContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkUpdate();
            }
        }, 0, 11 * 1000);
        //11 second delay

        startForeground();

        return super.onStartCommand(intent, flags, startId);
    }

    private void startForeground() {

        Context context = getApplicationContext();

        Intent notificationIntent = new Intent(context, prototype.xd.scheduler.MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        startForeground(1, new NotificationCompat.Builder(context, createNotificationChannel(NOTIF_ID, NOTIF_CHANNEL_ID)).setOngoing(true)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Service is running in background")
                .setContentIntent(pendingIntent)
                .build());

    }

    void checkUpdate() {
        updateDate("none", false);
        String lastDate = preferences.getString("date", "");

        if (!lastDate.equals(currentDate)) {
            constructBitmap();
            preferences.edit().putString("date", currentDate).apply();
        }

    }

    private String createNotificationChannel(String channelId, String channelName) {
        NotificationChannel chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_MIN);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return channelId;
    }
}
