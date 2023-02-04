package prototype.xd.scheduler.utilities.receivers;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.services.BackgroundSetterService;

public class PingReceiver extends BroadcastReceiver {
    
    private final String message;
    
    public PingReceiver() {
        message = "default";
    }
    
    public PingReceiver(@NonNull String message) {
        this.message = message;
    }
    
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        Logger.info("BroadcastReceiver", "ping (" + message + ")");
        BackgroundSetterService.ping(context);
    }
}
