package prototype.xd.scheduler;

import static prototype.xd.scheduler.utilities.Utilities.findFragmentInNavHost;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.viewbinding.ViewBinding;

import java.util.Map;

import prototype.xd.scheduler.utilities.ContextWrapper;
import prototype.xd.scheduler.utilities.Keys;

// base dialog class that refreshes main screen on settings changes
public abstract class BaseSettingsFragment<T extends ViewBinding> extends DialogFragment {
    
    protected T binding;
    
    @SuppressLint("UnknownNullness")
    protected ContextWrapper wrapper;
    
    private Map<String, ?> preferenceStateBefore;
    
    public abstract T inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container);
    
    // view creation begin
    @Override
    @MainThread
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = inflate(inflater, container);
        preferenceStateBefore = Keys.getAll();
        return binding.getRoot();
    }
    
    // fragment creation begin
    @Override
    @MainThread
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wrapper = ContextWrapper.from(this);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialog);
    }
    
    // dialog dismissed (user pressed back button)
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        if (!preferenceStateBefore.equals(Keys.getAll())) {
            findFragmentInNavHost(requireActivity(), HomeFragment.class).notifySettingsChanged();
        }
        super.onDismiss(dialog);
    }
}
