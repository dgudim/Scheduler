package prototype.xd.scheduler;

import static prototype.xd.scheduler.utilities.DateManager.checkIfTimeSettingsChanged;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedTimestampUTC;
import static prototype.xd.scheduler.utilities.DateManager.dateStringUTCFromMsUTC;
import static prototype.xd.scheduler.utilities.DateManager.getEndOfMonthDayUTC;
import static prototype.xd.scheduler.utilities.DateManager.getStartOfMonthDayUTC;
import static prototype.xd.scheduler.utilities.DateManager.selectDate;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayEntryAdditionEditDialog;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayMessageDialog;
import static prototype.xd.scheduler.utilities.Keys.DAY_FLAG_GLOBAL_STR;
import static prototype.xd.scheduler.utilities.Keys.END_DAY_UTC;
import static prototype.xd.scheduler.utilities.Keys.GITHUB_FAQ;
import static prototype.xd.scheduler.utilities.Keys.GITHUB_ISSUES;
import static prototype.xd.scheduler.utilities.Keys.GITHUB_RELEASES;
import static prototype.xd.scheduler.utilities.Keys.GITHUB_REPO;
import static prototype.xd.scheduler.utilities.Keys.IS_COMPLETED;
import static prototype.xd.scheduler.utilities.Keys.SERVICE_FAILED;
import static prototype.xd.scheduler.utilities.Keys.START_DAY_UTC;
import static prototype.xd.scheduler.utilities.Keys.TEXT_VALUE;
import static prototype.xd.scheduler.utilities.Keys.WALLPAPER_OBTAIN_FAILED;
import static prototype.xd.scheduler.utilities.Utilities.setSwitchChangeListener;
import static prototype.xd.scheduler.views.CalendarView.DAYS_ON_ONE_PANEL;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.navigation.NavigationView;

import java.time.LocalDate;
import java.util.List;

import prototype.xd.scheduler.databinding.ContentWrapperBinding;
import prototype.xd.scheduler.databinding.DebugMenuDialogBinding;
import prototype.xd.scheduler.databinding.HomeFragmentWrapperBinding;
import prototype.xd.scheduler.databinding.NavigationViewBinding;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.utilities.ContextWrapper;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Keys;
import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.SArrayMap;
import prototype.xd.scheduler.utilities.TodoEntryManager;
import prototype.xd.scheduler.utilities.Utilities;
import prototype.xd.scheduler.views.CalendarView;

public final class HomeFragment extends Fragment { // NOSONAR, this is a fragment
    
    public static final String NAME = HomeFragment.class.getSimpleName();
    
    private TodoEntryManager todoEntryManager;
    private ContentWrapperBinding contentBnd;
    private ContextWrapper wrapper;
    
    @Override
    @MainThread
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // init date manager
        // select current day
        selectDate(LocalDate.now());
        wrapper = ContextWrapper.from(this);
        todoEntryManager = new TodoEntryManager(wrapper);
    }
    
    @Override
    @MainThread
    @NonNull
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentActivity rootActivity = requireActivity();
        
        HomeFragmentWrapperBinding wrapperBnd = HomeFragmentWrapperBinding.inflate(inflater, container, false);
        contentBnd = wrapperBnd.contentWrapper;
        NavigationView navViewDrawer = wrapperBnd.navViewWrapper;
        NavigationViewBinding navViewContent = wrapperBnd.navView;
        
        contentBnd.content.recyclerView.setItemAnimator(null);
        contentBnd.content.recyclerView.setLayoutManager(new LinearLayoutManager(wrapper.context));
        contentBnd.content.recyclerView.setAdapter(todoEntryManager.getTodoListViewAdapter());
        
        // construct custom calendar view
        CalendarView calendarView = new CalendarView(contentBnd.content.calendar, todoEntryManager);
        todoEntryManager.attachCalendarView(calendarView);
        
        // not called on initial startup
        calendarView.setOnDateChangeListener((selectedDate, context) -> {
            selectDate(selectedDate);
            todoEntryManager.notifyEntryListChanged();
            updateStatusText();
        });
        
        // setup month listener, called when a new month is loaded (first month is loaded differently)
        calendarView.setNewMonthBindListener(month ->
                // load current month entries (with overlap of one panel) before displaying the data
                todoEntryManager.loadCalendarEntries(
                        getStartOfMonthDayUTC(month) - DAYS_ON_ONE_PANEL,
                        getEndOfMonthDayUTC(month) + DAYS_ON_ONE_PANEL)
        );
        
        contentBnd.toCurrentDateButton.setOnClickListener(v -> calendarView.selectDate(DateManager.getCurrentDate()));
        
        DrawerLayout drawerLayout = wrapperBnd.getRoot();
        
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                rootActivity, drawerLayout, contentBnd.toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                // move all content to the right when drawer opens
                float moveFactor = navViewDrawer.getWidth() * slideOffset / 3;
                contentBnd.getRoot().setTranslationX(moveFactor);
                contentBnd.getRoot().setScaleX(1 - slideOffset / 20);
                contentBnd.getRoot().setScaleY(1 - slideOffset / 20);
            }
        };
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        
        rootActivity.getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(navViewDrawer)) {
                    drawerLayout.closeDrawer(navViewDrawer);
                } else {
                    // prevent stack overflow
                    setEnabled(false);
                    rootActivity.onBackPressed();
                    setEnabled(true);
                }
            }
        });
        
        contentBnd.fab.setOnClickListener(view1 -> {
            final List<Group> groupList = todoEntryManager.getGroups();
            displayEntryAdditionEditDialog(wrapper,
                    null, groupList,
                    (text, dialogBinding, selectedIndex) -> {
                        SArrayMap<String, String> values = new SArrayMap<>();
                        values.put(TEXT_VALUE, text);
                        boolean isGlobal = dialogBinding.globalEntrySwitch.isChecked();
                        values.put(START_DAY_UTC, isGlobal ? DAY_FLAG_GLOBAL_STR :
                                dialogBinding.dayFromButton.getSelectedDayUTCStr());
                        values.put(END_DAY_UTC, isGlobal ? DAY_FLAG_GLOBAL_STR :
                                dialogBinding.dayToButton.getSelectedDayUTCStr());
                        values.put(IS_COMPLETED, Boolean.toString(false));
                        
                        todoEntryManager.addEntry(new TodoEntry(values, groupList.get(selectedIndex), System.currentTimeMillis()));
                    });
        });
        
        navViewContent.sourceCodeClickView.setOnClickListener(v -> Utilities.openUrl(wrapper.context, GITHUB_REPO));
        navViewContent.githubIssueClickView.setOnClickListener(v -> Utilities.openUrl(wrapper.context, GITHUB_ISSUES));
        navViewContent.latestReleaseClickView.setOnClickListener(v -> Utilities.openUrl(wrapper.context, GITHUB_RELEASES));
        navViewContent.faqClickView.setOnClickListener(v -> Utilities.openUrl(wrapper.context, GITHUB_FAQ));
        
        navViewContent.logo.setOnClickListener(v ->
                displayMessageDialog(wrapper, builder -> {
                    builder.setTitle(R.string.debug_menu);
                    builder.setMessage(R.string.debug_menu_description);
                    builder.setIcon(R.drawable.ic_developer_mode_24_primary);
                    
                    DebugMenuDialogBinding bnd = DebugMenuDialogBinding.inflate(LayoutInflater.from(wrapper.context));
                    bnd.shareLogcatView.setOnClickListener(v1 -> Logger.shareLog(wrapper.context));
                    
                    if (BuildConfig.DEBUG) {
                        bnd.debugLoggingSwitch.setCheckedSilent(true);
                        bnd.debugLoggingSwitch.setClickable(false);
                        bnd.debugLoggingSwitch.setAlpha(0.5F);
                    } else {
                        setSwitchChangeListener(bnd.debugLoggingSwitch, Keys.DEBUG_LOGGING, (switchView, isChecked) -> Logger.setDebugEnabled(isChecked));
                    }
                    builder.setView(bnd.root);
                    
                    builder.setPositiveButton(R.string.close, null);
                })
        );
        
        navViewContent.globalSettingsClickView.setOnClickListener(v ->
                Utilities.navigateToFragment(rootActivity, R.id.action_HomeFragment_to_GlobalSettingsFragment));
        
        navViewContent.calendarSettingsClickView.setOnClickListener(v ->
                Utilities.navigateToFragment(rootActivity, R.id.action_HomeFragment_to_CalendarSettingsFragment));
        
        navViewContent.sortingSettingsClickView.setOnClickListener(v ->
                Utilities.navigateToFragment(rootActivity, R.id.action_HomeFragment_to_SortingSettingsFragment));
        
        
        return wrapperBnd.getRoot();
    }
    
    // fragment becomes visible
    @Override
    @MainThread
    public void onResume() {
        super.onResume();
        Logger.debug(NAME, "Main screen is now visible");
        // update the ui only after it's fully inflated
        
        // when all entries are loaded, update current month
        todoEntryManager.onInitFinished(() -> requireActivity().runOnUiThread(() -> {
            if (checkIfTimeSettingsChanged()) {
                todoEntryManager.notifyDatasetChanged(true);
            } else {
                // update adapter showing entries
                todoEntryManager.notifyEntryListChanged();
                // update calendar updating indicators
                todoEntryManager.notifyCurrentMonthChanged();
            }
            // finally, update the status text with entry count
            updateStatusText();
        }));
        
        if (SERVICE_FAILED.get()) {
            // display warning if the background service failed
            displayMessageDialog(wrapper, R.style.ErrorAlertDialogTheme, builder -> {
                builder.setTitle(R.string.service_error);
                builder.setMessage(R.string.service_error_description);
                builder.setIcon(R.drawable.ic_warning_24_onerrorcontainer);
                builder.setPositiveButton(R.string.close, null);
            }, dialog -> SERVICE_FAILED.put(Boolean.FALSE));
        }
        
        if (WALLPAPER_OBTAIN_FAILED.get()) {
            // display warning if there wan an error getting the wallpaper
            displayMessageDialog(wrapper, R.style.ErrorAlertDialogTheme, builder -> {
                builder.setTitle(R.string.wallpaper_obtain_error);
                builder.setMessage(R.string.wallpaper_obtain_error_description);
                builder.setIcon(R.drawable.ic_warning_24_onerrorcontainer);
                builder.setPositiveButton(R.string.close, null);
            }, dialog -> WALLPAPER_OBTAIN_FAILED.put(Boolean.FALSE));
        }
    }
    
    public void notifySettingsChanged() {
        todoEntryManager.notifyDatasetChanged(false);
    }
    
    @Override
    @MainThread
    public void onDestroyView() {
        // remove reference to ui element
        todoEntryManager.detachCalendarView();
        super.onDestroyView();
    }
    
    private void updateStatusText() {
        contentBnd.statusText.setText(
                getString(R.string.status, dateStringUTCFromMsUTC(currentlySelectedTimestampUTC),
                        todoEntryManager.getCurrentlyVisibleEntriesCount()));
    }
}
