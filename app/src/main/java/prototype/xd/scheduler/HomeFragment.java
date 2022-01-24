package prototype.xd.scheduler;

import static prototype.xd.scheduler.entities.Group.BLANK_NAME;
import static prototype.xd.scheduler.entities.Group.readGroupFile;
import static prototype.xd.scheduler.utilities.DateManager.currentDate;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDate;
import static prototype.xd.scheduler.utilities.DateManager.updateDate;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DATE;
import static prototype.xd.scheduler.utilities.Keys.DATE_FLAG_GLOBAL;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;
import static prototype.xd.scheduler.utilities.Utilities.initStorage;
import static prototype.xd.scheduler.utilities.Utilities.loadTodoEntries;
import static prototype.xd.scheduler.utilities.Utilities.saveEntries;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import pl.droidsonroids.gif.GifImageView;
import prototype.xd.scheduler.adapters.ListViewAdapter;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.LockScreenBitmapDrawer;

public class HomeFragment extends Fragment {
    
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.SET_WALLPAPER,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
    };
    
    public ListViewAdapter listViewAdapter;
    
    public ArrayList<TodoListEntry> todoListEntries;
    public ArrayList<TodoListEntry> calendarEntries;
    
    private LockScreenBitmapDrawer lockScreenBitmapDrawer;
    
    private Timer queueTimer;
    
    public Activity rootActivity;
    
    public Context context;
    
    private ViewGroup rootViewGroup;
    
    public HomeFragment() {
        super();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.rootViewGroup = container;
        return inflater.inflate(R.layout.fragment_home, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        rootActivity = requireActivity();
        
        verifyStoragePermissions(rootActivity);
        
        context = requireContext();
        
        initStorage(context);
        updateDate("none", true);
        
        if (lockScreenBitmapDrawer == null) {
            lockScreenBitmapDrawer = new LockScreenBitmapDrawer(context);
        }
        
        final CalendarView datePicker = view.findViewById(R.id.calendar);
        
        todoListEntries = loadTodoEntries();
        
        ListView listView = view.findViewById(R.id.list);
        listView.setDividerHeight(0);
        listViewAdapter = new ListViewAdapter(HomeFragment.this, lockScreenBitmapDrawer);
        listView.setAdapter(listViewAdapter);
        
        datePicker.setOnDateChangeListener((view12, year, month, dayOfMonth) -> {
            updateDate(year + "_" + (month + 1) + "_" + dayOfMonth, true);
            listViewAdapter.updateData(currentlySelectedDate.equals(currentDate));
        });
        
        LayoutInflater inflater = LayoutInflater.from(context);
        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(view1 -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Добавить пункт");
            
            View addView = inflater.inflate(R.layout.add_entry_dialogue, rootViewGroup, false);
            final EditText input = addView.findViewById(R.id.entryNameEditText);
            
            final String[] currentGroup = {BLANK_NAME};
            
            final ArrayList<Group> groupList = readGroupFile();
            final ArrayList<String> groupNames = new ArrayList<>();
            for (Group group : groupList) {
                groupNames.add(group.name);
            }
            final Spinner groupSpinner = addView.findViewById(R.id.groupSpinner);
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, groupNames);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
            groupSpinner.setAdapter(arrayAdapter);
            groupSpinner.setSelection(groupNames.indexOf(BLANK_NAME));
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
                        ASSOCIATED_DATE, currentlySelectedDate,
                        IS_COMPLETED, "false"}, currentGroup[0]);
                todoListEntries.add(newEntry);
                saveEntries(todoListEntries);
                listViewAdapter.updateData(newEntry.getLockViewState());
            });
            
            builder.setNegativeButton("Добавить в общий список", (dialog, which) -> {
                TodoListEntry newEntry = new TodoListEntry(new String[]{
                        TEXT_VALUE, input.getText().toString(),
                        ASSOCIATED_DATE, DATE_FLAG_GLOBAL,
                        IS_COMPLETED, "false"}, currentGroup[0]);
                todoListEntries.add(newEntry);
                saveEntries(todoListEntries);
                listViewAdapter.updateData(newEntry.getLockViewState());
            });
            
            builder.setNeutralButton("Отмена", (dialog, which) -> dialog.dismiss());
            
            builder.show();
        });
        
        final GifImageView loadingGif = view.findViewById(R.id.loadingIcon);
        final TextView loadingText = view.findViewById(R.id.queueText);
        
        queueTimer = new Timer();
        queueTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                lockScreenBitmapDrawer.checkQueue();
                rootActivity.runOnUiThread(() -> {
                    if (lockScreenBitmapDrawer.currentlyProcessingBitmap) {
                        loadingGif.setVisibility(View.VISIBLE);
                    } else {
                        loadingGif.setVisibility(View.GONE);
                    }
                    if (lockScreenBitmapDrawer.needBitmapProcessing) {
                        loadingText.setVisibility(View.VISIBLE);
                    } else {
                        loadingText.setVisibility(View.GONE);
                    }
                });
            }
        }, 0, 100);
        
        view.findViewById(R.id.openSettingsButton).setOnClickListener(v -> {
            queueTimer.cancel();
            NavHostFragment.findNavController(HomeFragment.this).navigate(R.id.action_HomeFragment_to_SettingsFragment);
        });
        
        lockScreenBitmapDrawer.constructBitmap();
    }
    
    void verifyStoragePermissions(Activity activity) {
        boolean granted = false;
        ActivityCompat.requestPermissions(activity, PERMISSIONS, 1);
        while (!granted) {
            granted = true;
            for (String permission : PERMISSIONS) {
                granted = granted && ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
            }
        }
    }
}