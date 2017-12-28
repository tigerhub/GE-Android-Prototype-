package manda094.finalproject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;

import android.os.Bundle;
import android.content.Intent;
import android.util.Log;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import static java.net.Proxy.Type.HTTP;


public class login_activity extends AppCompatActivity {

    EditText edt_Username, edt_Password;
    Button btn_Login, btn_Register;

    // the JSON object (containing the user info) which was sent back from the DB
    JSONObject userInfo;
    String username;

    // for later use possibly (this will the authentication key holder/ container)
    static final String API_KEY = "API_KEY";

    // For predix, the api_url (for now) is just your account's specific predix cloud instance,
    // which is a UAA service instance
    static final String API_URL = "https://3c156de7-aebd-491d-a7bf-cc9975baeb1a.predix-uaa.run.aws-usw02-pr.ice.predix.io";
    // URL of the Web App (Webservice) by which we visit the Data Base from our Android App
    static final String APP_URL = "https://androidphp.run.aws-usw02-pr.ice.predix.io";

    // the SharedPreferences used to store data in this Android App
    SharedPreferences sp;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);
        setTitle("Predix Android App");

        edt_Username = (EditText) findViewById(R.id.login_username); // username of the user
        edt_Password = (EditText) findViewById(R.id.login_password); // password of the user

        // sign in button
        btn_Login = (Button) findViewById(R.id.login_btn);
        // sign up (register for app) button
        btn_Register = (Button) findViewById(R.id.register_btn);

        // if we returned to this screen from another activity (ie: register page, or user logged out):
        Intent intent = getIntent();
        String value = intent.getStringExtra("message");

        // bind sign-in button to a on-click-listener
        btn_Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RetrieveFeedTask().execute();
            }
        });

        // bind sign-up button to a on-click-listener
        btn_Register.setOnClickListener(myhandler1);
    }


    // the on-click-listener that will lead to the register page
    View.OnClickListener myhandler1 = new View.OnClickListener() {
        public void onClick(View v) {
            // go to register action
            Intent myIntent = new Intent(login_activity.this, user_profile_activity.class);
            login_activity.this.startActivity(myIntent);
        }
    };


    // **** FIRST TASK::: ****
    // a class that handles connections / requests
    class RetrieveFeedTask extends AsyncTask<Void, Void, String> {
        String user = "";
        private Exception exception;

        protected void onPreExecute() {
        }

        String email = edt_Username.getText().toString();
        String pass2 = edt_Password.getText().toString();

        // Validate the account (username and password) the user typed in
        // by UAA
        protected String doInBackground(Void... urls) {

            // set up an http request and connection
            try {

                // URL of the UAA, with some suffix, to require login
                String api_url = API_URL+"/oauth/token";
                user = email;
                String pass = pass2;

                // create new url type
                URL url = new URL(api_url);
                // create a new Http URL Connection
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                //type--> dont know what type based on the curl command? use this website (it really helps):  http://curl.trillworks.com/#node
                urlConnection.setRequestMethod("POST");

                //headers, set some properties of the Connection Request
                urlConnection.setRequestProperty ("Pragma", "no-cache");
                urlConnection.setRequestProperty ("content-type", "application/x-www-form-urlencoded;charset=UTF-8");
                urlConnection.setRequestProperty ("Cache-Control", "no-cache");
                urlConnection.setRequestProperty ("authorization", "Basic dGVzdDE6MTIz");

                //body (-- data) information
                String str = "username=" + user+"&password="+pass+"&grant_type=password";

                byte[] outputInBytes = str.getBytes("UTF-8");       //turn body data into bytes
                OutputStream os = urlConnection.getOutputStream();      //stream the data
                os.write( outputInBytes );      // write the data
                os.close();     //close the stream

                // check status of the connection
                int status = urlConnection.getResponseCode();
                if(status >= 400) {     // 400 and above usually refer to some type of server error code(s)
                    Log.e("ERROR", "" + urlConnection.getErrorStream());    //log the error
                    String response = "Something went wrong with the connection. Please try again.";
                    Toast.makeText(getApplicationContext(), response, Toast.LENGTH_LONG).show();
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
                    urlConnection.disconnect(); //disconnect the connection that was made earlier
                }
            }
            catch(Exception e) {
                Log.e("ERROR", e.getMessage(), e); //if anything goes wrong, log the error
                return null;
            }
        }

        // after we recieved the information and connection has closed
        protected void onPostExecute(String response) {
            if(response == null) {  // no response or bad response
                String errMsg = "Something went wrong. Please try again.";
                Toast.makeText(getApplicationContext(), errMsg, Toast.LENGTH_LONG).show();
            }
            else {
                Log.i("INFO", response);    //log info

                // set username to user for our next query
                username = user;
                // now query our own database on Predix Cloud, to get the rest of the credentials;
                // if this fails, then they will not be able to log-in
                new login_activity.getUserCreds().execute();
            }
        }
    }


    // **** SECOND TASK::: ****
    //query our own database on Predix Cloud for more credentials, via the Web App
    class getUserCreds extends AsyncTask<Void, Void, String>
    {
        private Exception exception;

        protected void onPreExecute() {
        }

        protected String doInBackground(Void... urls) {

            // set up an http request and connection
            try {

                // Visit the URL of our Web App on the Predix Cloud, and by this we are
                // actually visiting the DB on the Predix Cloud via our Web App
                // For the specific URL written below, we are checking if the input username
                // exists in our DB, namely if the account is a valid existing account
                String api_url = APP_URL+"/?users&param[]=username&val[]=" + username;

                // create new url type
                URL url = new URL(api_url);
                // create a Http URL Connection
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                //type--> dont know what type based on the curl command? use this website (it really helps):  http://curl.trillworks.com/#node
                urlConnection.setRequestMethod("GET");

                // check status of connection
                int status = urlConnection.getResponseCode();
                if(status >= 400) {     // 400 and above usually refer to some type of server error code(s)
                    Log.e("ERROR STREAM 2", "" + urlConnection.getErrorStream());    //log the error
                    String response = "Something went wrong. Please try again.";
                    Toast.makeText(getApplicationContext(), response, Toast.LENGTH_LONG).show();
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
                Log.e("ERROR Stream", e.getMessage(), e);      //otherwise, log the error
                return null;
            }
        }

        // after we recieved the information and connection has closed:
        protected void onPostExecute(String response) {
            if(response == null) {  // no response or bad response
                Log.i("Credentials Failure: ", response);
                response = "Something went wrong. Please try again.";
                Toast.makeText(getApplicationContext(), response, Toast.LENGTH_LONG).show();
            }
            else {
                Log.i("Cred. response INFO: ", response);    //log info
                try {
                    JSONArray res = new JSONArray(response);
                    userInfo = res.getJSONObject(0);   //if this fails; then we can say that something went wrong;

                    // otherwise, if everything goes well:
                    // we will go to the Main Page
                    Intent gotoMainScreen = new Intent(login_activity.this, HomeScreen.class);
                    gotoMainScreen.putExtra("user", response);

                    // save the user information into SharedPreferences
                    sp = getSharedPreferences("information_user", Context.MODE_PRIVATE);
                    sp.edit().putString("user", response).commit();

                    // go to the Main Page
                    login_activity.this.startActivity(gotoMainScreen);
                }
                catch(Exception e) {
                    Log.e("ERROR", e.getMessage(), e);      //otherwise, log the error
                    Log.i("Credentials Failure: ", response);

                }
            }
        }

    }


}