package prototype.xd.scheduler.utilities.misc;

import static prototype.xd.scheduler.utilities.Logger.logException;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.Uri;
import android.view.LayoutInflater;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.IntegerRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;

import java.io.IOException;
import java.io.InputStream;

import prototype.xd.scheduler.utilities.Logger;

public final class ContextWrapper {
    
    public static final String NAME = ContextWrapper.class.getSimpleName();
    
    private LayoutInflater layoutInflater;
    
    @NonNull
    public final Context context;
    @NonNull
    public final Lifecycle lifecycle;
    @NonNull
    public final FragmentManager fragmentManager;
    
    @NonNull
    public final FragmentActivity activity;
    
    private ContextWrapper(@NonNull final Context context,
                           @NonNull final Lifecycle lifecycle,
                           @NonNull final FragmentManager fragmentManager,
                           @NonNull final FragmentActivity activity) {
        this.context = context;
        this.lifecycle = lifecycle;
        this.fragmentManager = fragmentManager;
        this.activity = activity;
    }
    
    @NonNull
    public static ContextWrapper from(@NonNull Fragment fragment) {
        return new ContextWrapper(
                fragment.requireContext(),
                fragment.getLifecycle(),
                fragment.getChildFragmentManager(),
                fragment.requireActivity());
    }
    
    @NonNull
    public LayoutInflater getLayoutInflater() {
        if (layoutInflater == null) {
            layoutInflater = LayoutInflater.from(context);
        }
        return layoutInflater;
    }
    
    // ------------------------ METHODS FOR LIFECYCLE PART
    
    /**
     * Adds a LifecycleObserver that will be notified when the LifecycleOwner changes
     * state.
     * <p>
     * The given observer will be brought to the current state of the LifecycleOwner.
     * For example, if the LifecycleOwner is in {@link Lifecycle.State#STARTED} state, the given observer
     * will receive {@link Lifecycle.Event#ON_CREATE}, {@link Lifecycle.Event#ON_START} events.
     *
     * @param observer The observer to notify.
     */
    @MainThread
    public void addLifecycleObserver(@NonNull LifecycleObserver observer) {
        lifecycle.addObserver(observer);
    }
    
    @NonNull
    public Resources getResources() {
        return context.getResources();
    }
    
    @NonNull
    @MainThread
    public <T extends Dialog> T attachDialogToLifecycle(@NonNull final T dialog,
                                                        @Nullable DialogInterface.OnDismissListener dismissListener) {
        // make sure the dialog is dismissed on activity destroy
        DialogDismissObserver dismissLifecycleObserver = new DialogDismissObserver(dialog);
        lifecycle.addObserver(dismissLifecycleObserver);
        // remove the observer as soon as the dialog in dismissed
        dialog.setOnDismissListener(dialog1 -> {
            if (dismissListener != null) {
                dismissListener.onDismiss(dialog1);
            }
            lifecycle.removeObserver(dismissLifecycleObserver);
        });
        return dialog;
    }
    
    // ------------------------ METHODS FOR CONTEXT PART
    
    /**
     * Returns a localized string from the application's package's
     * default string table.
     *
     * @param resId Resource id for the string
     * @return The string data associated with the resource, stripped of styled
     * text information.
     */
    @NonNull
    public String getString(@StringRes int resId) {
        return context.getResources().getString(resId);
    }
    
    /**
     * Returns a localized formatted string from the application's package's
     * default string table, substituting the format arguments as defined in
     * {@link java.util.Formatter} and {@link java.lang.String#format}.
     *
     * @param resId      Resource id for the format string
     * @param formatArgs The format arguments that will be used for
     *                   substitution.
     * @return The string data associated with the resource, formatted and
     * stripped of styled text information.
     * @throws android.content.res.Resources.NotFoundException if the given ID
     *                                                         does not exist.
     */
    @NonNull
    public String getString(@StringRes int resId, @NonNull Object... formatArgs) throws Resources.NotFoundException {
        return context.getResources().getString(resId, formatArgs);
    }
    
    /**
     * Return an integer associated with a particular resource ID.
     *
     * @param resId The desired resource identifier, as generated by the aapt
     *              tool. This integer encodes the package, type, and resource
     *              entry. The value 0 is an invalid identifier.
     * @return Returns the integer value contained in the resource.
     * @throws Resources.NotFoundException Throws NotFoundException if the given ID does not exist.
     */
    public int getInteger(@IntegerRes int resId) throws Resources.NotFoundException {
        return context.getResources().getInteger(resId);
    }
    
    /**
     * Returns a color associated with a particular resource ID and styled for
     * the current theme.
     *
     * @param id The desired resource identifier, as generated by the aapt
     *           tool. This integer encodes the package, type, and resource
     *           entry. The value 0 is an invalid identifier.
     * @return A single color value in the form 0xAARRGGBB.
     * @throws android.content.res.Resources.NotFoundException if the given ID
     *                                                         does not exist.
     */
    @ColorInt
    public int getColor(@ColorRes int id) {
        return context.getResources().getColor(id, context.getTheme());
    }
    
    /**
     * Runs the specified action on the UI thread. If the current thread is the UI
     * thread, then the action is executed immediately. If the current thread is
     * not the UI thread, the action is posted to the event queue of the UI thread.
     *
     * @param action the action to run on the UI thread
     */
    public void runOnUiThread(@NonNull Runnable action) {
        activity.runOnUiThread(action);
    }
    
    @FunctionalInterface
    public interface IOConsumer<T> {
        void accept(T t) throws IOException;
    }
    
    public void processUri(@Nullable Uri uri, @NonNull IOConsumer<InputStream> consumer, @Nullable Runnable nullUriHandler) {
        if (uri == null) {
            if(nullUriHandler != null) {
                nullUriHandler.run();
            }
            Logger.error(NAME, "Uri is null");
            return;
        }
        try (InputStream stream = activity.getContentResolver().openInputStream(uri)) {
            consumer.accept(stream);
        } catch (IOException e) {
            logException(NAME, e);
        }
    }
}
