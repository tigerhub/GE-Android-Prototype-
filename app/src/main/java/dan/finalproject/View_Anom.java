package manda094.finalproject;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Toast;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * CREATED BY TEAM DADTS AT BOSTON UNIV FOR THE COURSE CS 591 MOBILE APP DEV.
 * FINAL PROJECT APPLICATION
 *
 * THIS IS THE FUNCTIONALITY PAGE FOR THE VIEW ANOMALIES PAGE
 * ON THIS PAGE, USERS WILL SEE A LIST VIEW OF ALL ANOMALIES THAT THIS CURRENT MACHINE IS EXPERIENCING
 *
 *
 * ASSOCIATED XML LAYOUT IS VIEW_ANOM
 */

public class View_Anom extends AppCompatActivity {

    // the button which leads back to the page of viewing all the machines that went wrong
    ImageButton viewmachinehome_btn;

    // interval of asking for new anomalies, if any, in milli-second. So 1000 means 1 second
    private int Interval = 25000;
    // the list view of all the anomalies of the current machine
    private ListView Anom_lv;

    ArrayList<Item> anom_ids;       // distinct machine_id list
    ArrayList<Item> anom_tempIDs;
    JSONArray anoms;   //this is what we will use to store anomaly data for later display
    String str_response;
    String value;
    String machine_id;

    ArrayAdapter<String> anomAdapter;
    MyAdapter Anomadapter; // our custom Adapter of the custom ListView
    Button sendreport_btn;
    TextView title;

    Handler handlerAnom;		// our handler for continuously requesting a response from our webservice

    // the URL of the Web App (Web-service)
    static final String APP_URL = "https://androidphp.run.aws-usw02-pr.ice.predix.io";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_anom);

        Intent intent = getIntent();
        machine_id = intent.getStringExtra("machine"); // get the info of the machine from the intent
        machine_id = machine_id.substring(1);   // just get the actual varchar without the "#"

        TextView title = (TextView) findViewById(R.id.viewmanom_title);
        title.setText("ANOMALIES FOR " + machine_id + "");

        //DEFINES SEND REPORT BUTTON TO GO TO SEND REPORT PAGE
        sendreport_btn = (Button) findViewById(R.id.sendReport_btn);
        sendreport_btn.setText("Report machine");
        sendreport_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v){
                Intent myIntent = new Intent(View_Anom.this, Report_page.class);

                // the id of this machine will be passed to the "send report" page for displaying
                myIntent.putExtra("machineKey", machine_id);
                startActivity(myIntent);
                finish();
            }
        });

        // the element type of these 2 ArraLists, "Item", is a public class that we made,
        // containing 2 properties: "title" and "description"
        anom_ids = new ArrayList<Item>();
        anom_tempIDs = new ArrayList<Item>();


        // ** create handler class which will auto-refresh every 15-30 sec (or so) here:::
        new View_Anom.RetrieveFeedTask().execute();
        handlerAnom = new Handler();        //initialize handler



        // 1. pass context and data to the custom adapter
        Anomadapter = new MyAdapter(this, anom_ids);

        // 2. Get ListView from activity_main.xml
        ListView listView = (ListView) findViewById(R.id.Anom_lv);

        // 3. setListAdapter
        listView.setAdapter(Anomadapter);

        //IMPLEMENTS VIEW MACHINE BUTTON AND FUNCTION THAT RETURNS THE USER BACK TO THE HOME SCREEN
        //SETS ONCLICKLISTERNER TO THE BUTTON
        viewmachinehome_btn = (ImageButton) findViewById(R.id.viewmachinehome_btn);
        viewmachinehome_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick (View v) {
                goHome();
            }
        });



    }


    // when we resume the activity, create the next delayed handler request 
    @Override
    public void onResume() {
        super.onResume();
        handlerAnom.postDelayed(runnableCodeAnom, Interval);
    }

    // Just as we did on Destroy, we also have it on pause; 
	// this is to lessen the load on our system, so that it only updates while you are viewing that page
    @Override
    public void onPause() {
        super.onPause();
        handlerAnom.removeCallbacks(runnableCodeAnom);
    }
	

// when we destory the activity, we want to remove all handler call-backs (in our case, this stops doing the "Every 25 sec interval- query the database")
    @Override
    public void onDestroy() {
        super.onDestroy();
        handlerAnom.removeCallbacks(runnableCodeAnom);
    }

    //FUNCTION THAT USES EXPLICIT INTENT THAT RETURNS THE USER BACK TO THE HOME SCREEN
    public void goHome(){
        Intent myIntent = new Intent(View_Anom.this, View_Machines.class);
        startActivity(myIntent);
        Toast.makeText(getApplicationContext(), "Returned to Machines", Toast.LENGTH_SHORT).show();
        finish();
    }

    // our runable (handler) class, which is what we call every 25 sec or so, to perform the task of requesting information from our webservice
    private Runnable runnableCodeAnom = new Runnable() {
        @Override
        public void run() {
            // call our "pull from database" task here 
            new View_Anom.RetrieveFeedTask().execute();    
            Log.d("Handlers", "Anom Called on main thread");	// more logging
            // Repeat this the same runnable code block again every 25 seconds
            handlerAnom.postDelayed(runnableCodeAnom, Interval);
        }
    };

    // a class that handles getting anomalies from webservice
    class RetrieveFeedTask extends AsyncTask<Void, Void, String> {
        private Exception exception;

        protected void onPreExecute() {
            //progressBar.setVisibility(View.VISIBLE);
            //responseView.setText("");
        }


        protected String doInBackground(Void... urls) {
            // srt up an http request and connection
            try {
                // Reach out to our Web App on Predix Cloud,
                // get all the anomalies, which has not been reported (reported==0), of the machine with the given machine ID
                String api_url = APP_URL+"/?anom&param[]=machine_id&val[]=" + "%23" + machine_id+ "&param[]=reported&val[]=0";    // set to specific url "route"

                // create new url type
                URL url = new URL(api_url);
                // create a Http URL connection
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                //type--> dont know what type based on the curl command? use this website (it really helps):  http://curl.trillworks.com/#node
                urlConnection.setRequestMethod("GET"); // a GET request

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
                    String line; // content within each line
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
                Log.i("Anom FAIL INFO", response);
            }
            else {
                //progressBar.setVisibility(View.GONE);       //remove progress bar
                //responseView.setText("Here are the current machines with anomalies: ");
                Log.i("Anom WIN INFO", response);    //log info


                // get results from response, and display them in list  (listview or custom listview would be populated here)
                try {
                    if(response.trim().equals("Nothing was found.")){
                        Toast.makeText(getApplicationContext(), "Nothing was found.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    anoms = new JSONArray(response);    // get currently pulled response and put into array

                    // ***transform into loop to get all records; so that we can display them in list:::
                    for (int i = 0; i< anoms.length(); i++) {
                        JSONObject record = anoms.getJSONObject(i);

                        // retrieve certain data of the specific anomaly from the JSON object
                        String anomStrID = record.getString("sensor_id");
                        String anomStrDev = record.getString("deviation");

                        // populate the ArrayList with the info of this anomaly
                        anom_tempIDs.add(new Item("ID: "+anomStrID,"Deviation: " + anomStrDev));
                    }


                    //manually update adapter
                    Anomadapter.clear();
                    for (int j = 0; j < anom_tempIDs.size(); j++) {
                        Anomadapter.add(anom_tempIDs.get(j));
                    }
                    Anomadapter.notifyDataSetChanged();        //update adapter

                    anom_tempIDs.clear();      //clear temp array

                }
                catch(Exception e) {
                    Log.e("ERROR", e.getMessage(), e);      //otherwise, log the error
                }


            }

        }
    }


}
