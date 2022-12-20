package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedTimestampUTC;
import static prototype.xd.scheduler.utilities.Keys.BG_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BORDER_COLOR;
import static prototype.xd.scheduler.utilities.Keys.FONT_COLOR;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewbinding.ViewBinding;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.function.Function;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.AddOrEditEntryDialogBinding;
import prototype.xd.scheduler.databinding.AddOrEditGroupDialogBinding;
import prototype.xd.scheduler.databinding.TwoButtonsBinding;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.views.SelectableAutoCompleteTextView;
import prototype.xd.scheduler.views.settings.PopupSettingsView;

public class DialogueUtilities {
    
    public static void displayConfirmationDialogue(@NonNull Context context,
                                                   @NonNull Lifecycle lifecycle,
                                                   @StringRes int titleStringResource,
                                                   @StringRes int messageStringResource,
                                                   @StringRes int cancelButtonStringResource,
                                                   @StringRes int confirmButtonStringResource,
                                                   @NonNull View.OnClickListener confirmationListener) {
        TwoButtonsBinding twoButtonsBinding = TwoButtonsBinding.inflate(LayoutInflater.from(context));
        Dialog dialog = buildTemplate(context, lifecycle, titleStringResource, messageStringResource, twoButtonsBinding);
        
        setupButtons(dialog, twoButtonsBinding,
                cancelButtonStringResource, confirmButtonStringResource,
                v -> {
                    confirmationListener.onClick(v);
                    dialog.dismiss();
                });
    }
    
    public static void displayGroupAdditionEditDialog(@NonNull Context context,
                                                      @NonNull Lifecycle lifecycle,
                                                      @StringRes int titleStringResource,
                                                      @StringRes int messageStringResource,
                                                      @StringRes int cancelButtonStringResource,
                                                      @StringRes int confirmButtonStringResource,
                                                      @NonNull String defaultEditTextValue,
                                                      @NonNull OnClickListenerWithViewAccess<AddOrEditGroupDialogBinding> confirmationListener,
                                                      @Nullable OnClickListenerWithDialogAccess deletionListener) {
        AddOrEditGroupDialogBinding dialogBinding = AddOrEditGroupDialogBinding.inflate(LayoutInflater.from(context));
        
        Dialog dialog = buildTemplate(context, lifecycle, titleStringResource, messageStringResource, dialogBinding);
        
        setupEditText(dialogBinding.entryNameEditText, defaultEditTextValue);
        
        if (deletionListener != null) {
            dialogBinding.deleteButton.setOnClickListener(v -> deletionListener.onClick(v, dialog));
        } else {
            dialogBinding.deleteButton.setVisibility(View.GONE);
        }
        
        setupButtons(dialog, dialogBinding.twoButtons,
                cancelButtonStringResource, confirmButtonStringResource,
                v -> {
                    String text = checkAndGetInput(dialogBinding.entryNameEditText);
                    if (!text.isEmpty() && confirmationListener.onClick(v, text, dialogBinding, 0)) {
                        dialog.dismiss();
                    }
                });
    }
    
    public static void displayEntryAdditionEditDialog(@NonNull FragmentManager fragmentManager,
                                                      @NonNull Context context,
                                                      @NonNull Lifecycle lifecycle,
                                                      @StringRes int titleStringResource,
                                                      @StringRes int confirmButtonStringResource,
                                                      @NonNull String defaultEditTextValue,
                                                      @NonNull List<Group> groups,
                                                      int defaultIndex,
                                                      @NonNull OnClickListenerWithViewAccess<AddOrEditEntryDialogBinding> confirmationListener) {
        
        AddOrEditEntryDialogBinding dialogBinding = AddOrEditEntryDialogBinding.inflate(LayoutInflater.from(context));
        
        Dialog dialog = buildTemplate(context, lifecycle, titleStringResource, -1, dialogBinding);
        setupEditText(dialogBinding.entryNameEditText, defaultEditTextValue);
        
        String[] items = Group.groupListToNames(groups, context);
        SelectableAutoCompleteTextView spinner = dialogBinding.groupSpinner;
        spinner.setSimpleItems(items);
        
        final int[] selectedIndex = {defaultIndex};
        
        spinner.setSelectedItem(defaultIndex);
        spinner.setOnItemClickListener((parent, view, position, id) -> selectedIndex[0] = position);
        
        dialogBinding.dayFromButton.setup(fragmentManager, currentlySelectedTimestampUTC);
        dialogBinding.dayToButton.setup(fragmentManager, currentlySelectedTimestampUTC);
        
        setupButtons(dialog, dialogBinding.twoButtons,
                R.string.cancel, confirmButtonStringResource,
                v -> {
                    String text = checkAndGetInput(dialogBinding.entryNameEditText);
                    if (!text.isEmpty() && confirmationListener.onClick(v, text, dialogBinding, selectedIndex[0])) {
                        dialog.dismiss();
                    }
                });
    }
    
    private static void setupEditText(@NonNull EditText editText,
                                      @NonNull String defaultEditTextValue) {
        editText.setText(defaultEditTextValue);
        editText.setOnFocusChangeListener((v, hasFocus) -> editText.postDelayed(() -> {
            InputMethodManager inputMethodManager = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }, 200));
        editText.requestFocus();
    }
    
    private static void setupButtons(@NonNull Dialog dialog,
                                     @NonNull TwoButtonsBinding buttonContainer,
                                     @StringRes int cancelButtonStringResource,
                                     @StringRes int confirmButtonStringResource,
                                     @NonNull View.OnClickListener confirmationListener) {
        
        Button confirmButton = buttonContainer.confirmButton;
        Button cancelButton = buttonContainer.cancelButton;
        
        cancelButton.setText(cancelButtonStringResource);
        confirmButton.setText(confirmButtonStringResource);
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        confirmButton.setOnClickListener(confirmationListener);
    }
    
    private static String checkAndGetInput(@NonNull EditText editText) {
        String text = editText.getText() == null ? "" : editText.getText().toString().trim();
        if (text.isEmpty()) {
            editText.setError(editText.getContext().getString(R.string.input_cant_be_empty));
        }
        return text;
    }
    
    private static Dialog buildTemplate(@NonNull final Context context,
                                        @NonNull final Lifecycle lifecycle,
                                        @StringRes int titleStringResource,
                                        @StringRes int messageStringResource,
                                        @NonNull ViewBinding viewBinding) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(titleStringResource);
        if (messageStringResource != -1) {
            builder.setMessage(messageStringResource);
        }
        builder.setView(viewBinding.getRoot());
        return attachDialogToLifecycle(builder.show(), lifecycle, null);
    }
    
    private static Dialog attachDialogToLifecycle(@NonNull final Dialog dialog,
                                                  @NonNull final Lifecycle lifecycle,
                                                  @Nullable DialogInterface.OnDismissListener dismissListener) {
        // make sure the dialog is dismissed on activity destroy
        DialogDismissLifecycleObserver dismissLifecycleObserver = new DialogDismissLifecycleObserver(dialog);
        lifecycle.addObserver(dismissLifecycleObserver);
        // remove the observer as soon as the dialog in dismissed
        dialog.setOnDismissListener(dialog1 -> {
            if (dismissListener != null) {
                dismissListener.onDismiss(dialog1);
            }
            lifecycle.removeObserver(dismissLifecycleObserver);
        });
        return dialog;
    }
    
    @FunctionalInterface
    public interface OnClickListenerWithViewAccess<T extends ViewBinding> {
        boolean onClick(View view, String text, T dialogBinding, int selectedIndex);
    }
    
    @FunctionalInterface
    public interface OnClickListenerWithDialogAccess {
        void onClick(View view, Dialog dialog);
    }
    
    public static void displayMessageDialog(@NonNull final Context context,
                                            @NonNull final Lifecycle lifecycle,
                                            @StringRes int titleStringResource,
                                            @StringRes int messageStringResource,
                                            @DrawableRes int iconResource,
                                            @StyleRes int theme,
                                            DialogInterface.OnDismissListener dismissListener) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, theme);
        builder.setTitle(titleStringResource);
        builder.setMessage(messageStringResource);
        builder.setIcon(iconResource);
        builder.setPositiveButton(R.string.close, null);
        attachDialogToLifecycle(builder.show(), lifecycle, dismissListener);
    }
    
    //color dialogue for general settings
    public static void invokeColorDialogue(@NonNull final Context context,
                                           @NonNull final Lifecycle lifecycle,
                                           final ColorPickerKeyedClickListener clickListener,
                                           final String key,
                                           final int defaultValue) {
        invokeColorDialogue(context, lifecycle, preferences.getInt(key, defaultValue),
                (dialogInterface, lastSelectedColor, allColors) ->
                        clickListener.onClick(dialogInterface, lastSelectedColor, key, allColors));
    }
    
    //color dialogue for entry settings
    public static void invokeColorDialogue(@NonNull final Context context,
                                           @NonNull final Lifecycle lifecycle,
                                           final TextView stateIcon,
                                           final PopupSettingsView settingsView,
                                           final String parameterKey,
                                           final Function<String, Integer> initialValueFactory) {
        invokeColorDialogue(context, lifecycle,
                initialValueFactory.apply(parameterKey), (dialog, selectedColor, allColors) -> {
                    settingsView.notifyParameterChanged(stateIcon, parameterKey, selectedColor);
                    switch (parameterKey) {
                        case FONT_COLOR:
                            settingsView.updatePreviewFont(selectedColor);
                            break;
                        case BORDER_COLOR:
                            settingsView.updatePreviewBorder(selectedColor);
                            break;
                        case BG_COLOR:
                        default:
                            settingsView.updatePreviewBg(selectedColor);
                            break;
                    }
                });
    }
    
    public static void invokeColorDialogue(@NonNull final Context context,
                                           @NonNull Lifecycle lifecycle,
                                           final int initialValue,
                                           @NonNull ColorPickerClickListener listener) {
        attachDialogToLifecycle(ColorPickerDialogBuilder
                .with(context)
                .setTitle(context.getString(R.string.choose_color))
                .initialColor(initialValue)
                .showAlphaSlider(false)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setPositiveButton(context.getString(R.string.apply), listener)
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                }).build(), lifecycle, null).show();
    }
    
    @FunctionalInterface
    public interface ColorPickerKeyedClickListener {
        void onClick(DialogInterface dialogInterface, int lastSelectedColor, String colorKey, Integer[] allColors);
    }
    
}
