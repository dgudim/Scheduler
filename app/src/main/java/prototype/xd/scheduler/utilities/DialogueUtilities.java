package prototype.xd.scheduler.utilities;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.entities.Group;
import prototype.xd.scheduler.entities.GroupList;
import prototype.xd.scheduler.views.SelectableAutoCompleteTextView;

public class DialogueUtilities {
    
    public static void displayConfirmationDialogue(Context context,
                                                   @StringRes int titleStringResource,
                                                   @StringRes int messageStringResource,
                                                   @StringRes int cancelButtonStringResource,
                                                   @StringRes int confirmButtonStringResource,
                                                   @StringRes int secondaryButtonStringResource,
                                                   @NonNull View.OnClickListener confirmationListener,
                                                   @Nullable View.OnClickListener secondaryConfirmationListener) {
        Dialog dialog = buildTemplate(context, titleStringResource, messageStringResource, R.layout.three_buttons);
        
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
    
    // TODO: 20.11.2022 close dialogs on activity exit? 
    
    public static void displayConfirmationDialogue(Context context,
                                                   @StringRes int titleStringResource,
                                                   @StringRes int messageStringResource,
                                                   @StringRes int cancelButtonStringResource,
                                                   @StringRes int confirmButtonStringResource,
                                                   @NonNull View.OnClickListener confirmationListener) {
        displayConfirmationDialogue(context, titleStringResource, messageStringResource,
                cancelButtonStringResource, confirmButtonStringResource, -1,
                confirmationListener, null);
    }
    
    public static void displayConfirmationDialogue(Context context,
                                                   @StringRes int titleStringResource,
                                                   @StringRes int cancelButtonStringResource,
                                                   @StringRes int confirmButtonStringResource,
                                                   @NonNull View.OnClickListener confirmationListener) {
        displayConfirmationDialogue(context, titleStringResource, -1,
                cancelButtonStringResource, confirmButtonStringResource, -1,
                confirmationListener, null);
    }
    
    public static void displayEditTextDialogue(@NonNull Context context,
                                               @StringRes int titleStringResource,
                                               @StringRes int messageStringResource,
                                               @StringRes int hintStringResource,
                                               @StringRes int cancelButtonStringResource,
                                               @StringRes int confirmButtonStringResource,
                                               @StringRes int secondaryButtonResource,
                                               @NonNull String defaultEditTextValue,
                                               @NonNull OnClickListenerWithEditText confirmationListener,
                                               @Nullable OnClickListenerWithEditText secondaryConfirmationListener) {
        Dialog dialog = buildTemplate(context, titleStringResource, messageStringResource, R.layout.edit_text_dialogue);
        
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
                                               @StringRes int titleStringResource,
                                               @StringRes int messageStringResource,
                                               @StringRes int hintStringResource,
                                               @StringRes int cancelButtonStringResource,
                                               @StringRes int confirmButtonStringResource,
                                               @NonNull String defaultEditTextValue,
                                               @NonNull OnClickListenerWithEditText confirmationListener) {
        displayEditTextDialogue(context, titleStringResource, messageStringResource, hintStringResource,
                cancelButtonStringResource, confirmButtonStringResource, -1,
                defaultEditTextValue,
                confirmationListener, null);
    }
    
    public static void displayEditTextDialogue(@NonNull Context context,
                                               @StringRes int titleStringResource,
                                               @StringRes int messageStringResource,
                                               @StringRes int hintStringResource,
                                               @StringRes int cancelButtonStringResource,
                                               @StringRes int confirmButtonStringResource,
                                               @NonNull OnClickListenerWithEditText confirmationListener) {
        displayEditTextDialogue(context, titleStringResource, messageStringResource, hintStringResource,
                cancelButtonStringResource, confirmButtonStringResource, -1,
                "",
                confirmationListener, null);
    }
    
    public static void displayEditTextDialogue(@NonNull Context context,
                                               @StringRes int titleStringResource,
                                               @StringRes int hintStringResource,
                                               @StringRes int cancelButtonStringResource,
                                               @StringRes int confirmButtonStringResource,
                                               @NonNull String defaultEditTextValue,
                                               @NonNull OnClickListenerWithEditText confirmationListener) {
        displayEditTextDialogue(context, titleStringResource, -1, hintStringResource,
                cancelButtonStringResource, confirmButtonStringResource, -1,
                defaultEditTextValue,
                confirmationListener, null);
    }
    
    public static void displayEditTextDialogue(@NonNull Context context,
                                               @StringRes int titleStringResource,
                                               @StringRes int hintStringResource,
                                               @StringRes int cancelButtonStringResource,
                                               @StringRes int confirmButtonStringResource,
                                               @StringRes int secondaryButtonStringResource,
                                               @NonNull String defaultEditTextValue,
                                               @NonNull OnClickListenerWithEditText confirmationListener,
                                               @Nullable OnClickListenerWithEditText secondaryConfirmationListener) {
        displayEditTextDialogue(context, titleStringResource, -1, hintStringResource,
                cancelButtonStringResource, confirmButtonStringResource, secondaryButtonStringResource,
                defaultEditTextValue,
                confirmationListener, secondaryConfirmationListener);
    }
    
    public static void displayEditTextSpinnerDialogue(@NonNull Context context,
                                                      @StringRes int titleStringResource,
                                                      @StringRes int messageStringResource,
                                                      @StringRes int hintStringResource,
                                                      @StringRes int cancelButtonStringResource,
                                                      @StringRes int confirmButtonStringResource,
                                                      @StringRes int secondaryButtonStringResource,
                                                      @NonNull String defaultEditTextValue,
                                                      @NonNull GroupList groups,
                                                      int defaultIndex,
                                                      @NonNull OnClickListenerWithEditText confirmationListener,
                                                      @Nullable OnClickListenerWithEditText secondaryConfirmationListener) {
        Dialog dialog = buildTemplate(context, titleStringResource, messageStringResource, R.layout.edit_text_spinner_dialog);
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
    
    private static Dialog buildTemplate(@NonNull Context context,
                                        @StringRes int titleStringResource,
                                        @StringRes int messageStringResource,
                                        @LayoutRes int layoutRes) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(titleStringResource);
        if (messageStringResource != -1) {
            builder.setMessage(messageStringResource);
        }
        builder.setView(layoutRes);
        return builder.show();
    }
    
    @FunctionalInterface
    public interface OnClickListenerWithEditText {
        boolean onClick(View view, String text, int selectedIndex);
    }
    
}
