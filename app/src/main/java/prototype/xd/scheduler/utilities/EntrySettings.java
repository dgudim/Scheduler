package prototype.xd.scheduler.utilities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import prototype.xd.scheduler.FirstFragment;
import prototype.xd.scheduler.R;
import prototype.xd.scheduler.SecondFragment;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;

import static prototype.xd.scheduler.entities.Group.createGroup;
import static prototype.xd.scheduler.entities.Group.readGroupFile;
import static prototype.xd.scheduler.entities.Group.saveGroupsFile;
import static prototype.xd.scheduler.entities.TodoListEntry.*;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDate;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.displayHeight;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.preferences_static;
import static prototype.xd.scheduler.utilities.ScalingUtilities.createSolidColorCircle;
import static prototype.xd.scheduler.utilities.Utilities.addSeekBarChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;
import static prototype.xd.scheduler.utilities.Utilities.saveEntries;

public class EntrySettings {

    FirstFragment fragment;

    public EntrySettings(final LayoutInflater inflater, final TodoListEntry entry, final Context context, final FirstFragment fragment) {

        View settingsView = inflater.inflate(R.layout.entry_settings, null);

        initialise(entry, context, fragment, settingsView);

        final AlertDialog.Builder alert = new AlertDialog.Builder(context);

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

    private void initialise(final TodoListEntry entry, final Context context, final FirstFragment fragment, final View settingsView) {
        this.fragment = fragment;

        ImageView fontColor_list_view = settingsView.findViewById(R.id.textColor_list);
        ImageView fontColor_lock_view = settingsView.findViewById(R.id.textColor_lock);
        ImageView bgColor_list_view = settingsView.findViewById(R.id.backgroundColor_list);
        ImageView bgColor_lock_view = settingsView.findViewById(R.id.backgroundColor_lock);
        ImageView padColor_lock_view = settingsView.findViewById(R.id.padColor_lock);
        ImageView add_group = settingsView.findViewById(R.id.addGroup);
        fontColor_list_view.setImageBitmap(createSolidColorCircle(entry.fontColor_list));
        fontColor_lock_view.setImageBitmap(createSolidColorCircle(entry.fontColor_lock));
        bgColor_list_view.setImageBitmap(createSolidColorCircle(entry.bgColor_list));
        bgColor_lock_view.setImageBitmap(createSolidColorCircle(entry.bgColor_lock));
        padColor_lock_view.setImageBitmap(createSolidColorCircle(entry.padColor));

        final ArrayList<Group> groupList = readGroupFile();
        final ArrayList<String> groupNames = new ArrayList<>();
        for (Group group : groupList) {
            groupNames.add(group.name);
        }
        final Spinner groupSpinner = settingsView.findViewById(R.id.groupSpinner);

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, groupNames) {
            @NonNull
            @Override
            public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                final View view = super.getView(position, convertView, parent);
                view.setLongClickable(true);
                if (convertView == null) {
                    view.setOnLongClickListener(new View.OnLongClickListener() {

                        @Override
                        public boolean onLongClick(View view) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            if (groupNames.get(position).equals("Ничего")) {
                                builder.setTitle("Нельзя переименовать эту группу");
                                builder.setMessage("Ты сломаешь сброс настроек");

                            } else {
                                builder.setTitle("Изменить");

                                final EditText input = new EditText(context);
                                input.setInputType(InputType.TYPE_CLASS_TEXT);
                                input.setText(groupNames.get(position));
                                input.setHint("Название");

                                builder.setView(input);

                                builder.setPositiveButton("Сохранить", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        groupNames.set(position, input.getText().toString());
                                        groupList.get(position).name = input.getText().toString();
                                        saveGroupsFile(groupList);
                                        if (groupSpinner.getSelectedItemPosition() == position) {
                                            entry.group.name = input.getText().toString();
                                            saveEntries(fragment.todoList);
                                        }
                                        notifyDataSetChanged();
                                    }
                                });

                                builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });


                            }
                            builder.show();
                            return true;
                        }
                    });
                }
                return view;
            }
        };
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(arrayAdapter);
        groupSpinner.setSelection(groupNames.indexOf(entry.group.name));

        groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!groupNames.get(position).equals(entry.group.name)) {
                    entry.changeGroup(groupNames.get(position));
                    saveEntries(fragment.todoList);
                    preferences_static.edit().putBoolean("need_to_reconstruct_bitmap", true).apply();
                    initialise(entry, context, fragment, settingsView);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        add_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Добавить текущую конфигурацию как группу?");
                builder.setMessage("\n (Будут добавлены только те параметры, которые были изменены вручную)");

                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setHint("Название");
                builder.setView(input);

                builder.setPositiveButton("Добавить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (input.getText().toString().equals("Ничего")) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle("Нельзя создать группу с таким названием");
                            builder.setMessage("Ты сломаешь сброс настроек");

                            builder.show();

                        } else if (groupNames.contains(input.getText().toString())) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle("Группа с таким именем уже существует");
                            builder.setMessage("Перезаписать?");

                            builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Group createdGroup = createGroup(input.getText().toString(), entry.getDisplayParams());
                                    groupList.set(groupNames.indexOf(input.getText().toString()), createdGroup);
                                    saveGroupsFile(groupList);
                                    entry.removeDisplayParams();
                                    entry.changeGroup(createdGroup);
                                }
                            });

                            builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });

                            builder.show();
                        } else {
                            Group createdGroup = createGroup(input.getText().toString(), entry.getDisplayParams());
                            groupNames.add(createdGroup.name);
                            groupList.add(createdGroup);
                            arrayAdapter.notifyDataSetChanged();
                            groupSpinner.setSelection(groupNames.size() - 1);
                            saveGroupsFile(groupList);
                            entry.removeDisplayParams();
                            entry.changeGroup(createdGroup);
                            saveEntries(fragment.todoList);
                        }
                    }
                });

                builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });


                builder.show();
            }
        });

        settingsView.findViewById(R.id.settingsResetButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Сбросить настройки?");

                builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        entry.removeDisplayParams();
                        entry.changeGroup("Ничего");
                        saveEntries(fragment.todoList);
                        preferences_static.edit().putBoolean("need_to_reconstruct_bitmap", true).apply();
                        initialise(entry, context, fragment, settingsView);
                    }
                });
                builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.show();
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
    }

}
