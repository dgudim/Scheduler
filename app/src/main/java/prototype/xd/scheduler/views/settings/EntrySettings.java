package prototype.xd.scheduler.views.settings;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.entities.Group.createGroup;
import static prototype.xd.scheduler.entities.Group.readGroupFile;
import static prototype.xd.scheduler.entities.Group.saveGroupsFile;
import static prototype.xd.scheduler.utilities.Keys.ADAPTIVE_COLOR_BALANCE;
import static prototype.xd.scheduler.utilities.Keys.ADAPTIVE_COLOR_ENABLED;
import static prototype.xd.scheduler.utilities.Keys.BG_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BLANK_GROUP_NAME;
import static prototype.xd.scheduler.utilities.Keys.BORDER_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BORDER_THICKNESS;
import static prototype.xd.scheduler.utilities.Keys.EXPIRED_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Keys.FONT_COLOR;
import static prototype.xd.scheduler.utilities.Keys.NEED_TO_RECONSTRUCT_BITMAP;
import static prototype.xd.scheduler.utilities.Keys.PRIORITY;
import static prototype.xd.scheduler.utilities.Keys.SHOW_ON_LOCK;
import static prototype.xd.scheduler.utilities.Keys.UPCOMING_ITEMS_OFFSET;
import static prototype.xd.scheduler.utilities.Utilities.addSeekBarChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.addSwitchChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;
import static prototype.xd.scheduler.utilities.Utilities.saveEntries;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import prototype.xd.scheduler.HomeFragment;
import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;

public class EntrySettings extends PopupSettingsView {
    
    private final AlertDialog dialog;
    public final HomeFragment fragment;
    private TodoListEntry todoListEntry;
    
    public EntrySettings(final HomeFragment fragment, final View settingsView) {
        super(settingsView);
        this.fragment = fragment;
        
        dialog = new AlertDialog.Builder(settingsView.getContext()).setOnDismissListener(dialog -> {
            saveEntries(fragment.todoListEntries);
            fragment.todoListViewAdapter.updateData(preferences.getBoolean(NEED_TO_RECONSTRUCT_BITMAP, false));
            preferences.edit().putBoolean(NEED_TO_RECONSTRUCT_BITMAP, false).apply();
        }).setView(settingsView).create();
    }
    
    public void show(final TodoListEntry entry, final Context context) {
        initialise(entry, context);
        dialog.show();
    }
    
    private void initialise(TodoListEntry entry, Context context) {
        
        todoListEntry = entry;
        
        updatePreviews(entry.fontColor_original, entry.bgColor_original, entry.borderColor_original, entry.border_thickness_original);
        
        updateAllIndicators();
        
        final ArrayList<Group> groupList = readGroupFile();
        final ArrayList<String> groupNames = new ArrayList<>();
        for (Group group : groupList) {
            groupNames.add(group.name);
        }
        
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, groupNames) {
            @NonNull
            @Override
            public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                final View view = super.getView(position, convertView, parent);
                view.setLongClickable(true);
                if (convertView == null) {
                    view.setOnLongClickListener(view1 -> {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(view1.getContext());
                        if (groupNames.get(position).equals(BLANK_GROUP_NAME)) {
                            builder.setTitle(R.string.cant_rename_this_group);
                            builder.setMessage(R.string.break_settings_reset_message);
                        } else {
                            builder.setTitle(R.string.edit);
                            
                            final EditText input = new EditText(view.getContext());
                            input.setInputType(InputType.TYPE_CLASS_TEXT);
                            input.setText(groupNames.get(position));
                            input.setHint(R.string.title);
                            
                            builder.setView(input);
                            
                            builder.setPositiveButton(R.string.save, (dialog, which) -> {
                                if (!input.getText().toString().equals(BLANK_GROUP_NAME)) {
                                    groupNames.set(position, input.getText().toString());
                                    String origName = groupList.get(position).name;
                                    groupList.get(position).name = input.getText().toString();
                                    saveGroupsFile(groupList);
                                    for (TodoListEntry entry1 : fragment.todoListEntries) {
                                        if (entry1.group.name.equals(origName)) {
                                            entry1.group.name = input.getText().toString();
                                        }
                                    }
                                    saveEntries(fragment.todoListEntries);
                                    notifyDataSetChanged();
                                } else {
                                    dialog.dismiss();
                                    final AlertDialog.Builder builder1 = new AlertDialog.Builder(view1.getContext());
                                    builder1.setTitle(R.string.cant_use_this_group_name);
                                    builder1.setMessage(R.string.break_settings_reset_message);
                                    builder1.show();
                                }
                            });
                            
                            builder.setNeutralButton(R.string.delete_group, (dialog, which) -> {
                                AlertDialog.Builder builder2 = new AlertDialog.Builder(view1.getContext());
                                builder2.setTitle(R.string.delete);
                                builder2.setMessage(R.string.are_you_sure);
                                
                                builder2.setPositiveButton(R.string.yes, (dialog1, which1) -> {
                                    String origName = groupNames.get(group_spinner.getSelectedItemPosition());
                                    groupList.remove(group_spinner.getSelectedItemPosition());
                                    groupNames.remove(group_spinner.getSelectedItemPosition());
                                    saveGroupsFile(groupList);
                                    for (TodoListEntry entry1 : fragment.todoListEntries) {
                                        if (entry1.group.name.equals(origName)) {
                                            entry1.resetGroup();
                                        }
                                    }
                                    saveEntries(fragment.todoListEntries);
                                    group_spinner.setSelection(groupNames.indexOf(BLANK_GROUP_NAME));
                                    updateAllIndicators();
                                    notifyDataSetChanged();
                                });
                                
                                builder2.setNegativeButton(R.string.no, (dialog12, which12) -> dialog12.dismiss());
                                
                                builder2.show();
                            });
                            
                            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                            
                            
                        }
                        builder.show();
                        return true;
                    });
                }
                return view;
            }
        };
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        group_spinner.setAdapter(arrayAdapter);
        group_spinner.setSelection(groupNames.indexOf(entry.group.name));
        
        group_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!groupNames.get(position).equals(entry.group.name)) {
                    entry.changeGroup(groupNames.get(position));
                    saveEntries(fragment.todoListEntries);
                    preferences.edit().putBoolean(NEED_TO_RECONSTRUCT_BITMAP, true).apply();
                    initialise(entry, context);
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
        
        add_group.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle(R.string.add_current_config_as_group_prompt);
            builder.setMessage(R.string.add_current_config_as_group_message);
            
            final EditText input = new EditText(v.getContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setHint(R.string.title);
            builder.setView(input);
            
            builder.setPositiveButton(R.string.add, (dialog, which) -> {
                if (input.getText().toString().equals(BLANK_GROUP_NAME)) {
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(v.getContext());
                    builder2.setTitle(R.string.cant_create_group_with_this_name);
                    builder2.setMessage(R.string.break_settings_reset_message);
                    
                    builder2.show();
                    
                } else if (groupNames.contains(input.getText().toString())) {
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(v.getContext());
                    builder2.setTitle(R.string.group_with_same_name_exists);
                    builder2.setMessage(R.string.overwrite_prompt);
                    
                    builder2.setPositiveButton(R.string.yes, (dialog14, which14) -> {
                        Group createdGroup = createGroup(input.getText().toString(), entry.getDisplayParams());
                        groupList.set(groupNames.indexOf(input.getText().toString()), createdGroup);
                        saveGroupsFile(groupList);
                        for (TodoListEntry entry2 : fragment.todoListEntries) {
                            if (entry2.group.name.equals(input.getText().toString())) {
                                entry2.removeDisplayParams();
                                entry2.changeGroup(createdGroup);
                            }
                        }
                        updateAllIndicators();
                        preferences.edit().putBoolean(NEED_TO_RECONSTRUCT_BITMAP, true).apply();
                    });
                    
                    builder2.setNegativeButton(R.string.no, (dialog13, which13) -> dialog13.dismiss());
                    
                    builder2.show();
                } else {
                    Group createdGroup = createGroup(input.getText().toString(), entry.getDisplayParams());
                    groupNames.add(createdGroup.name);
                    groupList.add(createdGroup);
                    arrayAdapter.notifyDataSetChanged();
                    group_spinner.setSelection(groupNames.size() - 1);
                    saveGroupsFile(groupList);
                    entry.removeDisplayParams();
                    entry.changeGroup(createdGroup);
                    saveEntries(fragment.todoListEntries);
                    updateAllIndicators();
                }
            });
            
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            builder.show();
        });
        
        settings_reset_button.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle(R.string.reset_settings_prompt);
            
            builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                entry.removeDisplayParams();
                entry.resetGroup();
                saveEntries(fragment.todoListEntries);
                preferences.edit().putBoolean(NEED_TO_RECONSTRUCT_BITMAP, true).apply();
                initialise(entry, context);
            });
            builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
            
            builder.show();
        });
        
        fontColor_select.setOnClickListener(view -> invokeColorDialogue(
                fontColor_view_state, this, fragment,
                entry, FONT_COLOR, entry.fontColor_original));
        
        bgColor_select.setOnClickListener(view -> invokeColorDialogue(
                bgColor_view_state, this, fragment,
                entry, BG_COLOR, entry.bgColor_original));
        
        borderColor_select.setOnClickListener(view -> invokeColorDialogue(
                padColor_view_state, this, fragment,
                entry, BORDER_COLOR, entry.borderColor_original));
        
        addSeekBarChangeListener(
                border_thickness_description,
                border_thickness_bar, border_size_state,
                this, true, R.string.settings_border_thickness,
                BORDER_THICKNESS, entry.border_thickness_original);
        
        addSeekBarChangeListener(
                priority_description,
                priority_bar, priority_state,
                this, false, R.string.settings_priority,
                PRIORITY, entry.priority);
        
        addSeekBarChangeListener(
                adaptive_color_balance_description,
                adaptive_color_balance_bar, adaptiveColor_bar_state,
                this, false, R.string.settings_adaptive_color_balance,
                ADAPTIVE_COLOR_BALANCE, entry.adaptiveColorBalance);
        
        addSeekBarChangeListener(
                show_days_beforehand_description,
                show_days_beforehand_bar, showDaysUpcoming_bar_state,
                this, false, R.string.settings_show_days_upcoming,
                UPCOMING_ITEMS_OFFSET, entry.dayOffset_upcoming);
        
        addSeekBarChangeListener(
                show_days_after_description,
                show_days_after_bar, showDaysExpired_bar_state,
                this, false, R.string.settings_show_days_expired,
                EXPIRED_ITEMS_OFFSET, entry.dayOffset_expired);
        
        addSwitchChangeListener(
                show_on_lock_switch,
                show_on_lock_state,
                fragment, entry,
                SHOW_ON_LOCK, entry.showOnLock);
        
        addSwitchChangeListener(
                adaptive_color_switch,
                adaptiveColor_switch_state,
                fragment, entry,
                ADAPTIVE_COLOR_ENABLED, entry.adaptiveColorEnabled);
    }
    
    public void changeEntryParameter(TextView icon, String parameter, String value) {
        todoListEntry.changeParameter(parameter, value);
        setStateIconColor(icon, parameter);
    }
    
    @Override
    protected void setStateIconColor(TextView icon, String parameter) {
        todoListEntry.setStateIconColor(icon, parameter);
    }
}
