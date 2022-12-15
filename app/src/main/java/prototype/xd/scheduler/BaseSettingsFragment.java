package prototype.xd.scheduler;

import static prototype.xd.scheduler.utilities.Keys.SERVICE_UPDATE_SIGNAL;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;
import static prototype.xd.scheduler.utilities.PreferencesStore.servicePreferences;
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

import java.util.Map;

import prototype.xd.scheduler.adapters.SettingsListViewAdapter;
import prototype.xd.scheduler.databinding.SettingsFragmentBinding;

public class BaseSettingsFragment extends DialogFragment {
    
    protected SettingsFragmentBinding binding;
    
    protected SettingsListViewAdapter settingsListViewAdapter;
    
    private Map<String, ?> preferenceStateBefore;
    
    // view creation begin
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = SettingsFragmentBinding.inflate(inflater, container, false);
        preferenceStateBefore = preferences.getAll();
        return binding.getRoot();
    }
    
    // dialog dismissed (user pressed back button)
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        servicePreferences.edit()
                .putBoolean(SERVICE_UPDATE_SIGNAL, true)
                .apply();
        if(!preferenceStateBefore.equals(preferences.getAll())) {
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
        binding.recyclerView.setAdapter(settingsListViewAdapter);
    }
    
}
