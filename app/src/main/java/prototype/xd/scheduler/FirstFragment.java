package prototype.xd.scheduler;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import prototype.xd.scheduler.entities.TodoListEntry;

import static prototype.xd.scheduler.utilities.DateManager.*;
import static prototype.xd.scheduler.utilities.DateManager.updateDate;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.constructBitmap;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.initialiseBitmapDrawer;
import static prototype.xd.scheduler.utilities.Utilities.createRootIfNeeded;
import static prototype.xd.scheduler.utilities.Utilities.loadEntries;
import static prototype.xd.scheduler.utilities.Utilities.saveEntries;

public class FirstFragment extends Fragment {

    private static String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.SET_WALLPAPER,
            Manifest.permission.FOREGROUND_SERVICE,
    };

    ListView listView;
    ListViewAdapter listViewAdapter;

    ArrayList<TodoListEntry> todoList;

    SharedPreferences preferences;
    DisplayMetrics displayMetrics;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        verifyStoragePermissions(this.getActivity());

        createRootIfNeeded();
        updateDate("none", true);

        preferences = getContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);

        WallpaperManager wallpaperManager = WallpaperManager.getInstance(getContext());

        displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        initialiseBitmapDrawer(wallpaperManager, preferences, displayMetrics);

        boolean updateInBackground = preferences.getBoolean("bgUpdate", true);
        boolean updateOnStart = preferences.getBoolean("startupUpdate", false);

        final CalendarView datePicker = view.findViewById(R.id.calendar);

        todoList = loadEntries();

        listView = view.findViewById(R.id.list);
        listView.setDividerHeight(0);
        listViewAdapter = new ListViewAdapter(FirstFragment.this, listView);
        listView.setAdapter(listViewAdapter);

        datePicker.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                updateDate(year + "_" + (month + 1) + "_" + dayOfMonth, true);
                listViewAdapter.updateData();
            }
        });

        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Добавить пункт");

                final EditText input = new EditText(getContext());
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                builder.setPositiveButton("Добавить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        todoList.add(new TodoListEntry(new String[]{"value", input.getText().toString(), "associatedDate", currentlySelectedDate, "completed", "false"}, "default"));
                        saveEntries(todoList);
                        listViewAdapter.updateData();
                    }
                });

                builder.setNegativeButton("Добавить в общий список", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        todoList.add(new TodoListEntry(new String[]{"value", input.getText().toString(), "associatedDate", "GLOBAL", "completed", "false"}, "default"));
                        saveEntries(todoList);
                        listViewAdapter.updateData();
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

        view.findViewById(R.id.openSettingsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavHostFragment.findNavController(FirstFragment.this).navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });

        if (updateOnStart || preferences.getBoolean("settingsModified", false)) {
            preferences.edit().putBoolean("settingsModified", false).apply();
            constructBitmap();
        }

        if (updateInBackground) {
            getContext().startService(new Intent(getActivity(), prototype.xd.scheduler.BackgroudUpdateService.class));
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