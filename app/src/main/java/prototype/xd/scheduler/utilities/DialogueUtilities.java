package prototype.xd.scheduler.utilities;

import static prototype.xd.scheduler.utilities.Keys.BG_COLOR;
import static prototype.xd.scheduler.utilities.Keys.BORDER_COLOR;
import static prototype.xd.scheduler.utilities.Keys.FONT_COLOR;
import static prototype.xd.scheduler.utilities.PreferencesStore.preferences;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.lifecycle.Lifecycle;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.function.Function;

import prototype.xd.scheduler.R;
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
                                                   @StringRes int secondaryButtonStringResource,
                                                   @NonNull View.OnClickListener confirmationListener,
                                                   @Nullable View.OnClickListener secondaryConfirmationListener) {
        Dialog dialog = buildTemplate(context, lifecycle, titleStringResource, messageStringResource, R.layout.three_buttons);
        
        setupButtons(dialog,
                cancelButtonStringResource, confirmButtonStringResource, secondaryButtonStringResource,
                v -> {
                    confirmationListener.onClick(v);
                    dialog.dismiss();
                }, secondaryConfirmationListener == null ? null : v -> {
                    secondaryConfirmationListener.onClick(v);
                    dialog.dismiss();
                });
    }
    
    public static void displayConfirmationDialogue(Context context,
                                                   @NonNull Lifecycle lifecycle,
                                                   @StringRes int titleStringResource,
                                                   @StringRes int messageStringResource,
                                                   @StringRes int cancelButtonStringResource,
                                                   @StringRes int confirmButtonStringResource,
                                                   @NonNull View.OnClickListener confirmationListener) {
        displayConfirmationDialogue(context, lifecycle, titleStringResource, messageStringResource,
                cancelButtonStringResource, confirmButtonStringResource, -1,
                confirmationListener, null);
    }
    
    public static void displayConfirmationDialogue(Context context,
                                                   @NonNull Lifecycle lifecycle,
                                                   @StringRes int titleStringResource,
                                                   @StringRes int cancelButtonStringResource,
                                                   @StringRes int confirmButtonStringResource,
                                                   @NonNull View.OnClickListener confirmationListener) {
        displayConfirmationDialogue(context, lifecycle, titleStringResource, -1,
                cancelButtonStringResource, confirmButtonStringResource, -1,
                confirmationListener, null);
    }
    
    public static void displayEditTextDialogue(@NonNull Context context,
                                               @NonNull Lifecycle lifecycle,
                                               @StringRes int titleStringResource,
                                               @StringRes int messageStringResource,
                                               @StringRes int hintStringResource,
                                               @StringRes int cancelButtonStringResource,
                                               @StringRes int confirmButtonStringResource,
                                               @StringRes int secondaryButtonResource,
                                               @NonNull String defaultEditTextValue,
                                               @NonNull OnClickListenerWithEditText confirmationListener,
                                               @Nullable OnClickListenerWithEditText secondaryConfirmationListener) {
        Dialog dialog = buildTemplate(context, lifecycle, titleStringResource, messageStringResource, R.layout.edit_text_dialogue);
        
        ((TextInputLayout) dialog.findViewById(R.id.textField)).setHint(hintStringResource);
        TextInputEditText editText = dialog.findViewById(R.id.entryNameEditText);
        setupEditText(editText, defaultEditTextValue);
        
        setupButtons(dialog,
                cancelButtonStringResource, confirmButtonStringResource, secondaryButtonResource,
                v -> {
                    String text = checkAndGetInput(editText);
                    if (!text.isEmpty() && confirmationListener.onClick(v, text, 0)) {
                        dialog.dismiss();
                    }
                }, secondaryConfirmationListener == null ? null : v -> {
                    String text = checkAndGetInput(editText);
                    if (!text.isEmpty() && secondaryConfirmationListener.onClick(v, text, 0)) {
                        dialog.dismiss();
                    }
                });
    }
    
    public static void displayEditTextDialogue(@NonNull Context context,
                                               @NonNull Lifecycle lifecycle,
                                               @StringRes int titleStringResource,
                                               @StringRes int messageStringResource,
                                               @StringRes int hintStringResource,
                                               @StringRes int cancelButtonStringResource,
                                               @StringRes int confirmButtonStringResource,
                                               @NonNull String defaultEditTextValue,
                                               @NonNull OnClickListenerWithEditText confirmationListener) {
        displayEditTextDialogue(context, lifecycle, titleStringResource, messageStringResource, hintStringResource,
                cancelButtonStringResource, confirmButtonStringResource, -1,
                defaultEditTextValue,
                confirmationListener, null);
    }
    
    public static void displayEditTextDialogue(@NonNull Context context,
                                               @NonNull Lifecycle lifecycle,
                                               @StringRes int titleStringResource,
                                               @StringRes int messageStringResource,
                                               @StringRes int hintStringResource,
                                               @StringRes int cancelButtonStringResource,
                                               @StringRes int confirmButtonStringResource,
                                               @NonNull OnClickListenerWithEditText confirmationListener) {
        displayEditTextDialogue(context, lifecycle, titleStringResource, messageStringResource, hintStringResource,
                cancelButtonStringResource, confirmButtonStringResource, -1,
                "",
                confirmationListener, null);
    }
    
    public static void displayEditTextDialogue(@NonNull Context context,
                                               @NonNull Lifecycle lifecycle,
                                               @StringRes int titleStringResource,
                                               @StringRes int hintStringResource,
                                               @StringRes int cancelButtonStringResource,
                                               @StringRes int confirmButtonStringResource,
                                               @NonNull String defaultEditTextValue,
                                               @NonNull OnClickListenerWithEditText confirmationListener) {
        displayEditTextDialogue(context, lifecycle, titleStringResource, -1, hintStringResource,
                cancelButtonStringResource, confirmButtonStringResource, -1,
                defaultEditTextValue,
                confirmationListener, null);
    }
    
    public static void displayEditTextDialogue(@NonNull Context context,
                                               @NonNull Lifecycle lifecycle,
                                               @StringRes int titleStringResource,
                                               @StringRes int hintStringResource,
                                               @StringRes int cancelButtonStringResource,
                                               @StringRes int confirmButtonStringResource,
                                               @StringRes int secondaryButtonStringResource,
                                               @NonNull String defaultEditTextValue,
                                               @NonNull OnClickListenerWithEditText confirmationListener,
                                               @Nullable OnClickListenerWithEditText secondaryConfirmationListener) {
        displayEditTextDialogue(context, lifecycle, titleStringResource, -1, hintStringResource,
                cancelButtonStringResource, confirmButtonStringResource, secondaryButtonStringResource,
                defaultEditTextValue,
                confirmationListener, secondaryConfirmationListener);
    }
    
    public static void displayEditTextSpinnerDialogue(@NonNull Context context,
                                                      @NonNull Lifecycle lifecycle,
                                                      @StringRes int titleStringResource,
                                                      @StringRes int messageStringResource,
                                                      @StringRes int hintStringResource,
                                                      @StringRes int cancelButtonStringResource,
                                                      @StringRes int confirmButtonStringResource,
                                                      @StringRes int secondaryButtonStringResource,
                                                      @NonNull String defaultEditTextValue,
                                                      @NonNull List<Group> groups,
                                                      int defaultIndex,
                                                      @NonNull OnClickListenerWithEditText confirmationListener,
                                                      @Nullable OnClickListenerWithEditText secondaryConfirmationListener) {
        Dialog dialog = buildTemplate(context, lifecycle, titleStringResource, messageStringResource, R.layout.edit_text_spinner_dialog);
        ((TextInputLayout) dialog.findViewById(R.id.textField)).setHint(hintStringResource);
        TextInputEditText editText = dialog.findViewById(R.id.entryNameEditText);
        setupEditText(editText, defaultEditTextValue);
        
        String[] items = Group.groupListToNames(groups, context);
        SelectableAutoCompleteTextView spinner = dialog.findViewById(R.id.groupSpinner);
        spinner.setSimpleItems(items);
        
        final int[] selectedIndex = {defaultIndex};
        
        spinner.setSelectedItem(defaultIndex);
        spinner.setOnItemClickListener((parent, view, position, id) -> selectedIndex[0] = position);
        
        setupButtons(dialog,
                cancelButtonStringResource, confirmButtonStringResource, secondaryButtonStringResource,
                v -> {
                    String text = checkAndGetInput(editText);
                    if (!text.isEmpty() && confirmationListener.onClick(v, text, selectedIndex[0])) {
                        dialog.dismiss();
                    }
                }, secondaryConfirmationListener == null ? null : v -> {
                    String text = checkAndGetInput(editText);
                    if (!text.isEmpty() && secondaryConfirmationListener.onClick(v, text, selectedIndex[0])) {
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
    
    private static void setupButtons(Dialog dialog,
                                     @StringRes int cancelButtonStringResource,
                                     @StringRes int confirmButtonStringResource,
                                     @StringRes int secondaryButtonStringResource,
                                     @NonNull View.OnClickListener confirmationListener,
                                     @Nullable View.OnClickListener secondaryConfirmationListener) {
        
        MaterialButton confirmButton = dialog.findViewById(R.id.confirm_button);
        MaterialButton secondaryButton = dialog.findViewById(R.id.secondary_action_button);
        MaterialButton cancelButton = dialog.findViewById(R.id.cancel_button);
        
        cancelButton.setText(cancelButtonStringResource);
        confirmButton.setText(confirmButtonStringResource);
        if (secondaryButtonStringResource != -1) {
            secondaryButton.setText(secondaryButtonStringResource);
        } else {
            secondaryButton.setVisibility(View.GONE);
            dialog.findViewById(R.id.button_flex_container).setLayoutParams(
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        confirmButton.setOnClickListener(confirmationListener);
        if (secondaryConfirmationListener != null) {
            secondaryButton.setOnClickListener(secondaryConfirmationListener);
        }
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
                                        @LayoutRes int layoutRes) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(titleStringResource);
        if (messageStringResource != -1) {
            builder.setMessage(messageStringResource);
        }
        builder.setView(layoutRes);
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
            if(dismissListener != null) {
                dismissListener.onDismiss(dialog1);
            }
            lifecycle.removeObserver(dismissLifecycleObserver);
        });
        return dialog;
    }
    
    @FunctionalInterface
    public interface OnClickListenerWithEditText {
        boolean onClick(View view, String text, int selectedIndex);
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
        builder.setPositiveButton(R.string.ignore, null);
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
