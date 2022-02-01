package prototype.xd.scheduler;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.entities.Group.readGroupFile;
import static prototype.xd.scheduler.utilities.DateManager.addTimeZoneOffset;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDay;
import static prototype.xd.scheduler.utilities.DateManager.dateToEpoch;
import static prototype.xd.scheduler.utilities.DateManager.daysFromEpoch;
import static prototype.xd.scheduler.utilities.DateManager.updateDate;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DAY;
import static prototype.xd.scheduler.utilities.Keys.BLANK_GROUP_NAME;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL_STR;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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

import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
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
        calendarView.setDate(preferences.getLong(Keys.PREVIOUSLY_SELECTED_DATE, calendarView.getDate()));
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            preferences.edit().putLong(Keys.PREVIOUSLY_SELECTED_DATE, dateToEpoch(year, month, dayOfMonth)).apply();
            updateDate(year + "_" + (month + 1) + "_" + dayOfMonth, true);
            todoListEntryStorage.lazyLoadEntries(view1.getContext());
        });
        
        view.<FloatingActionButton>findViewById(R.id.fab).setOnClickListener(view1 -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(view1.getContext());
            builder.setTitle(R.string.add_item);
            View addView = inflater.inflate(R.layout.add_entry_dialogue, container, false);
            builder.setView(addView);
            AlertDialog dialog = builder.create();
            
            final EditText input = addView.findViewById(R.id.entryNameEditText);
            input.setOnFocusChangeListener((v, hasFocus) -> input.postDelayed(() -> {
                InputMethodManager inputMethodManager = (InputMethodManager) view1.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }, 200));
            input.requestFocus();
            
            final String[] currentGroup = {BLANK_GROUP_NAME};
            
            final ArrayList<Group> groupList = readGroupFile();
            final ArrayList<String> groupNames = new ArrayList<>();
            for (Group group : groupList) {
                groupNames.add(group.name);
            }
            final Spinner groupSpinner = addView.findViewById(R.id.groupSpinner);
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(view1.getContext(), android.R.layout.simple_spinner_item, groupNames);
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
            
            addView.findViewById(R.id.save_button).setOnClickListener(v -> {
                TodoListEntry newEntry = new TodoListEntry(new String[]{
                        TEXT_VALUE, input.getText().toString().trim(),
                        ASSOCIATED_DAY, String.valueOf(currentlySelectedDay),
                        IS_COMPLETED, "false"}, currentGroup[0]);
                todoListEntryStorage.addEntry(newEntry);
                todoListEntryStorage.saveEntries();
                todoListEntryStorage.updateTodoListAdapter(newEntry.getLockViewState());
                dialog.dismiss();
            });
            
            addView.findViewById(R.id.add_to_global_button).setOnClickListener(v -> {
                TodoListEntry newEntry = new TodoListEntry(new String[]{
                        TEXT_VALUE, input.getText().toString().trim(),
                        ASSOCIATED_DAY, DAY_FLAG_GLOBAL_STR,
                        IS_COMPLETED, "false"}, currentGroup[0]);
                todoListEntryStorage.addEntry(newEntry);
                todoListEntryStorage.saveEntries();
                todoListEntryStorage.updateTodoListAdapter(newEntry.getLockViewState());
                dialog.dismiss();
            });
            
            addView.findViewById(R.id.cancel_button).setOnClickListener(v -> dialog.dismiss());
            
            dialog.show();
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
        if ((epoch = preferences.getLong(Keys.PREVIOUSLY_SELECTED_DATE, 0)) != 0) {
            currentlySelectedDay = daysFromEpoch(addTimeZoneOffset(epoch)); // timezone corrections because calendar returns in local timezone
        }
        todoListEntryStorage.lazyLoadEntries(view.getContext());
    }
    
    @Override
    public void onDestroy() {
        todoListEntryStorage = null;
        preferences.edit().remove(Keys.PREVIOUSLY_SELECTED_DATE).apply();
        super.onDestroy();
    }
}