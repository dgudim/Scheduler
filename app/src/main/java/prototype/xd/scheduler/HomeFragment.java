package prototype.xd.scheduler;

import static prototype.xd.scheduler.MainActivity.preferences_service;
import static prototype.xd.scheduler.entities.Group.readGroupFile;
import static prototype.xd.scheduler.utilities.DateManager.addTimeZoneOffset;
import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.DateManager.currentTimestamp;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDay;
import static prototype.xd.scheduler.utilities.DateManager.dateFromEpoch;
import static prototype.xd.scheduler.utilities.DateManager.dateToEpoch;
import static prototype.xd.scheduler.utilities.DateManager.daysFromEpoch;
import static prototype.xd.scheduler.utilities.DateManager.timeZone_SYSTEM;
import static prototype.xd.scheduler.utilities.DateManager.updateDate;
import static prototype.xd.scheduler.utilities.DialogueUtilities.displayEditTextSpinnerDialogue;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DAY;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL_STR;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Objects;

import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.DialogueUtilities;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.TodoListEntryStorage;

public class HomeFragment extends Fragment {
    
    private volatile TodoListEntryStorage todoListEntryStorage;
    
    public HomeFragment() {
        super();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        ListView listView = view.findViewById(R.id.list);
        listView.setDividerHeight(0);
        
        todoListEntryStorage = new TodoListEntryStorage(container);
        listView.setAdapter(todoListEntryStorage.getTodoListViewAdapter());
        
        CalendarView calendarView = view.findViewById(R.id.calendar);
        TextView statusText = view.findViewById(R.id.status_text);
        long epoch;
        if ((epoch = preferences_service.getLong(Keys.PREVIOUSLY_SELECTED_DATE, 0)) != 0) {
            calendarView.setDate(addTimeZoneOffset(epoch, timeZone_SYSTEM));
        }
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            preferences_service.edit().putLong(Keys.PREVIOUSLY_SELECTED_DATE, dateToEpoch(year, month + 1, dayOfMonth)).apply();
            updateDate(year + "_" + (month + 1) + "_" + dayOfMonth, true);
            todoListEntryStorage.lazyLoadEntries(view1.getContext());
            updateStatusText(statusText);
        });
        
        view.findViewById(R.id.to_current_date_button).setOnClickListener(v -> {
            calendarView.setDate(addTimeZoneOffset(currentTimestamp, timeZone_SYSTEM));
            currentlySelectedDay = currentDay;
            preferences_service.edit().remove(Keys.PREVIOUSLY_SELECTED_DATE).apply();
            todoListEntryStorage.updateTodoListAdapter(false);
            updateStatusText(statusText);
        });
        
        view.<FloatingActionButton>findViewById(R.id.fab).setOnClickListener(view1 -> {
            final ArrayList<Group> groupList = new ArrayList<>();
            groupList.add(new Group(view1.getContext()));
            groupList.addAll(readGroupFile());
            displayEditTextSpinnerDialogue(view1.getContext(), R.string.add_event_fab, -1, R.string.event_name_input_hint,
                    R.string.cancel, R.string.add, R.string.add_to_global_list, "", groupList, 0,
                    new DialogueUtilities.OnClickListenerWithEditText() {
                        @Override
                        public boolean onClick(View view, String text, int selectedIndex) {
                            TodoListEntry newEntry = new TodoListEntry(view.getContext(), new String[]{
                                    TEXT_VALUE, text.trim(),
                                    ASSOCIATED_DAY, String.valueOf(currentlySelectedDay),
                                    IS_COMPLETED, "false"}, groupList.get(selectedIndex).getName());
                            todoListEntryStorage.addEntry(newEntry);
                            todoListEntryStorage.saveEntries();
                            todoListEntryStorage.updateTodoListAdapter(newEntry.getLockViewState());
                            return true;
                        }
                    },
                    new DialogueUtilities.OnClickListenerWithEditText() {
                        @Override
                        public boolean onClick(View view, String text, int selectedIndex) {
                            TodoListEntry newEntry = new TodoListEntry(view.getContext(), new String[]{
                                    TEXT_VALUE, text.trim(),
                                    ASSOCIATED_DAY, DAY_FLAG_GLOBAL_STR,
                                    IS_COMPLETED, "false"}, groupList.get(selectedIndex).getName());
                            todoListEntryStorage.addEntry(newEntry);
                            todoListEntryStorage.saveEntries();
                            todoListEntryStorage.updateTodoListAdapter(newEntry.getLockViewState());
                            return true;
                        }
                    });
        });
        
        view.findViewById(R.id.openSettingsButton).setOnClickListener(v ->
                ((NavHostFragment) Objects.requireNonNull(requireActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)))
                        .getNavController().navigate(R.id.action_HomeFragment_to_SettingsFragment));
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        updateDate(DAY_FLAG_GLOBAL_STR, true);
        
        long epoch;
        if ((epoch = preferences_service.getLong(Keys.PREVIOUSLY_SELECTED_DATE, 0)) != 0) {
            currentlySelectedDay = daysFromEpoch(epoch, timeZone_SYSTEM);
        }
        todoListEntryStorage.lazyLoadEntries(view.getContext());
        
        updateStatusText(view.findViewById(R.id.status_text));
    }
    
    private void updateStatusText(TextView statusText) {
        statusText.setText(getString(R.string.status, dateFromEpoch(currentlySelectedDay * 86400000),
                todoListEntryStorage.getCurrentlyVisibleEntries()));
    }
    
    @Override
    public void onDestroy() {
        todoListEntryStorage = null;
        preferences_service.edit().remove(Keys.PREVIOUSLY_SELECTED_DATE).apply();
        super.onDestroy();
    }
}