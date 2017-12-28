package manda094.finalproject;

/**
 * Created by Amanda Doss on 11/15/16.
 * Code adapted from "https://tausiq.wordpress.com/2013/01/19/android-input-field-validation/"
 *
 * CREATED BY TEAM DADTS AT BOSTON UNIV FOR THE COURSE CS 591 MOBILE APP DEV. FINAL PROJECT APPLICATION
 *
 * This activity is the Validation where everything
 */

import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;

import java.util.regex.Pattern;


public class Validate {

    private static final String EMAIL_REGEX = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    private static final String PHONE_REGEX = "\\d{3}-\\d{3}-\\d{4}";

    // Error Messages when things not filled in
    private static final String REQUIRED_MSG = "REQURIED";
    private static final String PASS_MSG = "please recheck password";
    private static final String PHONE_MSG = "###-###-####";
    private static final String EMAIL_MSG = "invalid email format";


//METHODS CALLED TO CHECK USER INPUT IS CORRECT - EVERYTHING IS REQUIRED
     public static boolean isPhoneNumber(EditText editText, boolean required) {
         return isValid(editText, PHONE_REGEX, PHONE_MSG, required);
     }

    public static boolean isEmailAddress(EditText editText, boolean required) {
        return isValid(editText, EMAIL_REGEX, EMAIL_MSG, required);
    }

    // return true if the input field is valid, based on the parameter passed
    public static boolean isValid(EditText editText, String regex, String errMsg, boolean required) {

        String text = editText.getText().toString().trim();
        // clearing the error, if it was previously set by some other values
        editText.setError(null);
        // text required and editText is blank, so return false
        if ( required && !hasText(editText) ) return false;

        // pattern doesn't match so returning false
        if (required && !Pattern.matches(regex, text)) {
            editText.setError(errMsg);
            return false;
        }

        return true;
    }
    public static boolean isPasswordMatching(EditText password, EditText confirmPassword) {
        String pass = password.getText().toString().trim();
        String pass2 = confirmPassword.getText().toString().trim();

        password.setError(null);
        confirmPassword.setError(null);

        if (!(pass.equals(pass2))){
            confirmPassword.setError(PASS_MSG);
            password.setError(PASS_MSG);
            return false;
        }

        return true;
    }

    // check the input field has any text or not
    // return true if it contains text otherwise false
    public static boolean hasText(EditText editText) {

        String text = editText.getText().toString().trim();
        editText.setError(null);

        // length 0 means there is no text
        if (text.length() == 0) {
            editText.setError(REQUIRED_MSG);
            return false;
        }

        return true;
    }




}
