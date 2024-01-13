package prototype.xd.scheduler.utilities.misc;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.apache.commons.lang3.mutable.MutableObject;

import prototype.xd.scheduler.entities.SystemCalendar;
import prototype.xd.scheduler.entities.TodoEntry;

public class DialogDataViewModel extends ViewModel {
    
    private final MutableObject<TodoEntry> lastSelectedEntry = new MutableObject<>();
    private final MutableObject<SystemCalendar> lastSelectedCalendar = new MutableObject<>();
    
    private static DialogDataViewModel getInstance(@NonNull Fragment fragment) {
        return new ViewModelProvider(fragment.requireActivity()).get(DialogDataViewModel.class);
    }
    
    @NonNull
    public static TodoEntry getLastSelectedEntry(@NonNull Fragment fragment) {
        return getInstance(fragment).lastSelectedEntry.getValue();
    }
    
    @NonNull
    public SystemCalendar getLastSelectedCalendar(@NonNull Fragment fragment) {
        return getInstance(fragment).lastSelectedCalendar.getValue();
    }
    
    public void selectEntry(@NonNull Fragment fragment, @NonNull TodoEntry entry) {
        getInstance(fragment).lastSelectedEntry.setValue(entry);
    }
    
    public void selectCalendar(@NonNull Fragment fragment, @NonNull SystemCalendar calendar) {
        getInstance(fragment).lastSelectedCalendar.setValue(calendar);
    }
}
