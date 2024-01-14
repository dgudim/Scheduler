package prototype.xd.scheduler.fragments.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.ComponentDialog;
import androidx.annotation.CallSuper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewbinding.ViewBinding;

import java.util.Objects;

import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

// TODO: maybe cache fragment in livedata and clear on config change?
public abstract class BaseDialogFragment<T extends ViewBinding> extends DialogFragment {
    
    public static final String NAME = BaseDialogFragment.class.getSimpleName();
    
    @SuppressLint("UnknownNullness")
    protected ContextWrapper wrapper;
    @Nullable
    private T binding;
    @SuppressLint("UnknownNullness")
    private Dialog dialog;
    
    protected abstract void setVariablesFromData();
    
    @NonNull
    protected abstract T inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container);
    
    @NonNull
    protected Dialog buildDialogFrame() {
        return new ComponentDialog(wrapper.context, getTheme());
    }
    
    @Override
    public void show(@NonNull FragmentManager manager, @Nullable String tag) {
        var fragment = manager.findFragmentByTag(tag);
        if (fragment != null) {
            Logger.warning(NAME, "Not showing duplicate dialog fragment: " + tag + " using fragment manager: " + manager);
            return;
        }
        Logger.info(NAME, "Showing dialog fragment: " + tag + " using fragment manager: " + manager);
        super.show(manager, tag);
    }
    
    @NonNull
    protected T requireBinding() {
        return Objects.requireNonNull(binding);
    }
    
    protected abstract void buildDialogBody(@NonNull T binding);
    
    // fragment creation begin
    @Override
    @MainThread
    @CallSuper
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wrapper = new ContextWrapper(this);
        setVariablesFromData();
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        dialog = buildDialogFrame();
        return dialog;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        long time = System.currentTimeMillis();
        binding = inflate(inflater, container);
        buildDialogBody(binding);
        if (dialog instanceof AlertDialog alertDialog) {
            alertDialog.setView(binding.getRoot());
        } else {
            dialog.setContentView(binding.getRoot());
        }
        Logger.infoWithTime(NAME, "Build dialog body {time}", time);
        return null;
    }
}
