package manda094.finalproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Team DATS for the CS491 Mobile Application Development Boston Univ. course on 11/28/16.
 */

public class contact_page extends AppCompatActivity {

    // the correct url to our webservice which provides functions to modify database
    static final String APP_URL = "https://androidphp.run.aws-usw02-pr.ice.predix.io";

    EditText contact_name;
    Button contactsave_btn, cancel_btn;
    ImageButton contacthome_btn;
    TextView contact;
    String value;
    String id;
    String c_name;
    String c_id;
    JSONObject userInfo;
    DataOutputStream printout;
    SharedPreferences sp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_page);

        // Get information of user using sharedPreference
        sp = getSharedPreferences("information_user", Context.MODE_PRIVATE);
        value = sp.getString("user", null);

        // Decode the information to get the user_id
        try {
            JSONArray res = new JSONArray(value);    // *** ...maybe even save to sharedpreferences... *NOT IMPLEMENTED YET*
            userInfo = res.getJSONObject(0);
            id = userInfo.getString("id");

        } catch (Exception e) {
            Log.e("ERROR", e.getMessage(), e);      //otherwise, log the error
        }

        // Define all the Views
        contact_name = (EditText) findViewById(R.id.contact_name);
        cancel_btn = (Button) findViewById(R.id.cancel_btn);
        contact = (TextView) findViewById(R.id.contact);
        contacthome_btn = (ImageButton) findViewById(R.id.contacthome_btn);
        contactsave_btn = (Button) findViewById(R.id.contactsave_btn);

        // Click the cancel button to return to the all_contact page
        cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancel();
            }
        });

        // Click the GE Logo to return to the home page
        contacthome_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToHomeScreen();
            }
        });

        // Click the save button to verify the textView input for contact name
        // If correct form, connect the webservice to query and insert data into database
        // If not correct form, the user will get a Toast
        contactsave_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validation() == true) {
                    new contact_page.setContacts().execute();
                } else {
                    Toast.makeText(getApplicationContext(), "There are form errors", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    // Function to return to the all_contact page when click the cancel button
    public void cancel() {
        Intent myIntent = new Intent(contact_page.this, allcontact_page.class);
        startActivity(myIntent);
        Toast.makeText(getApplicationContext(), "Did not save contact.", Toast.LENGTH_SHORT).show();
    }

    // Function to return to the home page when click the GE Logo
    public void goToHomeScreen() {
        Intent myIntent = new Intent(contact_page.this, HomeScreen.class);
        startActivity(myIntent);
        Toast.makeText(getApplicationContext(), "Return to Home Screen", Toast.LENGTH_SHORT).show();
    }

    // Function to verify the textView input for the contact name
    public boolean validation() {
        boolean ret = true;
        if (!Validate.hasText(contact_name)) ret = false;
        return ret;
    }

    // Create AsyncTask to update the database
    class setContacts extends AsyncTask<Void, Void, String> {

        // Get the name of the contact to be added
        protected void onPreExecute() {
            c_name = contact_name.getText().toString().replace(" ","%20");  //Todo: check more weird character in contact name: . ,- ....
        }

        // Query the database and insert
        protected String doInBackground(Void... urls) {

            try {
                // Try setting up an http request and connection to make sure the contact user exist!

                // Build specific url to query from user table
                String api_url_query = APP_URL + "/?users&param[]=name&val[]=" + c_name;

                // Create new url type
                URL url_query = new URL(api_url_query);

                // Open url connection
                HttpURLConnection urlConnection_query = (HttpURLConnection) url_query.openConnection();

                // Set the request method type
                urlConnection_query.setRequestMethod("GET");

                // Check status of connection
                int status_query = urlConnection_query.getResponseCode();

                // If an error happened, return null. (400 and above usually refer to some type of server error code)
                if (status_query >= 400) {
                    //Log the error
                    Log.e("ERROR", "" + urlConnection_query.getErrorStream());
                    return null;
                }
                try {
                    // Get information back as stream
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection_query.getInputStream()));

                    // Build string out of returned streamed data
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();

                    // If nothing was found, return null.
                    if (stringBuilder.toString().trim().equals("Nothing was found.")) {
                        return null;
                    }
                    // Else, get the contact id that we need to update the database
                    else {
                        JSONArray resp = new JSONArray(stringBuilder.toString());
                        JSONObject record = resp.getJSONObject(0);
                        c_id = record.getString("id");
                    }
                } finally {
                    //Disconnect the connection that was made earlier
                    urlConnection_query.disconnect();
                }
                // Try setting up an http request and connection to store contact in the database!

                // Build specific url to insert into contact table
                String api_url = APP_URL + "/create.php/?contacts";

                // Create new url type
                URL url = new URL(api_url);

                // Open url connection
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                // Set the request method type
                urlConnection.setRequestMethod("POST");

                // Body (data) information
                // Create a way to map what we want to insert/send into a json object  (this is needed because our webservice requires it be put/posted in this manner)
                JSONArray contactArr = new JSONArray();
                JSONObject contact1 = new JSONObject();
                int user_id = Integer.parseInt(id);
                int cont_id = Integer.parseInt(c_id);
                contact1.put("user_id", user_id);
                contact1.put("contact_id", cont_id);
                contactArr.put(contact1);

                // Stream data (body) to connection target:
                printout = new DataOutputStream(urlConnection.getOutputStream());
                String str = contactArr.toString();
                byte[] data = str.getBytes("UTF-8");
                printout.write(data);
                printout.flush();
                printout.close();

                // Check status of connection
                int status = urlConnection.getResponseCode();

                // If an error happened, return null. (400 and above usually refer to some type of server error code)
                if (status >= 400) {
                    Log.e("ERROR", "" + urlConnection.getErrorStream());    //log the error
                    return null;
                }

                try {
                    // Get information back as stream
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

                    // Build string out of returned streamed data
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                } finally {
                    // Disconnect the connection that was made earlier
                    urlConnection.disconnect(); //disconnect the connection that was made earlier
                }
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage(), e);      //otherwise, log the error
                return null;
            }
        }

        // After we recieved the information and connection has closed, update UI
        protected void onPostExecute(String response) {
            if (response == null) {
                // If something went wrong or nothing was found, user will get a Toast.
                Toast.makeText(getApplicationContext(), "Something went wrong OR The user doesn't exist.", Toast.LENGTH_SHORT).show();
                //Log the response
                response = "Something went wrong. Please try again.";
                Log.i("Contact Create Fail: ", response);
            } else {
                // If getting the response successfully:

                // Log the response
                Log.i("Contact Create Win: ", response);

                // Check the response to see if successfully inserted the contact:
                // If duplicate key, already have that contact, user will get a Toast.
                if (response.contains("duplicate key"))
                    Toast.makeText(getApplicationContext(), "The user is already in your contact list.", Toast.LENGTH_SHORT).show();
                // Else, inserted successfully, user will get a Toast, and back to all_contact page
                else {
                    Intent myIntent = new Intent(contact_page.this, allcontact_page.class);
                    startActivity(myIntent);
                    Toast.makeText(getApplicationContext(), "Saved Contact Successfully.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

}
