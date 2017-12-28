package manda094.finalproject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Toast;
import static java.lang.Math.*;

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
import java.util.Date;
import java.util.List;


/**
 * CREATED BY TEAM DADTS AT BOSTON UNIV FOR THE COURSE CS 591 MOBILE APP DEV.
 * FINAL PROJECT APPLICATION
 *
 * THIS IS THE FUNCTIONALITY PAGE FOR THE VIEW MACHINES PAGE
 * ON THIS PAGE, USERS WILL SEE A LIST VIEW OF ALL THE MACHINES THAT HAVE ANOMALIES ASSOCIATED WITH THEM
 * ONLY MACHINES WITH ANOMALIES WILL BE DISPLAYED.
 *
 * TO SEE MORE INFORMATION ABOUT THE ANOMALIES, USERS CAN CLICK EACH ITEM IN THE LIST VIEW.
 *
 * ASSOCIATED XML LAYOUT IS VIEW_MACHINES
 */

public class View_Machines extends AppCompatActivity {

    // setting up time quantities to easily set intervals/ threasholds for when to update, etc...
    private int sec = 1000;
    private int hour = 3600*sec;
    private int day = 24*hour;

    private int Interval = 25*sec;       // how often to pull from database
    private int recentReportThresh = 3*day;     // what is considered to be a "recent report"

    ImageButton viewmachinehome_btn;

    private ListView lv;
    private ListView listView;
    ArrayList<Item> machine_ids;       // distinct machine_id list
    ArrayList<Item> temp_mIDs;		//used for updating the custom list view of machines

    JSONArray anoms;   //this is what we will use to store anomaly data for later display
    JSONArray machines;     // machines that were saved previously    (might be a good idea to save into preferences)

    String str_response;		// response from server/ requests  strings
    String value;
	
	//buttons
    Button submit;
    Button contacts;
	// adapters:
    ArrayAdapter<String> arrayAdapter;
    MyAdapter adapter;

    Handler handler;		// handler for countinous (with delay) tasks

	// url of our web-service application
    static final String APP_URL = "https://androidphp.run.aws-usw02-pr.ice.predix.io";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_machines);


        machine_ids = new ArrayList<Item>();
        temp_mIDs = new ArrayList<Item>();

        //
        // ** create handler class which will auto-refresh every 15-30 sec (or so) here:::
        new View_Machines.RetrieveFeedTask().execute();
        handler = new Handler();

		//set up adapter and list connections/ initialization:::
        // 1. pass context and data to the custom adapter
        adapter = new MyAdapter(this, machine_ids);

        // 2. Get ListView from activity_main.xml
        listView = (ListView) findViewById(R.id.lv);

        // 3. setListAdapter
        listView.setAdapter(adapter);

        //IMPLEMENTS VIEW MACHINE BUTTON AND FUNCTION THAT RETURNS THE USER BACK TO THE HOME SCREEN
        //SETS ONCLICKLISTERNER TO THE BUTTON
        viewmachinehome_btn = (ImageButton) findViewById(R.id.viewmachinehome_btn);
        viewmachinehome_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick (View v) {
                goHome();
            }
        });

        // create listener for items in list:
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // get text from item that user clicks
                TextView tv = (TextView)view.findViewById(R.id.label);
                String machine_text = tv.getText().toString();
				
                // create intent to go to View_anom page:::
                Intent seeAnom = new Intent(View_Machines.this, View_Anom.class);      //create new activity
                seeAnom.putExtra("machine", machine_text);		// put machine id into info for next activity 
                View_Machines.this.startActivity(seeAnom );


            }
        });

    }


    //FUNCTION THAT USES EXPLICIT INTENT THAT RETURNS THE USER BACK TO THE HOME SCREEN
    public void goHome(){
        Intent myIntent = new Intent(View_Machines.this, HomeScreen.class);
        startActivity(myIntent);
        Toast.makeText(getApplicationContext(), "Return to Home", Toast.LENGTH_SHORT).show();
    }

	// when we resume the activity, create the next delayed handler request
    @Override		 
    public void onResume() {
        super.onResume();
        handler.postDelayed(runnableMachines, Interval);
    }

// when we destory the activity, we want to remove all handler call-backs (in our case, this stops doing the "Every 25 sec interval- query the database")
    @Override		
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnableMachines);
    }


    // Just as we did on Destroy, we also have on pause; 
	//*** WE did this for a reason, which is that we do not wish to "spam" the employees with notifications after they left the page ***
	// this can be commented out, and run, but you have to close the entire activity to stop the constant spam; This was a personal design choice! ***
    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(runnableMachines);
    }

	// our runable class, which is what we call every 25 sec or so, to perform the task of requesting information from our webservice
    private Runnable runnableMachines = new Runnable() {
        @Override
        public void run() {
            // call our "pull from database" task here 
            new View_Machines.RetrieveFeedTask().execute();     
            Log.d("Handlers", "Called on main thread");		// more logging, because logging is great 
            // Repeat this the same runnable code block again every 25 seconds
            handler.postDelayed(runnableMachines, Interval);
        }
    };

	// Self-explanatory, this is the function that sends a notification to the user's phone
    private void sendNotification(String contentMsg){

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ge_logo)		// icon should be the GE logo 
                        .setContentTitle("New Anomalies")	// title
                        .setContentText(contentMsg);	// message of the notification
		
		// the usual notification set up:
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, mBuilder.build());		// we simply update the original notification, since we are setting the "notification id" to zero
    }


    // a class that handles getting anomalies from webservice
    class RetrieveFeedTask extends AsyncTask<Void, Void, String> {
        private Exception exception;

        protected void onPreExecute() {
		// left-over code from previous versions; this was used to show a progress bar of the requests working, as well as a response view; might be useable again
            //progressBar.setVisibility(View.VISIBLE);
            //responseView.setText("");
        }

			// do in background
        protected String doInBackground(Void... urls) {
            // let's try setting up an http request and connection
            try {
                // String api_url = APP_URL+"/?anom&param[]=reported&val[]=0";     --> previous code, can still be used for other versions.
                String api_url = APP_URL+"/api.php/?anom";    // set to specific url "route"

                URL url = new URL(api_url);     // create new url type
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                //type--> dont know what type based on the curl command? use this website (it really helps):  http://curl.trillworks.com/#node
                urlConnection.setRequestMethod("GET");    //this is a get request

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
                response = "Something went wrong. Please try again.";		//nothing found
                //responseView.setText(response);
            }
            else {
                //progressBar.setVisibility(View.GONE);       //remove progress bar
                //responseView.setText("Here are the current machines with anomalies: ");
                Log.i("INFO", response);    //log info

                // get results from response, and display them in list  (listview or custom listview would be populated here)
                try {
                    if( (response.trim().equals("Nothing was found.")) || (response == null) ){
                        Toast.makeText(getApplicationContext(), "Nothing was found.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    anoms = new JSONArray(response);    //set anoms array to newly currently pulled array

                    // check if anoms array is null:
                    if(anoms != null) {     // there are anomalies
                        // transform into loop to get all records
                        for (int i = 0; i < anoms.length(); i++) {
                            JSONObject record = anoms.getJSONObject(i);		//get json object from json array
                            String machineStr = record.getString("machine_id");		// get that objects "machine id" mapping value
                            String lastReportStr = record.getString("rep_date");	// get that objects "rep_date" (most recent report date) mapping value
                            //create distinct machine-id's list with last report date
                            if(!(lastReportStr.equals("null"))) {
                                temp_mIDs.add(new Item(machineStr, "Last reported on " + lastReportStr));		// if machine was reported at some point in the past
                            }
                            else{
                                temp_mIDs.add(new Item(machineStr, "Has not been reported yet!"));		// if machine has never been reported yet
                            }


                        }
                            // *** THIS PART IS ONLY FOR DETECTING CHANGES WITHIN THE ANOMALLY DATA SHOWN (difference between currently pulled vs previously pulled data!)
                        //check for if anoms (just recieved array) is different from machines (anoms array that was previously recieved):
                        if(machines != null) {          // previous anomally data is not null

                            ArrayList<String> alertArr = new ArrayList<String>();    // keeps track of which machines have been updated since the last pull

                            for (int i = 0; i < min(anoms.length(), machines.length()); i++) {
                                JSONObject record = anoms.getJSONObject(i);
                                String m_id = record.getString("machine_id");
                                String machineDate = record.getString("anom_date");    // get date of newly recieved data

                                JSONObject prevRecord = machines.getJSONObject(i);
                                // *first check: make sure that the machine in question was not reported recently:
                                String repDate = record.getString("rep_stamp");		// get time-stamp of most recent report for this machine
								
                               // check if report time-stamp of record is null OR not "recent"; recent is defined by comparing the current time ( System.currentTimeMillis())
							   // with the most recent reported time (which has to converted into a long type, parsed, and adjusted for into correct seconds);
							   // this is then checked to see if the difference between them is greater than some threashold (in our case, right now, this might be a few days)
							   // *** The reason for this, is to prevent "spamming" the user with too many notifications, especially if a machine was reported recently!
                                if ( (repDate.equals("null")) ||  ( ( System.currentTimeMillis() - ((Long.parseLong((repDate.split("\\.")[0])))*sec) )  > recentReportThresh ) )  {

                                    if (!prevRecord.isNull("anom_date")) {
                                        String prevMachineDate = prevRecord.getString("anom_date");

										// convert into proper integers (***this might have to be turned into longs to avoid error)
                                        int prevMdate = Integer.parseInt(prevMachineDate);
                                        int currMdate = Integer.parseInt(machineDate);
                                        //compare times of new and old "most recent" anomalies of each machine
                                        if (currMdate > prevMdate) {      // if it changed; that means we have new anomalies!

                                            //tell the user that a specific machine has new anomalies (after the pull)...
                                            // first add machine to array
                                            alertArr.add(m_id);


                                        }

                                    }

                                }
                            }
                            //check if array that contains that machine id's that have changed has a length greater than 0
                            if (alertArr.size() > 0){
                                String alertMsg = "Machines:  ";
                                for(int z =0; z < alertArr.size(); z++){
                                    alertMsg += alertArr.get(z);		// if yes, then append machines into alert message

                                }

                                alertMsg += " have New Anomalies!";
                                //alert the user...
                                //Toast.makeText(getApplicationContext(), alertMsg , Toast.LENGTH_LONG).show();
                                sendNotification(alertMsg);

                            }
                            else{
                                //sendNotification("No new anomalies.");   //--> Leave for maybe future implementations
                            }


                        }


                    }
                    else{     //if no anomalies are returned:
                        		
						Toast.makeText(getApplicationContext(), "no machines with anomalies found! Everything is running smoothly..." , Toast.LENGTH_LONG).show();

                    }


                    //manually update adapter
                    adapter.clear();
                    for (int j = 0; j < temp_mIDs.size(); j++) {
                        adapter.add(temp_mIDs.get(j));
                    }
                    adapter.notifyDataSetChanged();        //update adapter

                    temp_mIDs.clear();      //clear temp array
                    // "save" anoms array
                    machines = anoms;

                }
                catch(Exception e) {
                    Log.e("ERROR", e.getMessage(), e);      //otherwise, log the error
                }


            }

        }
    }






}
