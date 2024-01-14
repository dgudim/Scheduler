package prototype.xd.scheduler.fragments.dialogs;

import android.app.Dialog;

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public abstract class AlertSettingsDialogFragment<T extends ViewBinding> extends BaseDialogFragment<T> {
    
    @NonNull
    @Override
    protected Dialog buildDialogFrame() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper.context);
        buildDialogFrame(builder);
        return builder.create();
    }
    
    protected abstract void buildDialogFrame(@NonNull MaterialAlertDialogBuilder builder);
}
