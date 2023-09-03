package prototype.xd.scheduler.utilities;

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
import androidx.fragment.app.DialogFragment;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

import prototype.xd.scheduler.R;
import prototype.xd.scheduler.databinding.TwoButtonsBinding;
import prototype.xd.scheduler.utilities.misc.ContextWrapper;
import prototype.xd.scheduler.views.settings.PopupSettingsView;

public final class DialogUtilities {
    
    public static final String NAME = Utilities.class.getSimpleName();
    
    private DialogUtilities() throws InstantiationException {
        throw new InstantiationException(NAME);
    }
    
    /**
     * Sets up edit text field on the dialog
     *
     * @param editText             edit text field
     * @param defaultEditTextValue default (starting) value
     */
    public static void setupEditText(@NonNull EditText editText,
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
     * @param dialogFragment              target dialog fragment
     * @param buttonContainer             view binding for two buttons
     * @param cancelButtonStringResource  cancel button string resource
     * @param confirmButtonStringResource confirm button string resource
     * @param confirmationListener        listener to call when the dialog is confirmed
     */
    public static void setupButtons(@NonNull DialogFragment dialogFragment,
                                    @NonNull TwoButtonsBinding buttonContainer,
                                    @StringRes int cancelButtonStringResource,
                                    @StringRes int confirmButtonStringResource,
                                    @NonNull View.OnClickListener confirmationListener) {
        
        Button confirmButton = buttonContainer.confirmButton;
        Button cancelButton = buttonContainer.cancelButton;
        
        cancelButton.setText(cancelButtonStringResource);
        confirmButton.setText(confirmButtonStringResource);
        
        cancelButton.setOnClickListener(v -> dialogFragment.dismiss());
        confirmButton.setOnClickListener(confirmationListener);
    }
    
    /**
     * Invokes successCallback when text input field is not empty
     *
     * @param editText        text input field
     * @param successCallback callback to invoke
     */
    public static void callIfInputNotEmpty(@NonNull EditText editText, @NonNull Consumer<String> successCallback) {
        String text = editText.getText() == null ? "" : editText.getText().toString().trim();
        if (text.isEmpty()) {
            editText.setError(editText.getContext().getString(R.string.input_cant_be_empty));
        } else {
            successCallback.accept(text);
        }
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
    
    public static void displayAttentionDialog(@NonNull final ContextWrapper wrapper,
                                              @StringRes int message,
                                              @StringRes int buttonText) {
        displayMessageDialog(wrapper, builder -> {
            builder.setTitle(R.string.attention);
            builder.setMessage(message);
            builder.setIcon(R.drawable.ic_warning_24);
            builder.setPositiveButton(buttonText, null);
        });
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
    
    public static void displayErrorDialog(@NonNull final ContextWrapper wrapper,
                                          @StringRes int title,
                                          @StringRes int message,
                                          @Nullable DialogInterface.OnDismissListener dismissListener) {
        displayMessageDialog(wrapper, R.style.ErrorAlertDialogTheme, builder -> {
            builder.setTitle(title);
            builder.setMessage(message);
            builder.setIcon(R.drawable.ic_warning_24);
            builder.setPositiveButton(R.string.close, null);
        }, dismissListener);
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
