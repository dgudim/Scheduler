package prototype.xd.scheduler.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import prototype.xd.scheduler.utilities.misc.ContextWrapper;

public abstract class BaseFragment<T extends ViewBinding> extends Fragment {
    
    protected T binding;
    
    @SuppressLint("UnknownNullness")
    protected ContextWrapper wrapper;
    
    @NonNull
    public abstract T inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container);
    
    // fragment creation begin
    @Override
    @MainThread
    @CallSuper
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wrapper = ContextWrapper.from(this);
    }
    
    // fragment view created
    @Override
    @MainThread
    @Nullable
    @CallSuper
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = inflate(inflater, container);
        return binding.getRoot();
    }
    
}
