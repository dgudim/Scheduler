package prototype.xd.scheduler.utilities.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import prototype.xd.scheduler.utilities.services.BackgroundSetterService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            BackgroundSetterService.ping(context);
        }
    }
}
