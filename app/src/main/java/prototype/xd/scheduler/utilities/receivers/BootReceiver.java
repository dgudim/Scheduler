package prototype.xd.scheduler.utilities.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import prototype.xd.scheduler.utilities.services.BackgroundSetterService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            BackgroundSetterService.ping(context);
        }
    }
}
