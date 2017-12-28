package manda094.finalproject;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import android.widget.AdapterView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Team DATS for the CS491 Mobile Application Development Boston Univ. course on 11/28/16.
 */

public class Reportcontact extends AppCompatActivity {

    // the correct url to our webservice which provides functions to modify database
    static final String APP_URL = "https://androidphp.run.aws-usw02-pr.ice.predix.io";

    // Define the adapter and list for Custom ListView for checkBox ListView
    private List<HashMap<String, Object>> list = null;
    private CheckBoxAdapter.MyAdapter adapter;

    // Define the adapter used for display all responsible people in a ListView
    private ArrayAdapter<String> arrayAdapter;

    Button sendMessage;
    ListView contactreport_listview;
    ListView responsible_listview;

    String value;
    String id;
    String message;
    String machine;
    String SMSmessage;
    DataOutputStream printout;
    JSONObject userInfo;

    private HashMap<String, String> contact_phone = new HashMap<>(); //use to store contact phone numbers
    List<String> contact_name = new ArrayList<>();      //use to store contact name
    List<String> contact_id = new ArrayList<>();        //use to store contact id
    List<String> responsible_name = new ArrayList<>();  //use to store responsible people's names
    ArrayList<String> listStr = new ArrayList<>();      //use to store selected contact's id which is unique
    SharedPreferences sp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_report);

        // Request permission for sending SMS dynamically
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);

        // Define all the views
        contactreport_listview = (ListView) findViewById(R.id.contactreport_listview);
        responsible_listview = (ListView) findViewById(R.id.responsible_listview);
        sendMessage = (Button) findViewById(R.id.contactSendReport_btn);

        // Define the adapter used for display all responsible people in a ListView
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        // Get information of user, machine_id to report and message to send using sharedPreference
        sp = getSharedPreferences("information_user", Context.MODE_PRIVATE);
        value = sp.getString("user", null);
        message = sp.getString("comment", null);
        machine = sp.getString("machine_id", null);

        // Decode the information to get the user_id
        try {
            JSONArray res = new JSONArray(value);
            userInfo = res.getJSONObject(0);
            id = userInfo.getString("id");
            //format sms message
            SMSmessage =  "Sent by:  " + userInfo.getString("username") + "  (" + userInfo.getString("name") + ") " + "\n" + "Machine id: " + machine + "\n" + "Message: " + message;
        } catch (Exception e) {
            Log.e("ERROR", e.getMessage(), e);      //otherwise, log the error
            SMSmessage =  "\n" + "Machine id: " + machine + "\n" + "Message: " + message;		// just in case
        }

        // Query the database to get people who are responsible for this machine, populate the listView
        // Query the database to get contact if not responsible for this machine and in user's contact, populate the custom listView
        new GetResponsible().execute();

        // Click sendMessage button to send SMS to all responsible people and contact selected
        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SmsManager sms = SmsManager.getDefault();
                for (String contact : listStr) {
                    sms.sendTextMessage(contact_phone.get(contact), null, SMSmessage, null, null);
                }
                // Update the database to set the machine status to be reported
                new setReported().execute();
            }
        });
    }

    // Create AsyncTask to query the database and populate the responsible listView
    class GetResponsible extends AsyncTask<Void, Void, String> {

        protected void onPreExecute() {
        }

        protected String doInBackground(Void... urls) {
            // Try setting up an http request and connection to get contacts!
            try {
                // Build specific url to query from responsible table
                String api_url = APP_URL + "/?respons&param[]=machine_id&val[]=%23" + machine;

                // Create new url type
                URL url = new URL(api_url);

                // Open url connection
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                // Set the request method type
                urlConnection.setRequestMethod("GET");

                // Check status of connection
                int status = urlConnection.getResponseCode();

                // If an error happened, return null. (400 and above usually refer to some type of server error code)
                if (status >= 400) {
                    Log.e("ERROR", "" + urlConnection.getErrorStream());    //log the error
                    return null;
                }

                //Otherwise, try getting information
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
                    //Disconnect the connection that was made earlier
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                //Otherwise, log the error
                Log.e("ERROR", e.getMessage(), e);
                return null;
            }
        }

        // After we received the information and connection has closed:
        protected void onPostExecute(String response) {

            // If no response or bad response, log the info
            if (response == null) {
                response = "Something went wrong. Please try again.";
                Log.i("Contact GET Fail: ", response);
            } else {
                // If getting the response successfully:
                // Log info
                Log.i("Contact GET Win: ", response);

                //If nothing was found, the user will get a toast, and return to the previous page
                if (response.trim().equals("Nothing was found.")) {
                    Toast.makeText(getApplicationContext(), " No such machine.", Toast.LENGTH_SHORT).show();
                    Intent myIntent = new Intent(Reportcontact.this, Report_page.class);
                    startActivity(myIntent);
                    return;
                } else {
                    // Get the information we need, which are the id (Unique primary key) and name appears in the listView
                    try {
                        JSONArray resp = new JSONArray(response);
                        for (int i = 0; i < resp.length(); i++) {
                            JSONObject record = resp.getJSONObject(i);
                            String phone = record.getString("phone");
                            String name = record.getString("name");
                            String id = record.getString("id");
                            responsible_name.add(name);
                            contact_phone.put(id, phone);
                            listStr.add(id);
                        }

                        // Populate the listView for responsible people
                        arrayAdapter.addAll(responsible_name);
                        responsible_listview.setAdapter(arrayAdapter);
                    } catch (Exception e) {
                        Log.e("ERROR", e.getMessage(), e);      //otherwise, log the error
                    }
                }
            }
            // Query the database to get the contact of the user but is not responsible for this machine and populate the custom listView
            new GetReportContacts().execute();
        }
    }

    // Create AsyncTask to query the database to get the contact of the user but is not responsible and populate the custom listView
    class GetReportContacts extends AsyncTask<Void, Void, String> {

        protected void onPreExecute() {
        }

        protected String doInBackground(Void... urls) {
            // Try setting up an http request and connection to get contacts!
            try {
                // Build specific url to query from contacts table
                String api_url = APP_URL + "/?contacts&param[]=user_id&val[]=" + id;

                // Create new url type
                URL url = new URL(api_url);

                // Open url connection
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                // Set the request method type
                urlConnection.setRequestMethod("GET");

                // Check status of connection
                int status = urlConnection.getResponseCode();

                // If an error happened, return null. (400 and above usually refer to some type of server error code)
                if (status >= 400) {
                    Log.e("ERROR", "" + urlConnection.getErrorStream());    //log the error
                    return null;
                }

                //Otherwise, try getting information
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
                    //Disconnect the connection that was made earlier
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage(), e);      //otherwise, log the error
                return null;
            }
        }

        // After we received the information and connection has closed:
        protected void onPostExecute(String response) {
            // If no response or bad response, log the info
            if (response == null) {
                response = "Something went wrong. Please try again.";
                Log.i("Contact GET Fail: ", response);
            } else {
                // If getting the response successfully:

                // Log info
                Log.i("Contact GET Win: ", response);

                //Extract array
                try {
                    // If nothing was found, user will get a Toast. Return.
                    if (response.trim().equals("Nothing was found.")) {
                        Toast.makeText(getApplicationContext(), " No contact found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Get the information we need, which are the id (Unique primary key) and name appears in the listView
                    JSONArray resp = new JSONArray(response);
                    for (int i = 0; i < resp.length(); i++) {
                        JSONObject record = resp.getJSONObject(i);
                        String phone = record.getString("phone");
                        String name = record.getString("name");
                        String id = record.getString("id");
                        if (!contact_phone.containsKey(id)) {
                            contact_phone.put(id, phone);
                            contact_name.add(name);
                            contact_id.add(id);
                        }
                    }

                    // Populate the custom listView
                    showCheckBoxListView();
                } catch (Exception e) {
                    Log.e("ERROR", e.getMessage(), e);      //otherwise, log the error
                }
            }
        }
    }

    // Create AsyncTask to update the database to set the machine status to be reported
    class setReported extends AsyncTask<Void, Void, String> {
        protected void onPreExecute() {
        }

        protected String doInBackground(Void... urls) {
            // Try setting up an http request and connection to update the database to set the machine status to be reported
            try {
                // Build specific url to delete the contact
                String api_url = APP_URL + "/create.php/?rep";

                // Create new url type
                URL url = new URL(api_url);

                // Open url connection
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                // Set the request method type
                urlConnection.setRequestMethod("POST");

                // Body (data) information
                // Create a way to map what we want to insert/send into a json object  (this is needed because our webservice requires it be put/posted in this manner)
                JSONArray contactArr = new JSONArray();
                JSONObject report = new JSONObject();
                int user_id = Integer.parseInt(id);
                String machine_id = "#" + machine;
                report.put("user_id", user_id);
                report.put("machine_id", machine_id);
                contactArr.put(report);

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

                //otherwise, try getting information
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
                    //Disconnect the connection that was made earlier
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage(), e);      //otherwise, log the error
                return null;
            }
        }

        // After we received the information and connection has closed:
        protected void onPostExecute(String response) {
            if (response == null) {
                // If no response or bad response, log info and return
                response = "Something went wrong. Please try again.";
                Log.i("Report Create Fail: ", response);
            } else {
                // Else, log info and return to home page. User will get a Toast.
                Log.i("Report Create Win: ", response);
                Intent myIntent = new Intent(Reportcontact.this, HomeScreen.class);
                startActivity(myIntent);
                Toast.makeText(getApplicationContext(), "Reported and Send SMS Successfully.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Populate the checkBox ListView
    public void showCheckBoxListView() {
        list = new ArrayList<>();

        for (int i = 0; i < contact_name.size(); i++) {

            // Put the information of each item in the list
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("item_tv", contact_name.get(i));
            map.put("item_cb", false);
            list.add(map);

            // Populate the custom ListView
            adapter = new CheckBoxAdapter.MyAdapter(this, list);
            contactreport_listview.setAdapter(adapter);

            // Get and store the information when click an item in the listView
            contactreport_listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View view,
                                        int position, long arg3) {
                    // Found the item clicked by tag of the view
                    CheckBoxAdapter.ViewHolder holder = (CheckBoxAdapter.ViewHolder) view.getTag();

                    // Change the checkbox status when click the item
                    holder.cb.toggle();

                    // Update the map to modify select status
                    CheckBoxAdapter.MyAdapter.isSelected.put(position, holder.cb.isChecked());

                    // Get the information of checked item
                    if (holder.cb.isChecked() == true) {
                        listStr.add(contact_id.get(position));
                    } else {
                        listStr.remove(contact_id.get(position));
                    }
                }

            });
        }
    }
}
