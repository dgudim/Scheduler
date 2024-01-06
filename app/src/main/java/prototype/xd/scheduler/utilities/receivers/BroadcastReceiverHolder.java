package prototype.xd.scheduler.utilities.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

import prototype.xd.scheduler.utilities.Logger;

public class BroadcastReceiverHolder implements DefaultLifecycleObserver {
    
    public static final String NAME = BroadcastReceiverHolder.class.getSimpleName();
    
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    private final List<BroadcastReceiver> broadcastReceivers = new ArrayList<>();
    private final Context context;
    
    public <T extends Context & LifecycleOwner> BroadcastReceiverHolder(@NonNull T contextLifecycleOwner) {
        context = contextLifecycleOwner;
        contextLifecycleOwner.getLifecycle().addObserver(this); // NOSONAR, this is ok
    }
    
    public void registerReceiver(@NonNull BiConsumer<Context, Intent> callback, @NonNull UnaryOperator<IntentFilter> builder) {
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                callback.accept(context, intent);
            }
        }, builder.apply(new IntentFilter()));
    }
    
    public void registerReceiver(@Nullable BroadcastReceiver receiver, @NonNull IntentFilter filter) {
        broadcastReceivers.add(receiver);
        context.registerReceiver(receiver, filter);
        Logger.debug(NAME, "Receiver registered: " + receiver + " from " + context);
    }
    
    public void registerReceiver(@Nullable BroadcastReceiver receiver, @NonNull UnaryOperator<IntentFilter> builder) {
        registerReceiver(receiver, builder.apply(new IntentFilter()));
    }
    
    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        for (BroadcastReceiver receiver : broadcastReceivers) {
            if (receiver != null) {
                context.unregisterReceiver(receiver);
                Logger.debug(NAME, "Receiver unregistered: " + receiver + " from " + context);
            }
        }
    }
}
