package prototype.xd.scheduler;

import static prototype.xd.scheduler.utilities.DateManager.currentDay;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDay;
import static prototype.xd.scheduler.utilities.DateManager.dateStringFromMsUTC;
import static prototype.xd.scheduler.utilities.DateManager.selectCurrentDay;
import static prototype.xd.scheduler.utilities.DateManager.selectDay;
import static prototype.xd.scheduler.utilities.DialogueUtilities.displayEditTextSpinnerDialogue;
import static prototype.xd.scheduler.utilities.DialogueUtilities.displayErrorMessage;
import static prototype.xd.scheduler.utilities.Keys.ASSOCIATED_DAY;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL_STR;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_FAILED;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;
import static prototype.xd.scheduler.utilities.Keys.WALLPAPER_OBTAIN_FAILED;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;
import java.util.Objects;

import prototype.xd.scheduler.databinding.ContentWrapperBinding;
import prototype.xd.scheduler.databinding.HomeFragmentWrapperBinding;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoListEntry;
import prototype.xd.scheduler.utilities.SArrayMap;
import prototype.xd.scheduler.utilities.TodoListEntryManager;
import prototype.xd.scheduler.views.CalendarView;

public class HomeFragment extends Fragment {
    
    private volatile TodoListEntryManager todoListEntryManager;
    private ContentWrapperBinding contentBnd;
    
    public HomeFragment() {
        super();
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        todoListEntryManager = new TodoListEntryManager(requireContext(), getLifecycle());
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        
        HomeFragmentWrapperBinding wrapperBnd = HomeFragmentWrapperBinding.inflate(inflater, container, false);
        contentBnd = wrapperBnd.contentWrapper;
        
        contentBnd.content.recyclerView.setItemAnimator(null);
        contentBnd.content.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        contentBnd.content.recyclerView.setAdapter(todoListEntryManager.getTodoListViewAdapter());
        
        // init date manager
        selectCurrentDay();
        
        // construct custom calendar view
        CalendarView calendarView = new CalendarView(contentBnd.content.calendar, todoListEntryManager);
        todoListEntryManager.attachCalendarView(calendarView);
        
        // not called on initial startup
        calendarView.setOnDateChangeListener((selectedDate, context) -> {
            selectDay(selectedDate.toEpochDay());
            todoListEntryManager.invalidateEntryList();
            updateStatusText();
        });
        
        // setup month listener, called when a new month is loaded (first month is loaded differently)
        calendarView.setNewMonthBindListener((firstVisibleDay, lastVisibleDay, context) ->
                // load current month entries before displaying the data
                todoListEntryManager.loadEntries(firstVisibleDay, lastVisibleDay)
        );
        
        contentBnd.toCurrentDateButton.setOnClickListener(v -> calendarView.selectDay(currentDay));
        
        DrawerLayout drawerLayout = wrapperBnd.getRoot();
        
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                requireActivity(), drawerLayout, contentBnd.toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                // move all content to the right when drawer opens
                float moveFactor = wrapperBnd.navViewWrapper.getWidth() * slideOffset / 3;
                contentBnd.getRoot().setTranslationX(moveFactor);
                contentBnd.getRoot().setScaleX(1 - slideOffset / 20);
                contentBnd.getRoot().setScaleY(1 - slideOffset / 20);
            }
        };
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(wrapperBnd.navViewWrapper)) {
                    drawerLayout.closeDrawer(wrapperBnd.navViewWrapper);
                } else {
                    // prevent stack overflow
                    setEnabled(false);
                    requireActivity().onBackPressed();
                    setEnabled(true);
                }
            }
        });
        
        contentBnd.fab.setOnClickListener(view1 -> {
            final List<Group> groupList = todoListEntryManager.getGroups();
            displayEditTextSpinnerDialogue(view1.getContext(), getLifecycle(),
                    R.string.add_event_fab, -1, R.string.event_name_input_hint,
                    R.string.cancel, R.string.add, R.string.add_to_global_list, "", groupList, 0,
                    (view2, text, selectedIndex) -> {
                        SArrayMap<String, String> values = new SArrayMap<>();
                        values.put(TEXT_VALUE, text);
                        values.put(ASSOCIATED_DAY, String.valueOf(currentlySelectedDay));
                        values.put(IS_COMPLETED, "false");
                        
                        todoListEntryManager.addEntry(new TodoListEntry(values, // This is fine here as id because a person can't click 2 times in 1 ms
                                groupList.get(selectedIndex).getRawName(), groupList, System.currentTimeMillis()));
                        return true;
                    },
                    (view2, text, selectedIndex) -> {
                        SArrayMap<String, String> values = new SArrayMap<>();
                        values.put(TEXT_VALUE, text);
                        values.put(ASSOCIATED_DAY, DAY_FLAG_GLOBAL_STR);
                        values.put(IS_COMPLETED, "false");
                        
                        todoListEntryManager.addEntry(new TodoListEntry(values,       // This is fine, see note above
                                groupList.get(selectedIndex).getRawName(), groupList, System.currentTimeMillis()));
                        return true;
                    });
        });
        
        wrapperBnd.navView.openSettingsButton.setOnClickListener(v ->
                ((NavHostFragment) Objects.requireNonNull(
                        requireActivity()
                                .getSupportFragmentManager()
                                .findFragmentById(R.id.nav_host_fragment)))
                        .getNavController()
                        .navigate(R.id.action_HomeFragment_to_SettingsFragment));
        
        return wrapperBnd.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // update the ui only after it's fully inflated
        
        // when all entries are loaded, update current month
        todoListEntryManager.onInitFinished(() -> requireActivity().runOnUiThread(() -> {
            updateStatusText();
            // update adapter showing entries
            todoListEntryManager.invalidateEntryList();
            // update calendar updating indicators
            todoListEntryManager.invalidateCalendar();
        }));
        
        if (preferences.getBoolean(SERVICE_FAILED, false)) {
            // display warning if the background service failed
            displayErrorMessage(requireContext(), getLifecycle(),
                    R.string.service_error, R.string.service_error_description, R.drawable.ic_warning,
                    dialog -> preferences.edit().putBoolean(SERVICE_FAILED, false).apply());
        }
        
        if (preferences.getBoolean(WALLPAPER_OBTAIN_FAILED, false)) {
            // display warning if there wan an error getting the wallpaper
            displayErrorMessage(requireContext(), getLifecycle(),
                    R.string.wallpaper_obtain_error, R.string.wallpaper_obtain_error_description, R.drawable.ic_warning,
                    dialog -> preferences.edit().putBoolean(WALLPAPER_OBTAIN_FAILED, false).apply());
        }
    }
    
    public void invalidateAll() {
        todoListEntryManager.invalidateAll();
    }
    
    @Override
    public void onDestroyView() {
        // remove reference to ui element
        todoListEntryManager.detachCalendarView();
        super.onDestroyView();
    }
    
    private void updateStatusText() {
        contentBnd.statusText.setText(getString(R.string.status, dateStringFromMsUTC(currentlySelectedDay * 86400000),
                todoListEntryManager.getCurrentlyVisibleEntriesCount()));
    }
}