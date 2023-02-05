package prototype.xd.scheduler.utilities.misc;

import android.app.Dialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import prototype.xd.scheduler.utilities.Logger;

/**
 * Utility class for dismissing a dialog before activity exit to avoid android.view.WindowLeaked
 */
public class DialogDismissObserver implements DefaultLifecycleObserver {
    
    public static final String NAME = DialogDismissObserver.class.getSimpleName();
    
    @Nullable
    private Dialog dialog;
    
    public DialogDismissObserver(@NonNull final Dialog dialog) {
        this.dialog = dialog;
    }
    
    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        if (dialog == null) {
            Logger.warning(NAME, "Something is not right, onDestroy was called but observer already fired");
        }
        if (dialog != null && dialog.isShowing()) {
            // dismiss the dialog to avoid android.view.WindowLeaked
            dialog.dismiss();
            Logger.info(NAME, "Activity destroyed, closed lingering dialog " + dialog.hashCode());
            dialog = null;
        }
    }
}
