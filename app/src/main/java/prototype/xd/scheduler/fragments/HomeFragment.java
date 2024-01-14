package prototype.xd.scheduler.fragments;

import static prototype.xd.scheduler.utilities.DateManager.getCurrentlySelectedDate;
import static prototype.xd.scheduler.utilities.DateManager.getEndOfMonthDayUTC;
import static prototype.xd.scheduler.utilities.DateManager.getStartOfMonthDayUTC;
import static prototype.xd.scheduler.utilities.DateManager.selectDate;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayErrorDialog;
import static prototype.xd.scheduler.utilities.DialogUtilities.displayMessageDialog;
import static prototype.xd.scheduler.utilities.DialogUtilities.getRecyclerviewGap;
import static prototype.xd.scheduler.utilities.Static.GITHUB_FAQ;
import static prototype.xd.scheduler.utilities.Static.GITHUB_ISSUES;
import static prototype.xd.scheduler.utilities.Static.GITHUB_RELEASES;
import static prototype.xd.scheduler.utilities.Static.GITHUB_REPO;
import static prototype.xd.scheduler.utilities.Static.SERVICE_FAILED;
import static prototype.xd.scheduler.utilities.Static.WALLPAPER_OBTAIN_FAILED;
import static prototype.xd.scheduler.utilities.Utilities.setSwitchChangeListener;
import static prototype.xd.scheduler.views.CalendarView.DAYS_ON_ONE_PANEL;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.navigation.NavigationView;

import java.time.LocalDate;

import prototype.xd.scheduler.BuildConfig;
import prototype.xd.scheduler.R;
import prototype.xd.scheduler.adapters.TodoListViewAdapter;
import prototype.xd.scheduler.databinding.ContentWrapperBinding;
import prototype.xd.scheduler.databinding.DebugMenuDialogBinding;
import prototype.xd.scheduler.databinding.HomeFragmentWrapperBinding;
import prototype.xd.scheduler.databinding.NavigationViewBinding;
import prototype.xd.scheduler.fragments.dialogs.AddEditEntryDialogFragment;
import prototype.xd.scheduler.fragments.dialogs.UncompletedEntryListDialogFragment;
import prototype.xd.scheduler.utilities.DateManager;
import prototype.xd.scheduler.utilities.Logger;
import prototype.xd.scheduler.utilities.Static;
import prototype.xd.scheduler.utilities.TodoEntryManager;
import prototype.xd.scheduler.utilities.Utilities;
import prototype.xd.scheduler.views.CalendarView;

public final class HomeFragment extends BaseFragment<HomeFragmentWrapperBinding> { // NOSONAR, this is a fragment
    
    public static final String NAME = HomeFragment.class.getSimpleName();
    
    private TodoEntryManager todoEntryManager;
    private TodoListViewAdapter todoListViewAdapter;
    
    @Override
    @MainThread
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // init date manager
        // select current day
        selectDate(LocalDate.now());
        todoEntryManager = TodoEntryManager.getInstance(wrapper.context);
        todoListViewAdapter = new TodoListViewAdapter(wrapper);
    }
    
    @NonNull
    @Override
    public HomeFragmentWrapperBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return HomeFragmentWrapperBinding.inflate(inflater, container, false);
    }
    
    @Override
    @MainThread
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        
        ContentWrapperBinding contentBnd = binding.contentWrapper;
        NavigationView navViewDrawer = binding.navViewWrapper;
        
        contentBnd.content.recyclerView.setItemAnimator(null);
        contentBnd.content.recyclerView.setLayoutManager(new LinearLayoutManager(wrapper.context));
        contentBnd.content.recyclerView.setAdapter(todoListViewAdapter);
        contentBnd.content.recyclerView.addItemDecoration(getRecyclerviewGap(wrapper, LinearLayout.VERTICAL, R.dimen.list_item_vertical_padding));
        
        DrawerLayout drawerLayout = binding.root;
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                wrapper.activity, drawerLayout, contentBnd.toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                // move all content to the right when drawer opens
                float moveFactor = navViewDrawer.getWidth() * slideOffset / 3;
                contentBnd.root.setTranslationX(moveFactor);
                contentBnd.root.setScaleX(1 - slideOffset / 20);
                contentBnd.root.setScaleY(1 - slideOffset / 20);
            }
        };
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        
        wrapper.activity.getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(navViewDrawer)) {
                    drawerLayout.closeDrawer(navViewDrawer);
                } else {
                    // prevent stack overflow
                    setEnabled(false);
                    wrapper.activity.getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
        
        // construct custom calendar view
        CalendarView calendarView = new CalendarView(contentBnd.content.calendar, todoEntryManager);
        todoEntryManager.attachCalendarView(calendarView, wrapper.lifecycle);
        // when all entries are loaded and ui is created, setup listeners, todoEntryManager also updates the data
        todoEntryManager.onInitFinished(this, el -> {
            setupListeners(contentBnd, binding.navView, calendarView, wrapper.activity);
            binding.contentWrapper.content.progressBar.setVisibility(View.GONE);
        });
    }
    
    private void setupListeners(@NonNull ContentWrapperBinding contentBnd,
                                @NonNull NavigationViewBinding navViewContent,
                                @NonNull CalendarView calendarView,
                                @NonNull FragmentActivity rootActivity) {
        
        // not called on initial startup
        calendarView.setOnDateChangeListener((selectedDate, context) -> {
            selectDate(selectedDate);
            todoEntryManager.notifyEntryListChanged();
        });
        
        todoEntryManager.onListChanged(this, status -> {
            todoListViewAdapter.notifyEntryListChanged();
            // update the status text with entry count
            int events = todoListViewAdapter.getItemCount();
            binding.contentWrapper.toolbar.setTitle(getString(R.string.status, getCurrentlySelectedDate(), events));
            binding.contentWrapper.content.noEventsText.setVisibility(events == 0 ? View.VISIBLE : View.GONE);
        });
        
        // setup month listener, called when a new month is loaded (first month is loaded differently)
        calendarView.setNewMonthBindListener(month ->
                // load current month entries (with overlap of one panel) before displaying the data
                todoEntryManager.loadCalendarEntries(
                        getStartOfMonthDayUTC(month) - DAYS_ON_ONE_PANEL,
                        getEndOfMonthDayUTC(month) + DAYS_ON_ONE_PANEL)
        );
        
        contentBnd.toCurrentDateButton.setOnClickListener(v -> calendarView.selectDate(DateManager.getCurrentDate()));
        
        contentBnd.fab.setOnClickListener(view1 -> AddEditEntryDialogFragment.show(null, wrapper));
        
        navViewContent.sourceCodeClickView.setOnClickListener(v -> Utilities.openUrl(wrapper.context, GITHUB_REPO));
        navViewContent.githubIssueClickView.setOnClickListener(v -> Utilities.openUrl(wrapper.context, GITHUB_ISSUES));
        navViewContent.latestReleaseClickView.setOnClickListener(v -> Utilities.openUrl(wrapper.context, GITHUB_RELEASES));
        navViewContent.faqClickView.setOnClickListener(v -> Utilities.openUrl(wrapper.context, GITHUB_FAQ));
        
        navViewContent.logo.setOnClickListener(v ->
                displayMessageDialog(wrapper, builder -> {
                    builder.setTitle(R.string.debug_menu);
                    builder.setMessage(R.string.debug_menu_description);
                    builder.setIcon(R.drawable.ic_developer_mode_24_primary);
                    
                    DebugMenuDialogBinding bnd = DebugMenuDialogBinding.inflate(wrapper.getLayoutInflater());
                    bnd.shareLogcatView.setOnClickListener(v1 -> Logger.shareLog(wrapper.context));
                    bnd.uncompletedTodosView.setOnClickListener(v1 -> UncompletedEntryListDialogFragment.show(wrapper));
                    
                    if (BuildConfig.DEBUG) {
                        bnd.debugLoggingSwitch.freezeState(true);
                    } else {
                        setSwitchChangeListener(bnd.debugLoggingSwitch, Static.DEBUG_LOGGING, (switchView, isChecked) -> Logger.setDebugEnabled(isChecked));
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
        
    }
    
    // fragment becomes visible
    @Override
    @MainThread
    public void onResume() {
        super.onResume();
        Logger.debug(NAME, "Main screen is now visible");
        
        DateManager.updateTimeZone();
        
        if (SERVICE_FAILED.get()) {
            // display warning if the background service failed
            displayErrorDialog(wrapper,
                    R.string.service_error,
                    R.string.service_error_description,
                    dialog -> SERVICE_FAILED.put(Boolean.FALSE));
        }
        
        if (WALLPAPER_OBTAIN_FAILED.get()) {
            // display warning if there wan an error getting the wallpaper
            displayErrorDialog(wrapper,
                    R.string.wallpaper_obtain_error,
                    R.string.wallpaper_obtain_error_description,
                    dialog -> WALLPAPER_OBTAIN_FAILED.put(Boolean.FALSE));
        }
    }
    
    public void notifyDatasetChanged() {
        todoEntryManager.notifyDatasetChanged();
    }
}
