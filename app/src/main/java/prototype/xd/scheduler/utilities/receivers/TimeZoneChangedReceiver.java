package prototype.xd.scheduler.utilities.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.services.BackgroundSetterService;

public class TimeZoneChangedReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
            Logger.info("BroadcastReceiver", "Timezone changed");
            DateManager.updateTimeZone();
            BackgroundSetterService.ping(context);
        }
    }
}
