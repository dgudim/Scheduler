package prototype.xd.scheduler.utilities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import prototype.xd.scheduler.FirstFragment;
import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;

import static prototype.xd.scheduler.entities.Group.readGroupFile;
import static prototype.xd.scheduler.entities.TodoListEntry.*;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.preferences_static;
import static prototype.xd.scheduler.utilities.ScalingUtilities.createSolidColorCircle;
import static prototype.xd.scheduler.utilities.Utilities.addSeekBarChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;
import static prototype.xd.scheduler.utilities.Utilities.saveEntries;

public class EntrySettings {

    FirstFragment fragment;

    public EntrySettings(LayoutInflater inflater, final TodoListEntry entry, final Context context, final FirstFragment fragment) {
        this.fragment = fragment;
        View settingsView = inflater.inflate(R.layout.entry_settings, null);

        ImageView fontColor_list_view = settingsView.findViewById(R.id.textColor_list);
        ImageView fontColor_lock_view = settingsView.findViewById(R.id.textColor_lock);
        ImageView bgColor_list_view = settingsView.findViewById(R.id.backgroundColor_list);
        ImageView bgColor_lock_view = settingsView.findViewById(R.id.backgroundColor_lock);
        ImageView padColor_lock_view = settingsView.findViewById(R.id.padColor_lock);
        fontColor_list_view.setImageBitmap(createSolidColorCircle(entry.fontColor_list));
        fontColor_lock_view.setImageBitmap(createSolidColorCircle(entry.fontColor_lock));
        bgColor_list_view.setImageBitmap(createSolidColorCircle(entry.bgColor_list));
        bgColor_lock_view.setImageBitmap(createSolidColorCircle(entry.bgColor_lock));
        padColor_lock_view.setImageBitmap(createSolidColorCircle(entry.padColor));

        ArrayList<Group> groupList = readGroupFile();
        final ArrayList<String> groupNames = new ArrayList<>();
        for(Group group: groupList){
            groupNames.add(group.name);
        }
        Spinner groupSpinner = settingsView.findViewById(R.id.groupSpinner);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, groupNames);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down vieww
        groupSpinner.setAdapter(arrayAdapter);
        groupSpinner.setSelection(groupNames.indexOf(entry.group.name));
        groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                entry.changeGroup(groupNames.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        fontColor_list_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invokeColorDialogue(entry.fontColor_list, (ImageView) v, context, entry, FONT_COLOR_LIST, false);
            }
        });

        fontColor_lock_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invokeColorDialogue(entry.fontColor_lock, (ImageView) v, context, entry, FONT_COLOR_LOCK, true);
            }
        });

        bgColor_list_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invokeColorDialogue(entry.bgColor_list, (ImageView) v, context, entry, BACKGROUND_COLOR_LIST, false);
            }
        });

        bgColor_lock_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invokeColorDialogue(entry.bgColor_lock, (ImageView) v, context, entry, BACKGROUND_COLOR_LOCK, true);
            }
        });

        padColor_lock_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invokeColorDialogue(entry.padColor, (ImageView) v, context, entry, BEVEL_COLOR, true);
            }
        });

        addSeekBarChangeListener(
                (TextView) (settingsView.findViewById(R.id.textSizeDescription)),
                (SeekBar) (settingsView.findViewById(R.id.fontSizeBar)),
                entry.fontSize, R.string.settings_font_size, fragment, entry, FONT_SIZE);

        addSeekBarChangeListener(
                (TextView) (settingsView.findViewById(R.id.bevelThicknessDescription)),
                (SeekBar) (settingsView.findViewById(R.id.bevelThicknessBar)),
                entry.bevelSize, R.string.settings_bevel_thickness, fragment, entry, BEVEL_SIZE);

        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                saveEntries(fragment.todoList);
                fragment.listViewAdapter.updateData(preferences_static.getBoolean("need_to_reconstruct_bitmap", false));
                preferences_static.edit().putBoolean("need_to_reconstruct_bitmap", false).apply();
            }
        });

        alert.setView(settingsView);
        alert.show();
    }
}
