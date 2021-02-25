package prototype.xd.scheduler;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import static prototype.xd.scheduler.utilities.DateManager.currentDate;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDate;
import static prototype.xd.scheduler.utilities.DateManager.yesterdayDate;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.constructBitmap;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.showYesterdayCompletedItemsInList;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.showYesterdayItemsInList;
import static prototype.xd.scheduler.utilities.Utilities.loadEntries;
import static prototype.xd.scheduler.utilities.Utilities.saveEntries;

public class ListViewAdapter extends BaseAdapter {

    Context context;
    ArrayList<String> todoList;
    ArrayList<String> yesterdayCompletedItems;

    LayoutInflater inflater;
    ListView currentTodoListView;
    FirstFragment fragment;

    public ListViewAdapter(FirstFragment fragment, ListView currentTodoListView) {
        this.fragment = fragment;
        this.context = fragment.getContext();
        this.todoList = loadEntries(currentlySelectedDate);
        yesterdayCompletedItems = new ArrayList<>();
        if (showYesterdayItemsInList) {
            mergeArrays(loadEntries(yesterdayDate));
        }
        todoList.addAll(loadEntries("list_global"));
        this.currentTodoListView = currentTodoListView;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return todoList.size();
    }

    @Override
    public Object getItem(int i) {
        return todoList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    public void mergeArrays(ArrayList<String> yesterdayEntries) {
        if (currentlySelectedDate.equals(currentDate)) {
            for (int i = 0; i < yesterdayEntries.size(); i++) {
                todoList.add(yesterdayEntries.get(i) + "_Y");
            }
        }
    }

    public void updateData(boolean reconstructBitmap) {
        todoList = loadEntries(currentlySelectedDate);
        if (showYesterdayItemsInList) {
            mergeArrays(loadEntries(yesterdayDate));
        }
        todoList.addAll(loadEntries("list_global"));
        if (reconstructBitmap) {
            constructBitmap();
        }
        notifyDataSetChanged();
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {

        boolean hide = !showYesterdayCompletedItemsInList && todoList.get(i).endsWith("_1_Y");
        view = inflater.inflate(R.layout.list_selection, null);

        if (!hide) {
            final CheckBox isDone = view.findViewById(R.id.isDone);

            final TextView todoText = view.findViewById(R.id.todoText);

            int params = 1;
            final boolean global;

            if (todoList.get(i).endsWith("_Y")) {
                todoText.setTextColor(Color.parseColor("#CC0000"));
                params = 2;
            }

            global = todoList.get(i).endsWith("_G");
            if (global) {
                todoText.setTextColor(Color.parseColor("#00CC00"));
            }

            if (todoList.get(i).endsWith("_1") || todoList.get(i).endsWith("_1_Y")) {
                if (params == 1) {
                    todoText.setTextColor(Color.parseColor("#CCCCCC"));
                } else {
                    todoText.setTextColor(Color.parseColor("#FFCCCC"));
                }
            }

            todoText.setText(todoList.get(i).substring(0, todoList.get(i).length() - params * 2));
            ImageView delete = view.findViewById(R.id.deletionButton);

            final int finalParams = params;

            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    AlertDialog.Builder alert = new AlertDialog.Builder(context);
                    alert.setTitle("Удалить");
                    alert.setMessage("Вы уверены?");
                    alert.setPositiveButton("Да", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!global) {
                                if (finalParams == 1) {
                                    fragment.todoList.remove(i);
                                    saveEntries(currentlySelectedDate, fragment.todoList);
                                } else {
                                    ArrayList<String> yesterdayList = loadEntries(yesterdayDate);
                                    yesterdayList.remove(i - fragment.todoList.size());
                                    saveEntries(yesterdayDate, yesterdayList);
                                }
                            } else {
                                fragment.globalList.remove(i - todoList.size() + fragment.globalList.size());
                                saveEntries("list_global", fragment.globalList);
                            }
                            updateData(true);
                            dialog.dismiss();
                        }
                    });

                    alert.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    alert.show();
                }
            });
            isDone.setChecked(todoList.get(i).endsWith("_1") || todoList.get(i).endsWith("_1_Y"));
            isDone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String modifier = "0";
                    if (isDone.isChecked()) modifier = "1";
                    if (!global) {
                        if (finalParams == 1) {
                            if (isDone.isChecked()) {
                                todoText.setTextColor(Color.parseColor("#000000"));
                            } else {
                                todoText.setTextColor(Color.parseColor("#CCCCCC"));
                            }
                            fragment.todoList.set(i, todoList.get(i).substring(0, todoList.get(i).length() - 1) + modifier);
                            saveEntries(currentlySelectedDate, fragment.todoList);
                        } else {
                            if (isDone.isChecked()) {
                                todoText.setTextColor(Color.parseColor("#CC0000"));
                            } else {
                                todoText.setTextColor(Color.parseColor("#FFCCCC"));
                            }
                            ArrayList<String> yesterdayList = loadEntries(yesterdayDate);
                            yesterdayList.set(i - fragment.todoList.size(), todoList.get(i).substring(0, todoList.get(i).length() - 3) + modifier);
                            saveEntries(yesterdayDate, yesterdayList);
                        }
                    } else {
                        fragment.todoList.add(todoList.get(i).substring(0, todoList.get(i).length() - 2) + "_0");
                        saveEntries(currentlySelectedDate, fragment.todoList);

                        fragment.globalList.remove(i - todoList.size() + fragment.globalList.size());
                        saveEntries("list_global", fragment.globalList);
                    }
                    updateData(true);
                }
            });
            todoText.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(context);

                    final EditText input = new EditText(context);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    input.setText(todoList.get(i).substring(0, todoList.get(i).length() - finalParams * 2));
                    final String modifier = todoList.get(i).substring(todoList.get(i).length() - finalParams * 2);
                    alert.setView(input);

                    alert.setTitle("Изменить");
                    alert.setPositiveButton("Сохранить", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!global) {
                                if (finalParams == 1) {
                                    fragment.todoList.set(i, input.getText() + modifier);
                                    saveEntries(currentlySelectedDate, fragment.todoList);
                                } else {
                                    ArrayList<String> yesterdayList = loadEntries(yesterdayDate);
                                    yesterdayList.set(i - fragment.todoList.size(), input.getText() + modifier);
                                    saveEntries(yesterdayDate, yesterdayList);
                                }
                            } else {
                                fragment.globalList.set(i - todoList.size() + fragment.globalList.size(), input.getText() + "_G");
                                saveEntries("list_global", fragment.globalList);
                            }
                            updateData(true);
                            dialog.dismiss();
                        }
                    });

                    if (!global) {
                        alert.setNeutralButton("Переместить в общий список", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if (finalParams == 1) {
                                    fragment.globalList.add(input.getText() + "_G");
                                    fragment.todoList.remove(i);
                                    saveEntries(currentlySelectedDate, fragment.todoList);
                                    saveEntries("list_global", fragment.globalList);
                                } else {
                                    fragment.globalList.add(input.getText() + "_G");
                                    ArrayList<String> yesterdayList = loadEntries(yesterdayDate);
                                    yesterdayList.remove(i - fragment.todoList.size());
                                    saveEntries(yesterdayDate, yesterdayList);
                                    saveEntries("list_global", fragment.globalList);
                                }

                                updateData(true);
                            }
                        });
                    }

                    alert.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    alert.show();
                    return false;
                }
            });
            return view;
        } else {
            TextView emptyView = new TextView(context);
            emptyView.setVisibility(View.GONE);
            emptyView.setHeight(0);
            return emptyView;
        }
    }
}
