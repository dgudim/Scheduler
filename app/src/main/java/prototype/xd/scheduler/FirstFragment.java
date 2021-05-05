package prototype.xd.scheduler;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
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
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.LockScreenBitmapDrawer;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.entities.Group.BLANK_NAME;
import static prototype.xd.scheduler.entities.Group.readGroupFile;
import static prototype.xd.scheduler.utilities.DateManager.*;
import static prototype.xd.scheduler.utilities.DateManager.updateDate;
import static prototype.xd.scheduler.utilities.Utilities.createRootIfNeeded;
import static prototype.xd.scheduler.utilities.Utilities.loadEntries;
import static prototype.xd.scheduler.utilities.Utilities.saveEntries;

public class FirstFragment extends Fragment {

    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.SET_WALLPAPER,
            Manifest.permission.FOREGROUND_SERVICE,
    };

    ListView listView;
    public ListViewAdapter listViewAdapter;

    public ArrayList<TodoListEntry> todoListEntries;

    LockScreenBitmapDrawer lockScreenBitmapDrawer;

    Timer queueTimer;

    Activity rootActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rootActivity = getActivity();

        verifyStoragePermissions(rootActivity);

        createRootIfNeeded();
        updateDate("none", true);

        lockScreenBitmapDrawer = new LockScreenBitmapDrawer(getContext());

        boolean updateInBackground = preferences.getBoolean("bgUpdate", true);
        boolean updateOnStart = preferences.getBoolean("startupUpdate", false);

        final CalendarView datePicker = view.findViewById(R.id.calendar);

        todoListEntries = loadEntries();

        listView = view.findViewById(R.id.list);
        listView.setDividerHeight(0);
        listViewAdapter = new ListViewAdapter(FirstFragment.this, listView, lockScreenBitmapDrawer);
        listView.setAdapter(listViewAdapter);

        datePicker.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                updateDate(year + "_" + (month + 1) + "_" + dayOfMonth, true);
                listViewAdapter.updateData(currentlySelectedDate.equals(currentDate) || currentlySelectedDate.equals(yesterdayDate));
            }
        });

        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Добавить пункт");

                View addView = LayoutInflater.from(getContext()).inflate(R.layout.add_entry_dialogue, null);
                final EditText input = addView.findViewById(R.id.entryNameEditText);

                final String[] currentGroup = {BLANK_NAME};

                final ArrayList<Group> groupList = readGroupFile();
                final ArrayList<String> groupNames = new ArrayList<>();
                for (Group group : groupList) {
                    groupNames.add(group.name);
                }
                final Spinner groupSpinner = addView.findViewById(R.id.groupSpinner);
                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, groupNames);
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
                groupSpinner.setAdapter(arrayAdapter);
                groupSpinner.setSelection(groupNames.indexOf(BLANK_NAME));
                groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        currentGroup[0] = groupNames.get(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(addView);

                builder.setPositiveButton("Добавить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TodoListEntry newEntry = new TodoListEntry(new String[]{
                                "value", input.getText().toString(),
                                "associatedDate", currentlySelectedDate,
                                "completed", "false"}, currentGroup[0]);
                        todoListEntries.add(newEntry);
                        saveEntries(todoListEntries);
                        listViewAdapter.updateData(newEntry.getLockViewState());
                    }
                });

                builder.setNegativeButton("Добавить в общий список", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TodoListEntry newEntry = new TodoListEntry(new String[]{
                                "value", input.getText().toString(),
                                "associatedDate", "GLOBAL",
                                "completed", "false"}, currentGroup[0]);
                        todoListEntries.add(newEntry);
                        saveEntries(todoListEntries);
                        listViewAdapter.updateData(newEntry.getLockViewState());
                    }
                });

                builder.setNeutralButton("Отмена", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.show();
            }
        });

        final GifImageView loadingGif = view.findViewById(R.id.loadingIcon);
        final TextView loadingText = view.findViewById(R.id.queueText);

        queueTimer = new Timer();
        queueTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                lockScreenBitmapDrawer.checkQueue();
                rootActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(lockScreenBitmapDrawer.currentlyProcessingBitmap){
                            loadingGif.setVisibility(View.VISIBLE);
                        }else{
                            loadingGif.setVisibility(View.GONE);
                        }
                        if(lockScreenBitmapDrawer.needBitmapProcessing){
                            loadingText.setVisibility(View.VISIBLE);
                        }else{
                            loadingText.setVisibility(View.GONE);
                        }
                    }
                });
            }
        }, 0, 100);

        view.findViewById(R.id.openSettingsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                queueTimer.cancel();
                NavHostFragment.findNavController(FirstFragment.this).navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });

        if (updateOnStart || preferences.getBoolean("settingsModified", false)) {
            preferences.edit().putBoolean("settingsModified", false).apply();
            lockScreenBitmapDrawer.constructBitmap();
        }

        if (updateInBackground) {
            getContext().startService(new Intent(rootActivity, BackgroundUpdateService.class));
        }
    }

    void verifyStoragePermissions(Activity activity) {
        boolean granted = true;
        for (int i = 0; i < PERMISSIONS.length; i++) {
            granted = granted && ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        if (!granted) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS, 1);
        }
    }
}