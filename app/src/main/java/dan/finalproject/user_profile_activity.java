package manda094.finalproject;

/**
 * activity for the user profile page
 */

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import android.widget.EditText;
import android.widget.Button;

import android.content.Intent;
import android.widget.Toast;


import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class user_profile_activity extends AppCompatActivity {

    Button save_btn, cancel_btn;

    // URL of our UAA instance on the Predix Cloud
    static final String API_URL = "https://3c156de7-aebd-491d-a7bf-cc9975baeb1a.predix-uaa.run.aws-usw02-pr.ice.predix.io";
    // URL of our Web App on the Predix Cloud
    static final String APP_URL = "https://androidphp.run.aws-usw02-pr.ice.predix.io";

    EditText passTextVerify;
    EditText userText;
    EditText passText;
    EditText emailText;
    EditText phoneText;
    EditText nameText;

    DataOutputStream printout;

    String jsonResponse;
    String API_KEY = "";
    String user_temp;
    String pass_temp;
    String email_temp;
    String phone_temp;
    String name_temp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile);

        save_btn = (Button) findViewById(R.id.user_savebtn);
        cancel_btn = (Button) findViewById(R.id.user_cancelbtn);

        nameText = (EditText) findViewById(R.id.user_name);
        userText= (EditText) findViewById(R.id.user_username);
        passText= (EditText) findViewById(R.id.user_password);
        passTextVerify = (EditText) findViewById(R.id.user_password2);
        phoneText = (EditText) findViewById(R.id.user_phone);
        emailText =  (EditText) findViewById(R.id.emailText);

        save_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validation() == true){
                    save();
                }
                else{
                    Toast.makeText(getApplicationContext(), "There are form errors", Toast.LENGTH_LONG).show();
                }
            }
        });
        cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Did not save data", Toast.LENGTH_SHORT).show();
                Intent myIntent = new Intent(user_profile_activity.this, login_activity.class);
                startActivity(myIntent);
            }
        });
    }


    public void save() {
        new user_profile_activity.RetrieveFeedTask().execute();
    }


    public boolean validation(){
        boolean ret = true;

        if (!Validate.hasText(nameText)) ret = false;
        if (!Validate.hasText(userText)) ret = false;
        if (!Validate.hasText(passText)) ret = false;
        if (!Validate.hasText(passTextVerify)) ret = false;
        if (!Validate.isPasswordMatching(passText, passTextVerify)) ret = false;
        if (!Validate.isEmailAddress(emailText, true)) ret = false;
        if (!Validate.isPhoneNumber(phoneText, true)) ret = false;

        return ret;
    }


    // **** FIRST TASK::: ****
    // class that handles connection to predix; used for getting the API key (used later)
    class RetrieveFeedTask extends AsyncTask<Void, Void, String> {
        private Exception exception;

        protected void onPreExecute() {
        }

        protected String doInBackground(Void... urls) {

            // set up an http request and connection
            try {

                // URL of the UAA, with some suffix, to require login
                String api_url = API_URL+"/oauth/token";

                // create new url type
                URL url = new URL(api_url);
                // create Http URL connection
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                //type--> dont know what type based on the curl command? use this website (it really helps):  http://curl.trillworks.com/#node
                urlConnection.setRequestMethod("POST");

                //headers, set some properties of the Connection Request
                urlConnection.setRequestProperty ("Pragma", "no-cache");
                urlConnection.setRequestProperty ("content-type", "application/x-www-form-urlencoded;charset=UTF-8");
                urlConnection.setRequestProperty ("accept", "application/json;charset=UTF-8");
                urlConnection.setRequestProperty ("Cache-Control", "no-cache");
                urlConnection.setRequestProperty ("authorization", "Basic YWRtaW46MTIz");
                urlConnection.setRequestProperty ("Connection", "keep-alive");    // not 100% sure about this --

                //body (-- data) information
                String str = "grant_type=client_credentials";
                byte[] outputInBytes = str.getBytes("UTF-8");       //turn body data into bytes
                OutputStream os = urlConnection.getOutputStream();      //stream the data
                os.write( outputInBytes );      // write the data
                os.close();     //close the stream


                // check status of connection
                int status = urlConnection.getResponseCode();
                if(status >= 400) {     // 400 and above usually refer to some type of server error code(s)
                    Log.e("ERROR", "" + urlConnection.getErrorStream());    //log the error
                    return null;
                }

                // if the connection succeeded, try getting information
                try {
                    //get information back as stream
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    // build string out of returned streamed data
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                }
                finally{
                    urlConnection.disconnect();     //disconnect the connection that was made earlier
                }
            }
            catch(Exception e) {
                Log.e("ERROR", e.getMessage(), e);      //otherwise, log the error
                return null;
            }
        }

        // after we recieved the information and connection has closed:
        protected void onPostExecute(String response) {
            if(response == null) {          // no response or bad response
                //progressBar.setVisibility(View.GONE);
                response = "Something went wrong. Please try again.";
                //responseView.setText(response);
                Toast.makeText(getApplicationContext(), response, Toast.LENGTH_SHORT).show();
            }
            else {
                //progressBar.setVisibility(View.GONE);       //remove progress bar
                Log.i("API SUCCESS INFO", response);    //log info
                jsonResponse = response;        // will be later used in next task to get API key

                new user_profile_activity.RetrieveFeedTask2().execute();   //call next task
            }

        }
    }

    // **** SECOND TASK::: ****
    // class that handles connection request to predix; creates a user using the API key we just obtained
    // from the previous "retrieveFeedTask".
    class RetrieveFeedTask2 extends AsyncTask<Void, Void, String> {
        private Exception exception;

        protected void onPreExecute() {
        }

        String user = userText.getText().toString();
        String pass = passText.getText().toString();
        String email = emailText.getText().toString();
        String phone = phoneText.getText().toString();
        String name = nameText.getText().toString();

        protected String doInBackground(Void... urls) {

            // set up an http request and connection  (this time for creating a user)
            // --- FIRST:  create user in our instance of UAA:
            try {

                String api_url = API_URL+"/Users";    // set to specific url "route"

                //convert json response (string) from previous task into json object
                JSONObject resObject = new JSONObject(jsonResponse);
                //JSONObject keyObject = resObject.getJSONObject("----");
                // for json objects inside of a json object
                String  keyName = resObject.getString("access_token");

                // store the returned accessToken as our APIkey
                API_KEY = keyName;

                // create new url type
                URL url = new URL(api_url);
                // create Http URL Connection
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                //type--> dont know what type based on the curl command? use this website (it really helps):  http://curl.trillworks.com/#node
                urlConnection.setRequestMethod("POST");
                //headers
                urlConnection.setRequestProperty ("Pragma", "no-cache");
                urlConnection.setRequestProperty ("content-type", "application/json;charset=UTF-8");
                //urlConnection.setRequestProperty ("accept", "application/json");
                urlConnection.setRequestProperty ("Cache-Control", "no-cache");
                urlConnection.setRequestProperty ("authorization", "bearer " + API_KEY);

                // -- body (-- data) information
                // Create a way to map what we want to insert/send into a json object
                // (this is needed because the UAA curl command requires it be put/posted in this manner)
                JSONObject user1 = new JSONObject();
                user1.put("userName", user);
                user1.put("password", pass);

                JSONObject email_values = new JSONObject();
                email_values.put("value", email);

                JSONArray jsonarray = new JSONArray();
                jsonarray.put(email_values);

                user1.put("emails", jsonarray);

                // -- stream data (body) to connection target:
                printout = new DataOutputStream(urlConnection.getOutputStream ());
                String str = user1.toString();
                byte[] data=str.getBytes("UTF-8");
                printout.write(data);
                printout.flush ();
                printout.close ();

                // check status of connection
                int status = urlConnection.getResponseCode();
                if(status >= 400) {     // 400 and above usually refer to some type of server error code(s)
                    Log.e("ERROR", "" + urlConnection.getErrorStream());    //log the error
                    return null;
                }
                //otherwise, try getting information
                try {
                    //get information back as stream
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    // build string out of returned streamed data
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                }
                finally{
                    urlConnection.disconnect();     //disconnect the connection that was made earlier
                }
            }
            catch(Exception e) {
                Log.e("ERROR", e.getMessage(), e);      //otherwise, log the error
                return null;
            }
        }

        // after we recieved the information and connection has closed:
        protected void onPostExecute(String response) {
            if(response == null) {          // no response or bad response
                //progressBar.setVisibility(View.GONE);
                response = "Something went wrong. Please try again.";
                //responseView.setText(response);
                Toast.makeText(getApplicationContext(), response, Toast.LENGTH_SHORT).show();

            }
            else {
                //progressBar.setVisibility(View.GONE);       //remove progress bar
                Log.i("UAA User Success INFO", response);    //log info
                // put typed in information into temp variables, so that they are not changed during connection process (could cause a big mess)
                user_temp = user;
                phone_temp = phone;
                name_temp = name;
                //email_temp = email;   //---> currently, our database does not have a column for emails

                // start new (and final) task that will create a "cloned" user (same one created in UAA instance)
                // in our database on the predix Application
                new user_profile_activity.RetrieveFeedTask3().execute(); //call final task
            }

        }
    }


    // **** FINAL TASK::: ****
    // class that handles connection request to our predix appliciation
    // (the web-service; also known as the "black box");
    // this will create a "cloned" version (in our database) of the user
    // that was created in our UAA instance.
    class RetrieveFeedTask3 extends AsyncTask<Void, Void, String> {
        private Exception exception;

        protected void onPreExecute() {
        }

        String user = user_temp;
        //String email = email_temp;
        String phone = phone_temp;
        String name = name_temp;

        protected String doInBackground(Void... urls) {

            // set up an http request and connection  (this time for creating a user)
            // --- Now:  create user in our postgres database instance:
            try {

                String api_url = APP_URL+"/create.php/?users";    // set to specific url "route"

                URL url = new URL(api_url);     // create new url type
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                //headers
                urlConnection.setRequestProperty ("content-type", "application/json;charset=UTF-8");
                //urlConnection.setRequestProperty ("accept", "application/json");


                //type--> dont know what type based on the curl command? use this website (it really helps):  http://curl.trillworks.com/#node
                urlConnection.setRequestMethod("POST");
                //headers

                // -- body (-- data) information
                // Create a way to map what we want to insert/send into a json object
                // (this is needed because our webservice requires it be put/posted in this manner)
                JSONObject user1 = new JSONObject();
                user1.put("username", user);
                user1.put("name", name);
                user1.put("phone", phone);

                // -- stream data (body) to connection target:
                printout = new DataOutputStream(urlConnection.getOutputStream ());
                String str = user1.toString();
                byte[] data=str.getBytes("UTF-8");
                printout.write(data);
                printout.flush ();
                printout.close ();

                // check status of connection
                int status = urlConnection.getResponseCode();
                if(status >= 400) {     // 400 and above usually refer to some type of server error code(s)
                    Log.e("ERROR", "" + urlConnection.getErrorStream());    //log the error
                    return null;
                }

                // if the connection succeeded, try getting information
                try {
                    //get information back as stream
                    BufferedReader bufferedReader =
                            new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    // build string out of returned streamed data
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                }
                finally{
                    urlConnection.disconnect();     //disconnect the connection that was made earlier
                }
            }
            catch(Exception e) {
                Log.e("ERROR", e.getMessage(), e);      //otherwise, log the error
                return null;
            }
        }

        // after we recieved the information and connection has closed:
        protected void onPostExecute(String response) {
            if(response == null) {          // no response or bad response
                //progressBar.setVisibility(View.GONE);
                response = "Something went wrong. Please try again.";
                //responseView.setText(response);
                Toast.makeText(getApplicationContext(), response, Toast.LENGTH_SHORT).show();
            }
            else {
                //progressBar.setVisibility(View.GONE);       //remove progress bar
                Log.i("User Success! INFO", response);    //log info

                // send back to MainActivity (login Screen):
               // Intent gotoLogin = new Intent(user_profile_activity.this, login_activity.class);      //create new activity
                //gotoLogin.putExtra("message", "Your account was successfully created! Please Log in.");
                //user_profile_activity.this.startActivity(gotoLogin);

                Intent myIntent = new Intent(user_profile_activity.this,login_activity.class);
                startActivity(myIntent);
                Toast.makeText(getApplicationContext(), "Your account was successfully created! Returning to Login...", Toast.LENGTH_SHORT).show();

            }
        }
    }


}