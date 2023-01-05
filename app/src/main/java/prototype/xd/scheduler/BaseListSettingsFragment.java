package prototype.xd.scheduler;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import prototype.xd.scheduler.databinding.SettingsFragmentBinding;

// base dialog class with a list view
public class BaseListSettingsFragment<T extends RecyclerView.Adapter<?>> extends BaseSettingsFragment<SettingsFragmentBinding> {
    
    protected T listViewAdapter;
    
    @Override
    public SettingsFragmentBinding inflate(@NonNull LayoutInflater inflater, ViewGroup container) {
        return SettingsFragmentBinding.inflate(inflater, container, false);
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
