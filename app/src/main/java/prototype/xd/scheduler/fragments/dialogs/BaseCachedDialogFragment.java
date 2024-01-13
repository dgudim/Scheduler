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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public abstract class BaseCachedDialogFragment<T extends ViewBinding, D extends Dialog> extends DialogFragment {
    
    public static final String NAME = BaseCachedDialogFragment.class.getSimpleName();
    
    @SuppressLint("UnknownNullness")
    protected ContextWrapper wrapper;
    @Nullable
    private T binding;
    @SuppressLint("UnknownNullness")
    private D dialog;
    
    @NonNull
    protected abstract T inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container);
    
    @NonNull
    protected abstract D buildDialog();
    
    @Override
    public void show(@NonNull FragmentManager manager, @Nullable String tag) {
        // TODO: handle double invocation
        Logger.debug(NAME, "Showing dialog fragment: " + tag + " using fragment manager: " + manager + " with " + manager.getFragments().size() + " fragments");
        super.show(manager, tag);
    }
    
    @NonNull
    public AlertDialog getAlertDialog() {
        return new MaterialAlertDialogBuilder(wrapper.context).create();
    }
    
    @NonNull
    protected ComponentDialog getBaseDialog() {
        return new ComponentDialog(requireContext(), getTheme());
    }
    
    @NonNull
    protected T requireBinding() {
        return Objects.requireNonNull(binding);
    }
    
    protected abstract void buildDialogStatic(@NonNull T binding, @NonNull D dialog);
    
    protected abstract void buildDialogDynamic(@NonNull T binding, @NonNull D dialog);
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (dialog == null) {
            dialog = buildDialog();
        }
        return dialog;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        boolean initialStart = binding == null;
        if (initialStart) {
            long time = System.currentTimeMillis();
            binding = inflate(inflater, container);
            buildDialogStatic(binding, dialog);
            Logger.infoWithTime(NAME, "Build dialog static + inflation {time}", time);
        }
        long time = System.currentTimeMillis();
        buildDialogDynamic(binding, dialog);
        Logger.infoWithTime(NAME, "Build dialog dynamic {time}", time);
        if (initialStart) {
            if (dialog instanceof AlertDialog) {
                ((AlertDialog) dialog).setView(binding.getRoot());
            } else {
                dialog.setContentView(binding.getRoot());
            }
        }
        return null;
    }
    
    
    // fragment creation begin
    @Override
    @MainThread
    @CallSuper
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wrapper = ContextWrapper.from(this);
    }
    
}
