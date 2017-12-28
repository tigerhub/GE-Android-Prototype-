package manda094.finalproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import android.widget.AdapterView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Team DATS for the CS491 Mobile Application Development Boston Univ. course on 11/28/16.
 */

public class allcontact_page extends AppCompatActivity {

    // the correct url to our webservice which provides functions to modify database
    static final String APP_URL = "https://androidphp.run.aws-usw02-pr.ice.predix.io";

    // Define the adapter and list for Custom ListView for checkBox ListView
    private CheckBoxAdapter.MyAdapter myAdapter;
    private List<HashMap<String, Object>> list = null;

    Button add_btn;
    ImageButton deletecontact_btn;
    ListView contact_listview;

    String value;
    String id;
    String d_id;
    JSONObject userInfo;

    List<String> contact_name = new ArrayList<>();  //use to store contact name
    List<String> delete_name = new ArrayList<>();   //use to store contact name for delete
    ArrayList<String> listStr = new ArrayList<>();  //use to store selected contact's id which is unique
    List<String> contact_id = new ArrayList<>();    //use to store contact id
    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.all_contacts);

        // Get information of user using sharedPreference
        sp = getSharedPreferences("information_user", Context.MODE_PRIVATE);
        value = sp.getString("user", null);

        // Decode the information to get the user_id
        try {
            JSONArray res = new JSONArray(value);
            userInfo = res.getJSONObject(0);
            id = userInfo.getString("id");

        } catch (Exception e) {
            Log.e("ERROR", e.getMessage(), e);      //otherwise, log the error
        }

        // Define all the views
        contact_listview = (ListView) findViewById(R.id.contact_listview);
        deletecontact_btn = (ImageButton) findViewById(R.id.deletecontact_btn);
        add_btn = (Button) findViewById(R.id.add_btn);

        // Populate contacts in custom ListView with checkbox from database
        new GetContacts().execute();

        // Click the add button to go to contact page (add contact page)
        add_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToContactPage();
            }
        });

        // Click the delete imageButton to delete the contact in database and repopulate the custom listView
        deletecontact_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If nothing is selected, user will get a Toast
                if (listStr == null || listStr.size() == 0) {
                    Toast.makeText(getApplicationContext(), "Please select someone to delete.", Toast.LENGTH_SHORT).show();
                }
                // Else delete the contact from database and repopulate the custom listView
                else {
                    for (int i = 0; i < listStr.size(); i++) {
                        d_id = listStr.get(i);
                        new DeleteContacts().execute();
                    }
                    listStr = new ArrayList<>();
                    contact_name.removeAll(delete_name);
                    contact_id.removeAll(listStr);
                    delete_name = new ArrayList<>();
                    // User will get a Toast
                    Toast.makeText(getApplicationContext(), "Delete Successfully!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    // Function to go to the add contact page when click the add button
    public void goToContactPage() {
        Intent myIntent = new Intent(allcontact_page.this, contact_page.class);
        startActivity(myIntent);
    }

    // Create AsyncTask to query the database and populate the contact listView
    class GetContacts extends AsyncTask<Void, Void, String> {

        protected void onPreExecute() {
        }

        protected String doInBackground(Void... urls) {
            // Try setting up an http request and connection to get contacts!
            try {
                // Build specific url to query from contacts table
                String api_url = APP_URL + "/?contacts&param[]=user_id&val[]=" + id;

                // Create new url type
                URL url = new URL(api_url);     // create new url type

                // Open url connection
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                // Set the request method type
                urlConnection.setRequestMethod("GET");

                // Check status of connection
                int status = urlConnection.getResponseCode();

                // If an error happened, return null. (400 and above usually refer to some type of server error code)
                if (status >= 400) {
                    //Log the error
                    Log.e("ERROR", "" + urlConnection.getErrorStream());
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
                        String id = record.getString("id");
                        String name = record.getString("name");
                        contact_name.add(name);
                        contact_id.add(id);
                    }

                    // Populate the custom listView
                    showCheckBoxListView();
                } catch (Exception e) {
                    Log.e("ERROR", e.getMessage(), e);      //otherwise, log the error
                }
            }
        }
    }

    // Create AsyncTask to delete in the database and repopulate the contact listView
    class DeleteContacts extends AsyncTask<Void, Void, String> {
        private String delete_id;

        // Get the id of the contact to be deleted
        protected void onPreExecute() {
            delete_id = d_id;
        }

        // Delete in the database
        protected String doInBackground(Void... urls) {
            // Try setting up an http request and connection to delete the contact
            try {
                // Build specific url to delete the contact
                String api_url = APP_URL + "/api.php/?removeContact&param[]=user_id&param[]=contact_id&val[]=" + id + "&val[]=" + delete_id;    // set to specific url "route"

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
                    // Log the error
                    Log.e("ERROR", "" + urlConnection.getErrorStream());
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
            // If no response or bad response, log info and return
            if (response == null) {
                response = "Something went wrong. Please try again.";
                Log.i("Delete GET Fail: ", response);
            } else {
                // Else, log info and repopulate the custom listView
                Log.i("Delete GET Win: ", response);
                showCheckBoxListView();
            }
        }
    }

    // Populate the checkBox ListView
    public void showCheckBoxListView() {
        list = new ArrayList<>();

        // If no contact found, set an empty custom listView
        if (contact_name.size() == 0) {
            myAdapter = new CheckBoxAdapter.MyAdapter(this, list);
            contact_listview.setAdapter(myAdapter);
            return;
        }

        // Else, populate the custom listView
        for (int i = 0; i < contact_name.size(); i++) {
            // Put the information of each item in the list
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("item_tv", contact_name.get(i));
            map.put("item_cb", false);
            list.add(map);

            // Populate the custom ListView
            myAdapter = new CheckBoxAdapter.MyAdapter(this, list);
            contact_listview.setAdapter(myAdapter);

            // Get and store the information when click an item in the listView
            contact_listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

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
                        delete_name.add(contact_name.get(position));
                    } else {
                        listStr.remove(contact_id.get(position));
                        delete_name.remove(contact_name.get(position));
                    }
                }

            });
        }
    }
}
