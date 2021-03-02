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

import prototype.xd.scheduler.entities.TodoListEntry;

import static prototype.xd.scheduler.utilities.DateManager.currentDate;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDate;
import static prototype.xd.scheduler.utilities.DateManager.yesterdayDate;
import static prototype.xd.scheduler.utilities.LockScreenBitmapDrawer.constructBitmap;
import static prototype.xd.scheduler.utilities.Utilities.loadEntries;
import static prototype.xd.scheduler.utilities.Utilities.saveEntries;

public class ListViewAdapter extends BaseAdapter {

    Context context;
    ArrayList<TodoListEntry> todoList;

    LayoutInflater inflater;
    ListView currentTodoListView;
    FirstFragment fragment;

    public ListViewAdapter(FirstFragment fragment, ListView currentTodoListView) {
        this.fragment = fragment;
        this.context = fragment.getContext();
        this.todoList = fragment.todoList;
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

    public void updateData() {
        this.todoList = fragment.todoList;
        constructBitmap();
        notifyDataSetChanged();
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {

        boolean show = todoList.get(i).showInList_ifCompleted || (!todoList.get(i).completed && todoList.get(i).showInList);
        if (todoList.get(i).isYesterdayEntry || (!todoList.get(i).isGlobalEntry && !todoList.get(i).isYesterdayEntry)) {
            show = show && todoList.get(i).associatedDate.equals(currentlySelectedDate);
        }

        view = inflater.inflate(R.layout.list_selection, null);

        if (show) {
            final CheckBox isDone = view.findViewById(R.id.isDone);

            final TextView todoText = view.findViewById(R.id.todoText);

            if (todoList.get(i).completed) {
                todoText.setTextColor(todoList.get(i).fontColor_list_completed);
            } else {
                todoText.setTextColor(todoList.get(i).fontColor_list);
            }

            todoText.setText(todoList.get(i).textValue);
            ImageView delete = view.findViewById(R.id.deletionButton);

            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    AlertDialog.Builder alert = new AlertDialog.Builder(context);
                    alert.setTitle("Удалить");
                    alert.setMessage("Вы уверены?");
                    alert.setPositiveButton("Да", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            fragment.todoList.remove(i);
                            saveEntries(fragment.todoList);
                            updateData();
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
            isDone.setChecked(todoList.get(i).completed);
            isDone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (!todoList.get(i).isGlobalEntry) {
                        if (isDone.isChecked()) {
                            todoText.setTextColor(todoList.get(i).fontColor_list_completed);
                        } else {
                            todoText.setTextColor(todoList.get(i).fontColor_list);
                        }
                        fragment.todoList.get(i).changeParameter(TodoListEntry.IS_COMPLETED, String.valueOf(isDone.isChecked()), true, true);
                        saveEntries(fragment.todoList);
                    } else {
                        fragment.todoList.get(i).changeParameter(TodoListEntry.ASSOCIATED_DATE, currentlySelectedDate, true, true);
                        saveEntries(fragment.todoList);
                    }
                    updateData();
                }
            });

            todoText.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(context);

                    final EditText input = new EditText(context);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    input.setText(todoList.get(i).textValue);
                    alert.setView(input);

                    alert.setTitle("Изменить");
                    alert.setPositiveButton("Сохранить", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            fragment.todoList.get(i).changeParameter(TodoListEntry.TEXT_VALUE, input.getText().toString(), true, true);
                            saveEntries(fragment.todoList);
                            updateData();
                        }
                    });

                    if (!fragment.todoList.get(i).isGlobalEntry) {
                        alert.setNeutralButton("Переместить в общий список", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                fragment.todoList.get(i).changeParameter(TodoListEntry.ASSOCIATED_DATE, "GLOBAL", true, true);
                                saveEntries(fragment.todoList);
                                updateData();
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
