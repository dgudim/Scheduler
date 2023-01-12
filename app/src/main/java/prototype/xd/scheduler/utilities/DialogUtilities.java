package prototype.xd.scheduler.utilities;

import static java.lang.Math.max;
import static prototype.xd.scheduler.entities.Group.groupIndexInList;
import static prototype.xd.scheduler.utilities.DateManager.currentlySelectedDayUTC;
import static prototype.xd.scheduler.utilities.Utilities.fancyHideUnhideView;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.views.DateSelectButton;
import prototype.xd.scheduler.views.SelectableAutoCompleteTextView;
import prototype.xd.scheduler.views.settings.PopupSettingsView;

public class DialogUtilities {
    
    public static void displayConfirmationDialogue(@NonNull ContextWrapper wrapper,
                                                   @StringRes int titleStringResource,
                                                   @StringRes int messageStringResource,
                                                   @StringRes int cancelButtonStringResource,
                                                   @StringRes int confirmButtonStringResource,
                                                   @NonNull View.OnClickListener confirmationListener) {
        TwoButtonsBinding twoButtonsBinding = TwoButtonsBinding.inflate(wrapper.getLayoutInflater());
        Dialog dialog = buildTemplate(wrapper, titleStringResource, messageStringResource, twoButtonsBinding);
        
        setupButtons(dialog, twoButtonsBinding,
                cancelButtonStringResource, confirmButtonStringResource,
                v -> {
                    confirmationListener.onClick(v);
                    dialog.dismiss();
                });
    }
    
    public static void displayGroupAdditionEditDialog(@NonNull ContextWrapper wrapper,
                                                      @StringRes int titleStringResource,
                                                      @StringRes int messageStringResource,
                                                      @StringRes int cancelButtonStringResource,
                                                      @StringRes int confirmButtonStringResource,
                                                      @NonNull String defaultEditTextValue,
                                                      @NonNull OnClickListenerWithViewAccess<AddOrEditGroupDialogBinding> confirmationListener,
                                                      @Nullable OnClickListenerWithDialogAccess deletionListener) {
        AddOrEditGroupDialogBinding dialogBinding = AddOrEditGroupDialogBinding.inflate(wrapper.getLayoutInflater());
        
        Dialog dialog = buildTemplate(wrapper, titleStringResource, messageStringResource, dialogBinding);
        
        setupEditText(dialogBinding.entryNameEditText, defaultEditTextValue);
        
        if (deletionListener != null) {
            dialogBinding.deleteGroupButton.setOnClickListener(v -> deletionListener.onClick(v, dialog));
        } else {
            dialogBinding.deleteGroupButton.setVisibility(View.GONE);
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
    
    public static void displayEntryAdditionEditDialog(@NonNull ContextWrapper wrapper,
                                                      @Nullable TodoEntry entry,
                                                      @NonNull List<Group> groupList,
                                                      @NonNull OnClickListenerWithViewAccess<AddOrEditEntryDialogBinding> confirmationListener) {
        
        AddOrEditEntryDialogBinding dialogBinding = AddOrEditEntryDialogBinding.inflate(wrapper.getLayoutInflater());
        
        Dialog dialog = buildTemplate(wrapper,
                entry == null ? R.string.add_event_fab : R.string.edit_event,
                -1, dialogBinding);
        setupEditText(dialogBinding.entryNameEditText, entry == null ? "" : entry.getRawTextValue());
        
        String[] items = Group.groupListToNames(groupList, wrapper);
        SelectableAutoCompleteTextView groupSpinner = dialogBinding.groupSpinner;
        groupSpinner.setSimpleItems(items);
        
        int initialGroupIndex = entry == null ? 0 : max(groupIndexInList(groupList, entry.getRawGroupName()), 0);
        
        final int[] selectedIndex = {initialGroupIndex};
        
        groupSpinner.setSelectedItem(initialGroupIndex);
        groupSpinner.setOnItemClickListener((parent, view, position, id) -> selectedIndex[0] = position);
        
        dialogBinding.dayFromButton.setup(wrapper.fragmentManager, entry == null ? currentlySelectedDayUTC : entry.startDayLocal.get());
        dialogBinding.dayToButton.setup(wrapper.fragmentManager, entry == null ? currentlySelectedDayUTC : entry.endDayLocal.get());
        
        // for date validation
        dialogBinding.dayFromButton.setRole(DateSelectButton.Role.START_DAY, dialogBinding.dayToButton);
        dialogBinding.dayToButton.setRole(DateSelectButton.Role.END_DAY, dialogBinding.dayFromButton);
        
        dialogBinding.globalEntrySwitch.setOnCheckedChangeListener((buttonView, isChecked, fromUser) -> {
            fancyHideUnhideView(dialogBinding.dayFromButton, !isChecked, fromUser);
            fancyHideUnhideView(dialogBinding.dayToButton, !isChecked, fromUser);
            fancyHideUnhideView(dialogBinding.dateFromToArrow, !isChecked, fromUser);
            fancyHideUnhideView(dialogBinding.divider2, !isChecked, fromUser);
        });
        dialogBinding.globalEntrySwitch.setChecked(entry != null && entry.isGlobal());
        
        setupButtons(dialog, dialogBinding.twoButtons,
                R.string.cancel, entry == null ? R.string.add : R.string.save,
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
    
    private static Dialog buildTemplate(@NonNull final ContextWrapper wrapper,
                                        @StringRes int titleStringResource,
                                        @StringRes int messageStringResource,
                                        @NonNull ViewBinding viewBinding) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper.context);
        builder.setTitle(titleStringResource);
        if (messageStringResource != -1) {
            builder.setMessage(messageStringResource);
        }
        builder.setView(viewBinding.getRoot());
        return wrapper.attachDialogToLifecycle(builder.show(), null);
    }
    
    @FunctionalInterface
    public interface OnClickListenerWithViewAccess<T extends ViewBinding> {
        boolean onClick(View view, String text, T dialogBinding, int selectedIndex);
    }
    
    @FunctionalInterface
    public interface OnClickListenerWithDialogAccess {
        void onClick(View view, Dialog dialog);
    }
    
    public static void displayMessageDialog(@NonNull final ContextWrapper wrapper,
                                            @StringRes int titleStringResource,
                                            @StringRes int messageStringResource,
                                            @DrawableRes int iconResource,
                                            @StringRes int positiveButton,
                                            @StyleRes int theme,
                                            DialogInterface.OnDismissListener dismissListener) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(wrapper.context, theme);
        builder.setTitle(titleStringResource);
        builder.setMessage(messageStringResource);
        builder.setIcon(iconResource);
        builder.setPositiveButton(positiveButton, null);
        wrapper.attachDialogToLifecycle(builder.show(), dismissListener);
    }
    
    //color dialog for general settings
    public static void invokeColorDialog(@NonNull final ContextWrapper wrapper,
                                         final ColorPickerColorSelectionListener clickListener,
                                         final Keys.DefaultedInteger defaultedInteger) {
        invokeColorDialog(wrapper, defaultedInteger.get(),
                (dialog, selectedColor, allColors) ->
                        clickListener.onClick(defaultedInteger, selectedColor));
    }
    
    //color dialog for entry settings
    public static void invokeColorDialog(@NonNull final ContextWrapper wrapper,
                                         final TextView stateIcon,
                                         final PopupSettingsView settingsView,
                                         final Keys.DefaultedInteger defaultedInteger,
                                         final Function<Keys.DefaultedInteger, Integer> initialValueFactory) {
        invokeColorDialog(wrapper,
                initialValueFactory.apply(defaultedInteger), (dialog, selectedColor, allColors) -> {
                    settingsView.notifyParameterChanged(stateIcon, defaultedInteger.key, selectedColor);
                    settingsView.notifyColorChanged(defaultedInteger, selectedColor);
                });
    }
    
    public static void invokeColorDialog(@NonNull final ContextWrapper wrapper,
                                         final int initialValue,
                                         @NonNull ColorPickerClickListener listener) {
        
        wrapper.attachDialogToLifecycle(ColorPickerDialogBuilder
                .with(wrapper.context, R.style.ColorPickerDialogStyle)
                .setTitle(wrapper.getString(R.string.choose_color))
                .initialColor(initialValue)
                .showAlphaSlider(false)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setPositiveButton(wrapper.getString(R.string.apply), listener)
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                }).build(), null).show();
    }
    
    @FunctionalInterface
    public interface ColorPickerColorSelectionListener {
        void onClick(Keys.DefaultedInteger defaultedInteger, int selectedColor);
    }
    
}
