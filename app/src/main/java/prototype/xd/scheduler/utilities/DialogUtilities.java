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

import androidx.annotation.MainThread;
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
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.AddOrEditEntryDialogBinding;
import prototype.xd.scheduler.databinding.AddOrEditGroupDialogBinding;
import prototype.xd.scheduler.databinding.TwoButtonsBinding;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.TodoEntry;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;
import prototype.xd.scheduler.views.DateSelectButton;
import prototype.xd.scheduler.views.SelectableAutoCompleteTextView;
import prototype.xd.scheduler.views.settings.PopupSettingsView;

public final class DialogUtilities {
    
    public static final String NAME = Utilities.class.getSimpleName();
    
    private DialogUtilities() throws InstantiationException {
        throw new InstantiationException(NAME);
    }
    
    /**
     * Display a dialog to add or edit a group
     *
     * @param wrapper                     any context wrapper
     * @param titleStringResource         dialog title
     * @param messageStringResource       dialog message
     * @param cancelButtonStringResource  cancel button text
     * @param confirmButtonStringResource confirmation button text
     * @param defaultEditTextValue        edit field default text value
     * @param confirmationListener        listener to call on dialog confirmation
     * @param deletionListener            listener to call when deletion button is pressed
     */
    @MainThread
    public static void displayGroupAdditionEditDialog(@NonNull ContextWrapper wrapper,
                                                      @StringRes int titleStringResource,
                                                      @StringRes int messageStringResource,
                                                      @StringRes int cancelButtonStringResource,
                                                      @StringRes int confirmButtonStringResource,
                                                      @NonNull String defaultEditTextValue,
                                                      @NonNull Consumer<String> confirmationListener,
                                                      @Nullable Consumer<Dialog> deletionListener) {
        AddOrEditGroupDialogBinding dialogBinding = AddOrEditGroupDialogBinding.inflate(wrapper.getLayoutInflater());
        
        Dialog dialog = buildTemplate(wrapper, titleStringResource, messageStringResource, dialogBinding);
        
        setupEditText(dialogBinding.entryNameEditText, defaultEditTextValue);
        
        if (deletionListener != null) {
            dialogBinding.deleteGroupButton.setOnClickListener(v -> deletionListener.accept(dialog));
        } else {
            dialogBinding.deleteGroupButton.setVisibility(View.GONE);
        }
        
        setupButtons(dialog, dialogBinding.twoButtons,
                cancelButtonStringResource, confirmButtonStringResource,
                v -> callIfInputNotEmpty(dialogBinding.entryNameEditText, text -> {
                    confirmationListener.accept(text);
                    dialog.dismiss();
                }));
    }
    
    /**
     * Display a dialog to add or edit an entry
     *
     * @param wrapper              any context wrapper
     * @param entry                TodoEntry to edit or null
     * @param groupList            a list with all groups
     * @param confirmationListener listener to call on dialog confirmation
     */
    @MainThread
    public static void displayEntryAdditionEditDialog(@NonNull ContextWrapper wrapper,
                                                      @Nullable TodoEntry entry,
                                                      @NonNull List<Group> groupList,
                                                      @NonNull EditEntryConfirmationListener confirmationListener) {
        
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
                v -> callIfInputNotEmpty(dialogBinding.entryNameEditText, text -> {
                    confirmationListener.onClick(text, dialogBinding, selectedIndex[0]);
                    dialog.dismiss();
                }));
    }
    
    /**
     * Sets up edit text field on the dialog
     *
     * @param editText             edit text field
     * @param defaultEditTextValue default (starting) value
     */
    private static void setupEditText(@NonNull EditText editText,
                                      @NonNull String defaultEditTextValue) {
        editText.setText(defaultEditTextValue);
        editText.setOnFocusChangeListener((v, hasFocus) -> editText.postDelayed(() ->
                        ((InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                                .showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT),
                200));
        editText.requestFocus();
    }
    
    /**
     * Sets up buttons on the dialog
     *
     * @param dialog                      target dialog
     * @param buttonContainer             view binding for two buttons
     * @param cancelButtonStringResource  cancel button string resource
     * @param confirmButtonStringResource confirm button string resource
     * @param confirmationListener        listener to call when the dialog is confirmed
     */
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
    
    /**
     * Invokes successCallback when text input field is not empty
     *
     * @param editText        text input field
     * @param successCallback callback to invoke
     */
    private static void callIfInputNotEmpty(@NonNull EditText editText, @NonNull Consumer<String> successCallback) {
        String text = editText.getText() == null ? "" : editText.getText().toString().trim();
        if (text.isEmpty()) {
            editText.setError(editText.getContext().getString(R.string.input_cant_be_empty));
        } else {
            successCallback.accept(text);
        }
    }
    
    /**
     * Build dialog template
     *
     * @param wrapper               context wrapper (context and lifecycle)
     * @param titleStringResource   dialog title string resource
     * @param messageStringResource dialog message string resource
     * @param viewBinding           dialog contents
     * @return dialog with title, message and target view
     */
    @NonNull
    @MainThread
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
    public interface EditEntryConfirmationListener {
        void onClick(@NonNull String text, @NonNull AddOrEditEntryDialogBinding dialogBinding, int selectedIndex);
    }
    
    /**
     * Display a simple message dialog
     *
     * @param wrapper         any context wrapper
     * @param theme           dialog theme
     * @param builder         builder function to set dialog parameters
     * @param dismissListener listener to call on dialog dismiss
     */
    @MainThread
    public static void displayMessageDialog(@NonNull final ContextWrapper wrapper,
                                            @StyleRes int theme,
                                            @NonNull Consumer<MaterialAlertDialogBuilder> builder,
                                            @Nullable DialogInterface.OnDismissListener dismissListener) {
        MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(wrapper.context, theme);
        builder.accept(alertDialogBuilder);
        wrapper.attachDialogToLifecycle(alertDialogBuilder.show(), dismissListener);
    }
    
    /**
     * Display a simple message dialog
     *
     * @param wrapper         any context wrapper
     * @param builderConsumer builder function to set dialog parameters
     */
    @MainThread
    public static void displayMessageDialog(@NonNull final ContextWrapper wrapper,
                                            @NonNull Consumer<MaterialAlertDialogBuilder> builderConsumer) {
        displayMessageDialog(wrapper, R.style.DefaultAlertDialogTheme, builderConsumer, null);
    }
    
    /**
     * Display a simple deletion confirmation dialog
     *
     * @param wrapper              any context wrapper
     * @param confirmationListener listener to call on dialog confirmation
     */
    public static void displayDeletionDialog(@NonNull final ContextWrapper wrapper,
                                             @NonNull DialogInterface.OnClickListener confirmationListener) {
        displayMessageDialog(wrapper, builder -> {
            builder.setTitle(R.string.delete);
            builder.setMessage(R.string.are_you_sure);
            builder.setIcon(R.drawable.ic_delete_50);
            builder.setNegativeButton(R.string.no, null);
            builder.setPositiveButton(R.string.yes, confirmationListener);
        });
    }
    
    /**
     * Shows color picker dialog (for general settings)
     *
     * @param wrapper               context wrapper
     * @param colorSelectedListener listener to call when color is selected
     * @param defaultedInteger      default value
     */
    @MainThread
    public static void displayColorPicker(@NonNull final ContextWrapper wrapper,
                                          @NonNull final ObjIntConsumer<Static.DefaultedInteger> colorSelectedListener,
                                          @NonNull final Static.DefaultedInteger defaultedInteger) {
        displayColorPicker(wrapper, defaultedInteger.get(),
                (dialog, selectedColor, allColors) ->
                        colorSelectedListener.accept(defaultedInteger, selectedColor));
    }
    
    //color dialog for entry settings
    
    /**
     * Shows color picker dialog (for entry settings)
     *
     * @param wrapper             context wrapper
     * @param stateIcon           parameter state icon (from entry settings)
     * @param settingsView        entry settings view
     * @param defaultedInteger    default value
     * @param initialValueFactory function to get default value
     */
    @MainThread
    public static void displayColorPicker(@NonNull final ContextWrapper wrapper,
                                          @NonNull final TextView stateIcon,
                                          @NonNull final PopupSettingsView settingsView,
                                          @NonNull final Static.DefaultedInteger defaultedInteger,
                                          @NonNull final ToIntFunction<Static.DefaultedInteger> initialValueFactory) {
        displayColorPicker(wrapper,
                initialValueFactory.applyAsInt(defaultedInteger), (dialog, selectedColor, allColors) -> {
                    settingsView.notifyParameterChanged(stateIcon, defaultedInteger.key, selectedColor);
                    settingsView.notifyColorChanged(defaultedInteger, selectedColor);
                });
    }
    
    /**
     * Display a dialog to pick a color
     *
     * @param wrapper      any context wrapper
     * @param initialValue initial color
     * @param listener     listener to call on dialog confirmation
     */
    @MainThread
    public static void displayColorPicker(@NonNull final ContextWrapper wrapper,
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
}
