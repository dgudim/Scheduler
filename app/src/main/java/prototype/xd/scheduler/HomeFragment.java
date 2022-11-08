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
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Objects;

import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.TodoListEntryStorage;
import prototype.xd.scheduler.views.CalendarView;

public class HomeFragment extends Fragment {
    
    private volatile TodoListEntryStorage todoListEntryStorage;
    
    public HomeFragment() {
        super();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        
        View view = inflater.inflate(R.layout.home_fragment, container, false);
        
        ListView listView = view.findViewById(R.id.list);
        listView.setDividerHeight(0);
        
        todoListEntryStorage = new TodoListEntryStorage(container);
        listView.setAdapter(todoListEntryStorage.getTodoListViewAdapter());
        
        // construct custom calendar view
        CalendarView calendarView = new CalendarView(view.findViewById(R.id.calendar), todoListEntryStorage);
        todoListEntryStorage.getTodoListViewAdapter().setDateUpdateListener(calendarView::notifyCurrentDayChanged);
        
        TextView statusText = view.findViewById(R.id.status_text);
        calendarView.setOnDateChangeListener((selectedDate, context) -> {
            updateDate(selectedDate.toEpochDay(), true);
            todoListEntryStorage.updateTodoListAdapter(false, false);
            updateStatusText(statusText);
        });
        
        calendarView.setOnMonthPreChangeListener((calendarMonth, firstVisibleDay, lastVisibleDay, context) ->
                // load current month before displaying the data
                todoListEntryStorage.lazyLoadEntries(context, firstVisibleDay, lastVisibleDay));
        
        view.findViewById(R.id.to_current_date_button).setOnClickListener(v -> calendarView.selectDay(currentDay));
        
        view.<FloatingActionButton>findViewById(R.id.fab).setOnClickListener(view1 -> {
            final List<Group> groupList = todoListEntryStorage.getGroups();
            displayEditTextSpinnerDialogue(view1.getContext(), R.string.add_event_fab, -1, R.string.event_name_input_hint,
                    R.string.cancel, R.string.add, R.string.add_to_global_list, "", groupList, 0,
                    (view2, text, selectedIndex) -> {
                        TodoListEntry newEntry = new TodoListEntry(view2.getContext(), new String[]{
                                TEXT_VALUE, text,
                                ASSOCIATED_DAY, String.valueOf(currentlySelectedDay),
                                IS_COMPLETED, "false"}, groupList.get(selectedIndex).getName(), groupList);
                        todoListEntryStorage.addEntry(newEntry);
                        todoListEntryStorage.saveEntries();
                        todoListEntryStorage.updateTodoListAdapter(newEntry.isVisibleOnLockscreen(), true);
                        return true;
                    },
                    (view2, text, selectedIndex) -> {
                        TodoListEntry newEntry = new TodoListEntry(view2.getContext(), new String[]{
                                TEXT_VALUE, text,
                                ASSOCIATED_DAY, DAY_FLAG_GLOBAL_STR,
                                IS_COMPLETED, "false"}, groupList.get(selectedIndex).getName(), groupList);
                        todoListEntryStorage.addEntry(newEntry);
                        todoListEntryStorage.saveEntries();
                        todoListEntryStorage.updateTodoListAdapter(newEntry.isVisibleOnLockscreen(), false);
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
                todoListEntryStorage.getCurrentlyVisibleEntriesCount()));
    }
    
    @Override
    public void onDestroy() {
        todoListEntryStorage = null;
        super.onDestroy();
    }
}