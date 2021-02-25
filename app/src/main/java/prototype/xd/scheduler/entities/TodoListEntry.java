package prototype.xd.scheduler.entities;

import android.content.SharedPreferences;

import static org.apache.commons.lang.ArrayUtils.addAll;
import static prototype.xd.scheduler.utilities.DateManager.currentDate;
import static prototype.xd.scheduler.utilities.DateManager.yesterdayDate;

public class TodoListEntry {

    String associatedDate;
    boolean completed;
    Group group;

    int bgColor_lock;
    int bgColor_list;
    int bgColor_list_completed;

    int padColor_lock;

    int fontSize;
    int padSize;

    int fontColor = 0xFF000000;

    boolean showOnLock;
    boolean showOnLock_ifCompleted;
    boolean showInList;
    boolean showInList_ifCompleted;

    String textValue;

    public TodoListEntry(String[] params, String groupName, SharedPreferences preferences) {
        group = new Group(groupName);
        for (int i = 0; i < params.length; i++) {
            if (params[i].equals("associatedDate")) {
                if (params[i + 1].equals(currentDate)) {

                    bgColor_lock = preferences.getInt("todayBgColor", 0xFFFFFFFF);
                    padColor_lock = preferences.getInt("todayBevelColor", 0xFF888888);
                    padSize = preferences.getInt("defaultBevelThickness", 5);

                    showInList = true;
                    showInList_ifCompleted = true;
                    showOnLock = true;
                    showOnLock_ifCompleted = preferences.getBoolean("completedTasks", false);

                } else if (params[i + 1].equals(yesterdayDate)) {

                    bgColor_lock = preferences.getInt("yesterdayBgColor", 0xFFFFCCCC);
                    padColor_lock = preferences.getInt("yesterdayBevelColor", 0xFFFF8888);
                    padSize = preferences.getInt("yesterdayBevelThickness", 5);

                    showOnLock_ifCompleted = preferences.getBoolean("yesterdayItemsLock", false);
                    showInList_ifCompleted = preferences.getBoolean("yesterdayItemsList", false);
                    showInList = preferences.getBoolean("yesterdayTasks", true);
                    showOnLock = preferences.getBoolean("yesterdayTasksLock", true);

                } else if(params[i + 1].equals("GLOBAL")){

                    bgColor_lock = preferences.getInt("globalBgColor", 0xFFCCFFCC);
                    padColor_lock = preferences.getInt("globalBevelColor", 0xFF88FF88);
                    padSize = preferences.getInt("globalBevelThickness", 5);

                    showOnLock_ifCompleted = false;
                    showInList_ifCompleted = false;
                    showInList = true;
                    showOnLock = preferences.getBoolean("globalTasksLock", true);

                }
            }
        }
        fontSize = preferences.getInt("fontSize", 21);
        setParams((String[]) addAll(params, group.params));
    }

    // TODO: 25.02.2021 load all params (bgColor, etc) 

    private void setParams(String[] params) {
        for (int i = 0; i < params.length; i++) {
            switch (params[i]) {
                case ("value"):
                    textValue = params[i];
                    break;
                case ("completed0"):
                    completed = false;
                    break;
                case ("completed1"):
                    completed = true;
                    break;
                case ("lock0"):
                    showOnLock = false;
                    break;
                case ("lock1"):
                    showOnLock = true;
                    break;
                case ("fontSize"):
                    fontSize = Integer.parseInt(params[i + 1]);
                    break;
                case ("padSize"):
                    padSize = Integer.parseInt(params[i + 1]);
                    break;
                case ("fontColor"):
                    fontColor = Integer.parseInt(params[i + 1]);
                    break;
                case ("bgColor"):
                    bgColor_lock = Integer.parseInt(params[i + 1]);
                    break;
                case ("padColor"):
                    padColor_lock = Integer.parseInt(params[i + 1]);
                    break;
                case ("associatedDate"):
                    associatedDate = params[i + 1];
                    break;
            }
        }
    }

}
