package manda094.finalproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.View;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;

import android.widget.TextView;
import android.widget.Toast;

import android.widget.AdapterView;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * CREATED BY TEAM DADTS AT BOSTON UNIV FOR THE COURSE CS 591 MOBILE APP DEV.
 FINAL PROJECT APPLICATION

 THIS ACTIVITY OUTLINES THE FUNCTIONS USED FOR THE SEND REPORT PAGE.  FROM THIS PAGE, USERS CAN FILL
 OUT INFORMATION SUCH AS THE MACHINE ID AND A MESSAGE TO BE SENT BY SMS TO A MANAGER.

 USERS CAN ACCESS THIS PAGE FROM THE HOME SCREEN AND AFTER CHOOSING AN ANOMALY TO SEND AS A MESSAGE
 FROM THIS PAGE, THE USER IS TAKEN TO THE CONTACT REPORT PAGE WHERE THEY CHOOSE WHO THIS NEEDS TO BE
 SEND TO.

 */

public class Report_page  extends AppCompatActivity {

    // Views
    TextView employeename;
    ImageButton home_btn;
    Button contact_btn;
    EditText report_message, report_machineID;
    String machineID;

    JSONObject userInfo; // JSON object used to parse the data of all the info of the current user
    String curUserName;
    SharedPreferences sp; // SharedPreference used to store data in this Android App
    KeyListener mKeyListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_page);

        // the textView that keeps the name of the current user
        employeename = (TextView)findViewById(R.id.employeename);

        // the editText view that keeps the machineID
        report_machineID = (EditText) findViewById(R.id.report_machineID);

        mKeyListener = report_machineID.getKeyListener();   // get listener

        // the editText view that keeps the comment text input by the user
        report_message = (EditText) findViewById(R.id.report_message);
        report_message.getText().toString();

        // get the machineID sent by the intent from the "View Machine" Page
        if (getIntent() != null) {
            Intent intentFromViewMachine = getIntent();
            machineID = intentFromViewMachine.getStringExtra("machineKey");
            //check if machine id is null
            if (machineID != null) {
                // set textedit to machine id and disable it
                report_machineID.setText(machineID);
                report_machineID.setKeyListener(null);    //disable
            } else {
                report_machineID.setKeyListener(mKeyListener);
            }
        }
        // if we came from Home Page to this Report Page, then we need to do nothing like above
        // and the Home Page was not supposed to send any intent to this Report Page

        // get the complete info of the current user from the SharedPreference
        sp = getSharedPreferences("information_user", Context.MODE_PRIVATE);
        String allInfoOfThisUser = sp.getString("user", null);

        // parse the complete info of the current user, and get his name
        try {
            JSONArray res = new JSONArray(allInfoOfThisUser);
            userInfo = res.getJSONObject(0);
            curUserName = userInfo.getString("username");

            // fill this name into the TextView
            employeename.setText(curUserName);
        }
        catch (Exception e) {
            Log.e("ERROR", e.getMessage(), e);
        }

        //DEFINES LOG OUT BUTTON TO RETURN TO LOG IN PAGE
        home_btn = (ImageButton)findViewById(R.id.reportpagehome_btn);
        home_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                home();
            }
        });

        //DEFINES PROFILE BUTTON TO GO TO PROFILE PAGE
        contact_btn = (Button) findViewById(R.id.report_contactbtn);
        contact_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validation() == true) { //CHECKS VALIDATION OF THE INPUT BY THE USER
                    goToContact();
                } else {
                    Toast.makeText(getApplicationContext(), "There are form errors", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    //FUNCTIONS THAT GO TO THE USE EXPLICIT INTENTS TO GO TO DIFFERENT PAGES WITHIN THE APPLICATION

    public void home() {
        Intent intent_GoToHome = new Intent(Report_page.this, HomeScreen.class);
        startActivity(intent_GoToHome);
        Toast.makeText(getApplicationContext(), "Return to Home", Toast.LENGTH_SHORT).show();
    }

    public void goToContact() {
        Intent intent_GoToContact = new Intent(Report_page.this, Reportcontact.class);
        // intent_GoToContact.putExtra("");

        // save the machineID and commentText into the SharedPreferences, before shifting to the contact page
        sp = getSharedPreferences("information_user", Context.MODE_PRIVATE);
        sp.edit().putString("machine_id", report_machineID.getText().toString()).commit();
        sp.edit().putString("comment", report_message.getText().toString()).commit();

        // go to the page listing all the guys that you can send your report to, including
        // all the responsible personnel of this machine, and all the contacts you currently have
        startActivity(intent_GoToContact);
    }

    //CALLS THE VALIDATION ACTIVITY TO VERIFY USER INPUT.
    public boolean validation() {
        boolean ret = true;

        //CHECKS THAT THE USER HAS ENTERED TEXT INTO THE machineID field
        if (!Validate.hasText(report_machineID))
            ret = false;

        //CHECKS THAT THE USER HAS ENTERED TEXT INTO THE SMS FIELD INPUT
        if (!Validate.hasText(report_message))
            ret = false;
        return ret;
    }

}