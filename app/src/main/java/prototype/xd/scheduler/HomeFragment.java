package prototype.xd.scheduler;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.entities.Group.BLANK_GROUP_NAME;
import static prototype.xd.scheduler.entities.Group.readGroupFile;
import static prototype.xd.scheduler.utilities.DateManager.addTimeZoneOffset;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDay;
import static prototype.xd.scheduler.utilities.DateManager.dateToEpoch;
import static prototype.xd.scheduler.utilities.DateManager.daysFromEpoch;
import static prototype.xd.scheduler.utilities.DateManager.updateDate;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DAY;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL_STR;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;
import static prototype.xd.scheduler.utilities.Utilities.loadTodoEntries;
import static prototype.xd.scheduler.utilities.Utilities.saveEntries;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Objects;

import prototype.xd.scheduler.adapters.TodoListViewAdapter;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.Keys;

public class HomeFragment extends Fragment {
    
    public TodoListViewAdapter todoListViewAdapter;
    
    public ArrayList<TodoListEntry> todoListEntries;
    
    public MainActivity rootActivity;
    
    public HomeFragment() {
        super();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    
        rootActivity = (MainActivity) requireActivity();
        
        View view = inflater.inflate(R.layout.fragment_home, container, false);
    
        todoListEntries = new ArrayList<>();
        ListView listView = view.findViewById(R.id.list);
        listView.setDividerHeight(0);
        todoListViewAdapter = new TodoListViewAdapter(HomeFragment.this, rootActivity);
        listView.setAdapter(todoListViewAdapter);
        
        CalendarView calendarView = view.findViewById(R.id.calendar);
        calendarView.setDate(preferences.getLong(Keys.PREVIOUSLY_SELECTED_DATE, calendarView.getDate()));
        calendarView.setOnDateChangeListener((view12, year, month, dayOfMonth) -> {
            preferences.edit().putLong(Keys.PREVIOUSLY_SELECTED_DATE, dateToEpoch(year, month, dayOfMonth)).apply();
            updateDate(year + "_" + (month + 1) + "_" + dayOfMonth, true);
            todoListViewAdapter.updateData(false);
        });
        
        view.<FloatingActionButton>findViewById(R.id.fab).setOnClickListener(view1 -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(rootActivity);
            builder.setTitle("Добавить пункт");
        
            View addView = inflater.inflate(R.layout.add_entry_dialogue, container, false);
            final EditText input = addView.findViewById(R.id.entryNameEditText);
        
            final String[] currentGroup = {BLANK_GROUP_NAME};
        
            final ArrayList<Group> groupList = readGroupFile();
            final ArrayList<String> groupNames = new ArrayList<>();
            for (Group group : groupList) {
                groupNames.add(group.name);
            }
            final Spinner groupSpinner = addView.findViewById(R.id.groupSpinner);
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(rootActivity, android.R.layout.simple_spinner_item, groupNames);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
            groupSpinner.setAdapter(arrayAdapter);
            groupSpinner.setSelection(groupNames.indexOf(BLANK_GROUP_NAME));
            groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                    currentGroup[0] = groupNames.get(position);
                }
            
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                
                }
            });
        
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(addView);
        
            builder.setPositiveButton("Добавить", (dialog, which) -> {
                TodoListEntry newEntry = new TodoListEntry(new String[]{
                        TEXT_VALUE, input.getText().toString(),
                        ASSOCIATED_DAY, String.valueOf(currentlySelectedDay),
                        IS_COMPLETED, "false"}, currentGroup[0]);
                todoListEntries.add(newEntry);
                saveEntries(todoListEntries);
                todoListViewAdapter.updateData(newEntry.getLockViewState());
            });
        
            builder.setNegativeButton("Добавить в общий список", (dialog, which) -> {
                TodoListEntry newEntry = new TodoListEntry(new String[]{
                        TEXT_VALUE, input.getText().toString(),
                        ASSOCIATED_DAY, DAY_FLAG_GLOBAL_STR,
                        IS_COMPLETED, "false"}, currentGroup[0]);
                todoListEntries.add(newEntry);
                saveEntries(todoListEntries);
                todoListViewAdapter.updateData(newEntry.getLockViewState());
            });
        
            builder.setNeutralButton("Отмена", (dialog, which) -> dialog.dismiss());
        
            builder.show();
        });
        
        
        view.findViewById(R.id.openSettingsButton).setOnClickListener(v ->
                ((NavHostFragment) Objects.requireNonNull(rootActivity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)))
                        .getNavController().navigate(R.id.action_HomeFragment_to_SettingsFragment));
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        updateDate(DAY_FLAG_GLOBAL_STR, true);
        
        new Thread(() -> {
            todoListEntries.clear();
            todoListEntries.addAll(loadTodoEntries(rootActivity));
            long epoch;
            if((epoch = preferences.getLong(Keys.PREVIOUSLY_SELECTED_DATE, 0)) != 0){
                currentlySelectedDay = daysFromEpoch(addTimeZoneOffset(epoch)); // timezone corrections because calendar returns in local timezone
            }
            if(todoListViewAdapter != null) todoListViewAdapter.updateData(false);
        }).start();
    }
    
    @Override
    public void onDestroy() {
        rootActivity = null;
        todoListViewAdapter = null;
        todoListEntries = null;
        preferences.edit().remove(Keys.PREVIOUSLY_SELECTED_DATE).apply();
        super.onDestroy();
    }
}