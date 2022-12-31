package prototype.xd.scheduler.utilities;

import android.app.Dialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

/**
 * Utility class for dismissing a dialog before activity exit
 */
public class DialogDismissLifecycleObserver implements DefaultLifecycleObserver {
    
    private static final String NAME = "DialogLifecycle";
    
    private @Nullable
    Dialog dialog;
    
    public DialogDismissLifecycleObserver(@NonNull final Dialog dialog) {
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
            Logger.info(NAME, "Activity destroyed, closed lingering dialog " + dialog);
            dialog = null;
        }
    }
}
