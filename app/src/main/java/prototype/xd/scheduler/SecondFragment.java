package prototype.xd.scheduler;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.io.File;

import prototype.xd.scheduler.utilities.LockScreenBitmapDrawer;

import static prototype.xd.scheduler.MainActivity.preferences;
import static prototype.xd.scheduler.utilities.BitmapUtilities.createSolidColorCircle;
import static prototype.xd.scheduler.utilities.DateManager.availableDays;
import static prototype.xd.scheduler.utilities.Utilities.addSeekBarChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.addSwitchChangeListener;
import static prototype.xd.scheduler.utilities.Utilities.invokeColorDialogue;
import static prototype.xd.scheduler.utilities.Utilities.rootDir;

@SuppressWarnings({"ResultOfMethodCallIgnored", "ApplySharedPref"})
public class SecondFragment extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_second, container, false);
    }

    public void onViewCreated(@NonNull final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.resetBgButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Удалить все сохраненные фоны?");

                builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new File(rootDir, "bg.png").delete();
                        for (int i = 0; i < 7; i++) {
                            new File(rootDir, availableDays[i] + ".png").delete();
                        }
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

        ImageView todayBgColor = view.findViewById(R.id.defaultColor);
        ImageView todayBevelColor = view.findViewById(R.id.defaultBevelColor);
        ImageView yesterdayBgColor = view.findViewById(R.id.defaultYesterdayColor);
        ImageView yesterdayBevelColor = view.findViewById(R.id.defaultYesterdayBevelColor);

        ImageView globalBgColor = view.findViewById(R.id.globalColor);
        ImageView globalBevelColor = view.findViewById(R.id.globalBevelColor);

        todayBgColor.setImageBitmap(createSolidColorCircle(preferences.getInt("todayBgColor", 0xFFFFFFFF)));
        yesterdayBgColor.setImageBitmap(createSolidColorCircle(preferences.getInt("yesterdayBgColor", 0xFFFFCCCC)));
        todayBevelColor.setImageBitmap(createSolidColorCircle(preferences.getInt("todayBevelColor", 0xFF888888)));
        yesterdayBevelColor.setImageBitmap(createSolidColorCircle(preferences.getInt("yesterdayBevelColor", 0xFFFF8888)));

        globalBgColor.setImageBitmap(createSolidColorCircle(preferences.getInt("globalBgColor", 0xFFCCFFCC)));
        globalBevelColor.setImageBitmap(createSolidColorCircle(preferences.getInt("globalBevelColor", 0xFF88FF88)));

        todayBgColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invokeColorDialogue("todayBgColor", (ImageView) v, 0xFFFFFFFF, getContext());
            }
        });

        yesterdayBgColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invokeColorDialogue("yesterdayBgColor", (ImageView) v, 0xFFFFCCCC, getContext());
            }
        });

        todayBevelColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invokeColorDialogue("todayBevelColor", (ImageView) v, 0xFF888888, getContext());
            }
        });

        yesterdayBevelColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invokeColorDialogue("yesterdayBevelColor", (ImageView) v, 0xFFFF8888, getContext());
            }
        });

        globalBgColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invokeColorDialogue("globalBgColor", (ImageView) v, 0xFFCCFFCC, getContext());
            }
        });

        globalBevelColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invokeColorDialogue("globalBevelColor", (ImageView) v, 0xFF88FF88, getContext());
            }
        });

        addSwitchChangeListener((Switch) view.findViewById(R.id.bgUpdateSwitch), "bgUpdate", true, new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    getContext().startService(new Intent(getActivity(), BackgroundUpdateService.class));
                } else {
                    getContext().stopService(new Intent(getActivity(), BackgroundUpdateService.class));
                }
            }
        });

        addSeekBarChangeListener(
                (TextView) (view.findViewById(R.id.textSizeDescripton)),
                (SeekBar) (view.findViewById(R.id.fontSizeBar)),
                "fontSize", 21, R.string.settings_font_size, this);

        addSeekBarChangeListener(
                (TextView) (view.findViewById(R.id.defaultBevelThickness)),
                (SeekBar) (view.findViewById(R.id.defaultBevelThicknessBar)),
                "defaultBevelThickness", 5, R.string.settings_default_bevel_thickness, this);

        addSeekBarChangeListener(
                (TextView) (view.findViewById(R.id.yesterdayBevelThickness)),
                (SeekBar) (view.findViewById(R.id.yesterdaytBevelThicknessBar)),
                "yesterdayBevelThickness", 5, R.string.settings_yesterday_bevel_thickness, this);

        addSeekBarChangeListener(
                (TextView) (view.findViewById(R.id.globalBevelThickness)),
                (SeekBar) (view.findViewById(R.id.globalBevelThicknessBar)),
                "globalBevelThickness", 5, R.string.settings_global_bevel_thickness, this);

        addSeekBarChangeListener(
                (TextView) (view.findViewById(R.id.adaptive_color_balance_description)),
                (SeekBar) (view.findViewById(R.id.adaptive_color_balance_bar)),
                "adaptiveColorBalance", 500, R.string.settings_adaptive_color_balance, this);

        addSwitchChangeListener((Switch) view.findViewById(R.id.yesterdayItemsLockSwitch), "yesterdayItemsLock", false, null);
        addSwitchChangeListener((Switch) view.findViewById(R.id.yesterdayItemsListSwitch), "yesterdayItemsList", false, null);
        addSwitchChangeListener((Switch) view.findViewById(R.id.startupUpdateSwitch), "startupUpdate", false, null);
        addSwitchChangeListener((Switch) view.findViewById(R.id.completedTasksSwitch), "completedTasks", false, null);
        addSwitchChangeListener((Switch) view.findViewById(R.id.yesterdayTasksSwitch), "yesterdayTasks", true, null);
        addSwitchChangeListener((Switch) view.findViewById(R.id.yesterdayTasksLockSwitch), "yesterdayTasksLock", true, null);
        addSwitchChangeListener((Switch) view.findViewById(R.id.globalItemsSwitch), "globalTasksLock", true, null);
        addSwitchChangeListener((Switch) view.findViewById(R.id.itemWidthSwitch), "forceMaxRWidthOnLock", false, null);
        addSwitchChangeListener((Switch) view.findViewById(R.id.adaptiveColorSwitch), "adaptiveColorEnabled", false, null);
        addSwitchChangeListener((Switch) view.findViewById(R.id.adaptiveBackgroundSwitch), "adaptiveBackgroundEnabled", true, null);

        view.findViewById(R.id.resetSettingsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Сбросить настройки?");

                builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        preferences.edit().clear().commit();
                        preferences.edit().putBoolean("settingsModified", true).apply();
                        SecondFragment.this.onViewCreated(view, savedInstanceState);
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

    }
}