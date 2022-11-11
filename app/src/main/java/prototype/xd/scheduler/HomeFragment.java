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
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Objects;

import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.TodoListEntryManager;
import prototype.xd.scheduler.views.CalendarView;

public class HomeFragment extends Fragment {
    
    private volatile TodoListEntryManager todoListEntryManager;
    
    public HomeFragment() {
        super();
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        todoListEntryManager = new TodoListEntryManager(requireContext(), getLifecycle());
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        
        View view = inflater.inflate(R.layout.home_fragment, container, false);
        ((ListView) view.findViewById(R.id.list)).setAdapter(todoListEntryManager.getTodoListViewAdapter());
        
        // construct custom calendar view
        CalendarView calendarView = new CalendarView(view.findViewById(R.id.calendar), todoListEntryManager);
        todoListEntryManager.setCurrentDayIndicatorChangeListener(calendarView::notifyCurrentDayChanged);
        
        TextView statusText = view.findViewById(R.id.status_text);
        // not called on initial startup
        calendarView.setOnDateChangeListener((selectedDate, context) -> {
            updateDate(selectedDate.toEpochDay(), true);
            todoListEntryManager.updateTodoListAdapter(false, false);
            updateStatusText(statusText);
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
        
        view.findViewById(R.id.to_current_date_button).setOnClickListener(v -> calendarView.selectDay(currentDay));
        
        view.<FloatingActionButton>findViewById(R.id.fab).setOnClickListener(view1 -> {
            final List<Group> groupList = todoListEntryManager.getGroups();
            displayEditTextSpinnerDialogue(view1.getContext(), R.string.add_event_fab, -1, R.string.event_name_input_hint,
                    R.string.cancel, R.string.add, R.string.add_to_global_list, "", groupList, 0,
                    (view2, text, selectedIndex) -> {
                        TodoListEntry newEntry = new TodoListEntry(view2.getContext(), new String[]{
                                TEXT_VALUE, text,
                                ASSOCIATED_DAY, String.valueOf(currentlySelectedDay),
                                IS_COMPLETED, "false"}, groupList.get(selectedIndex).getName(), groupList);
                        todoListEntryManager.addEntry(newEntry);
                        todoListEntryManager.saveEntriesAsync();
                        todoListEntryManager.updateTodoListAdapter(newEntry.isVisibleOnLockscreen(), true);
                        return true;
                    },
                    (view2, text, selectedIndex) -> {
                        TodoListEntry newEntry = new TodoListEntry(view2.getContext(), new String[]{
                                TEXT_VALUE, text,
                                ASSOCIATED_DAY, DAY_FLAG_GLOBAL_STR,
                                IS_COMPLETED, "false"}, groupList.get(selectedIndex).getName(), groupList);
                        todoListEntryManager.addEntry(newEntry);
                        todoListEntryManager.saveEntriesAsync();
                        todoListEntryManager.updateTodoListAdapter(newEntry.isVisibleOnLockscreen(), false);
                        return true;
                    });
        });
        
        view.findViewById(R.id.openSettingsButton).setOnClickListener(v ->
                ((NavHostFragment) Objects.requireNonNull(
                        requireActivity()
                                .getSupportFragmentManager()
                                .findFragmentById(R.id.nav_host_fragment)))
                        .getNavController()
                        .navigate(R.id.action_HomeFragment_to_SettingsFragment));
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        updateDate(DAY_FLAG_GLOBAL, true);
        
        updateStatusText(view.findViewById(R.id.status_text));
    }
    
    private void updateStatusText(TextView statusText) {
        statusText.setText(getString(R.string.status, dateFromEpoch(currentlySelectedDay * 86400000),
                todoListEntryManager.getCurrentlyVisibleEntriesCount()));
    }
}