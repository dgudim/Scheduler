package prototype.xd.scheduler;

import static prototype.xd.scheduler.utilities.Keys.setBitmapUpdateFlag;
import static prototype.xd.scheduler.utilities.Utilities.findFragmentInNavHost;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Map;

import prototype.xd.scheduler.databinding.SettingsFragmentBinding;
import prototype.xd.scheduler.utilities.Keys;

public class BaseSettingsFragment <T extends RecyclerView.Adapter<?>> extends DialogFragment {
    
    protected SettingsFragmentBinding binding;
    
    protected T listViewAdapter;
    
    private Map<String, ?> preferenceStateBefore;
    
    // view creation begin
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = SettingsFragmentBinding.inflate(inflater, container, false);
        preferenceStateBefore = Keys.getAll();
        return binding.getRoot();
    }
    
    // dialog dismissed (user pressed back button)
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        setBitmapUpdateFlag();
        if(!preferenceStateBefore.equals(Keys.getAll())) {
            findFragmentInNavHost(requireActivity(), HomeFragment.class).invalidateAll();
        }
        super.onDismiss(dialog);
    }
    
    // view creation end (fragment visible)
    @Override
    public void onViewCreated(@NonNull final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(binding.recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        binding.recyclerView.addItemDecoration(dividerItemDecoration);
        binding.recyclerView.setAdapter(listViewAdapter);
    }
    
}
