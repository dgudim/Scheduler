package prototype.xd.scheduler;

import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDay;
import static prototype.xd.scheduler.utilities.DateManager.dateStringFromMsUTC;
import static prototype.xd.scheduler.utilities.DateManager.updateDate;
import static prototype.xd.scheduler.utilities.DialogueUtilities.displayEditTextSpinnerDialogue;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DAY;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL_STR;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;
import java.util.Objects;

import prototype.xd.scheduler.databinding.HomeFragmentBinding;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.SArrayMap;
import prototype.xd.scheduler.utilities.TodoListEntryManager;
import prototype.xd.scheduler.views.CalendarView;

public class HomeFragment extends Fragment {
    
    private volatile TodoListEntryManager todoListEntryManager;
    private HomeFragmentBinding binding;
    
    public HomeFragment() {
        super();
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        todoListEntryManager = new TodoListEntryManager(requireContext(), getLifecycle());
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        
        binding = HomeFragmentBinding.inflate(inflater, container, false);
        
        binding.recyclerView.setItemAnimator(null);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(todoListEntryManager.getTodoListViewAdapter());
        
        // construct custom calendar view
        CalendarView calendarView = new CalendarView(binding.calendar, todoListEntryManager);
        todoListEntryManager.bindCalendarView(calendarView);
        
        // not called on initial startup
        calendarView.setOnDateChangeListener((selectedDate, context) -> {
            updateDate(selectedDate.toEpochDay(), true);
            todoListEntryManager.invalidateArrayAdapter();
            updateStatusText();
        });
        
        // setup month listener, first month is loaded differently
        calendarView.setOnMonthPreChangeListener((prevMonth, firstVisibleDay, lastVisibleDay, context) -> {
                    // load current month entries before displaying the data, skip first init
                    if (prevMonth != null) {
                        todoListEntryManager.loadEntries(firstVisibleDay, lastVisibleDay);
                    }
                }
        );
        
        // when all entries are loaded, update current month
        todoListEntryManager.onInitFinished(() -> requireActivity().runOnUiThread(() -> {
            updateDate(DAY_FLAG_GLOBAL, true);
            updateStatusText();
            // update adapter showing entries
            todoListEntryManager.invalidateArrayAdapter();
            // update calendar updating indicators
            todoListEntryManager.invalidateCalendar();
        }));
        
        binding.toCurrentDateButton.setOnClickListener(v -> calendarView.selectDay(currentDay));
        
        binding.fab.setOnClickListener(view1 -> {
            final List<Group> groupList = todoListEntryManager.getGroups();
            displayEditTextSpinnerDialogue(view1.getContext(), getLifecycle(),
                    R.string.add_event_fab, -1, R.string.event_name_input_hint,
                    R.string.cancel, R.string.add, R.string.add_to_global_list, "", groupList, 0,
                    (view2, text, selectedIndex) -> {
                        SArrayMap<String, String> values = new SArrayMap<>();
                        values.put(TEXT_VALUE, text);
                        values.put(ASSOCIATED_DAY, String.valueOf(currentlySelectedDay));
                        values.put(IS_COMPLETED, "false");
                        
                        todoListEntryManager.addEntry(new TodoListEntry(values, // This is fine here as id because a person can't click 2 times in 1 ms
                                groupList.get(selectedIndex).getRawName(), groupList, System.currentTimeMillis()));
                        return true;
                    },
                    (view2, text, selectedIndex) -> {
                        SArrayMap<String, String> values = new SArrayMap<>();
                        values.put(TEXT_VALUE, text);
                        values.put(ASSOCIATED_DAY, DAY_FLAG_GLOBAL_STR);
                        values.put(IS_COMPLETED, "false");
                        
                        todoListEntryManager.addEntry(new TodoListEntry(values,       // This is fine, see note above
                                groupList.get(selectedIndex).getRawName(), groupList, System.currentTimeMillis()));
                        return true;
                    });
        });
        
        binding.openSettingsButton.setOnClickListener(v ->
                ((NavHostFragment) Objects.requireNonNull(
                        requireActivity()
                                .getSupportFragmentManager()
                                .findFragmentById(R.id.nav_host_fragment)))
                        .getNavController()
                        .navigate(R.id.action_HomeFragment_to_SettingsFragment));
        
        return binding.getRoot();
    }
    
    public void invalidateAll() {
        todoListEntryManager.invalidateAll();
    }
    
    @Override
    public void onDestroyView() {
        // remove reference to ui elements
        todoListEntryManager.unbindCalendarView();
        super.onDestroyView();
    }
    
    private void updateStatusText() {
        binding.statusText.setText(getString(R.string.status, dateStringFromMsUTC(currentlySelectedDay * 86400000),
                todoListEntryManager.getCurrentlyVisibleEntriesCount()));
    }
}