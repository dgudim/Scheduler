package prototype.xd.scheduler;

import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDay;
import static prototype.xd.scheduler.utilities.DateManager.dateFromEpoch;
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
import prototype.xd.scheduler.utilities.SSMap;
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
        todoListEntryManager.bindToCalendarView(calendarView);
        
        // not called on initial startup
        calendarView.setOnDateChangeListener((selectedDate, context) -> {
            updateDate(selectedDate.toEpochDay(), true);
            todoListEntryManager.updateTodoListAdapter(false, false);
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
            // update adapter showing entries
            todoListEntryManager.updateTodoListAdapter(false, false);
            // rebind all views updating indicators
            calendarView.notifyCurrentMonthChanged();
        }));
        
        binding.toCurrentDateButton.setOnClickListener(v -> calendarView.selectDay(currentDay));
        
        binding.fab.setOnClickListener(view1 -> {
            final List<Group> groupList = todoListEntryManager.getGroups();
            displayEditTextSpinnerDialogue(view1.getContext(), R.string.add_event_fab, -1, R.string.event_name_input_hint,
                    R.string.cancel, R.string.add, R.string.add_to_global_list, "", groupList, 0,
                    (view2, text, selectedIndex) -> {
                        SSMap values = new SSMap();
                        values.put(TEXT_VALUE, text);
                        values.put(ASSOCIATED_DAY, String.valueOf(currentlySelectedDay));
                        values.put(IS_COMPLETED, "false");
                        TodoListEntry newEntry = new TodoListEntry(view2.getContext(), values,
                                groupList.get(selectedIndex).getName(), groupList, System.currentTimeMillis());
                        // This is fine here as id because a person can't click 2 times in 1 ms
                        todoListEntryManager.addEntry(newEntry);
                        todoListEntryManager.saveEntriesAsync();
                        todoListEntryManager.updateTodoListAdapter(newEntry.isVisibleOnLockscreen(), true);
                        return true;
                    },
                    (view2, text, selectedIndex) -> {
                        SSMap values = new SSMap();
                        values.put(TEXT_VALUE, text);
                        values.put(ASSOCIATED_DAY, DAY_FLAG_GLOBAL_STR);
                        values.put(IS_COMPLETED, "false");
                        TodoListEntry newEntry = new TodoListEntry(view2.getContext(), values,
                                groupList.get(selectedIndex).getName(), groupList, System.currentTimeMillis());
                        // This is fine, see note above
                        todoListEntryManager.addEntry(newEntry);
                        todoListEntryManager.saveEntriesAsync();
                        todoListEntryManager.updateTodoListAdapter(newEntry.isVisibleOnLockscreen(), false);
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
    
    @Override
    public void onDestroyView() {
        // remove reference to ui elements
        todoListEntryManager.unbindCalendarView();
        super.onDestroyView();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateDate(DAY_FLAG_GLOBAL, true);
        updateStatusText();
    }
    
    private void updateStatusText() {
        binding.statusText.setText(getString(R.string.status, dateFromEpoch(currentlySelectedDay * 86400000),
                todoListEntryManager.getCurrentlyVisibleEntriesCount()));
    }
}