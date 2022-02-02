package prototype.xd.scheduler.utilities.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import prototype.xd.scheduler.BackgroundSetterService;

public class DateChangedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_DATE_CHANGED.equals(intent.getAction())) {
            BackgroundSetterService.ping(context);
        }
    }
}
