package manda094.finalproject;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/*
CREATED BY TEAM DADTS AT BOSTON UNIV FOR THE COURSE CS 591 MOBILE APP DEV.
FINAL PROJECT APPLICATION


This page is the functionality page for the HOME SCREEN. It lays out each button and is the basis of the
application. Users can access any part of the application from this screen.

 */

public class HomeScreen extends AppCompatActivity {

    Button logout_btn, sendreport_btn, viewmachine_btn, contactpage_btn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

       //DEFINES LOG OUT BUTTON TO RETURN TO LOG IN PAGE
        logout_btn = (Button) findViewById(R.id.logout_btn);
        logout_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }});

        //DEFINES SEND REPORT BUTTON TO GO TO SEND REPORT PAGE
        sendreport_btn = (Button) findViewById(R.id.sendReport_btn);
        sendreport_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v){
                goToSendReport();
            }
        });


        //DEFINES MACHINE REPORT BUTTON AND LEADS TO VIEW MACHINES PAGE
        viewmachine_btn = (Button) findViewById(R.id.viewmachine_btn);
        viewmachine_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v){
                goToViewMachines();
            }
        });


        //DEFINES CONTACT PAGE BUTTON TO GOES TO CONTACT LIST PAGE
        contactpage_btn = (Button) findViewById(R.id.contactpage_btn);
        contactpage_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v){
                goToContact();
            }
        });

    }

    //FUNCTIONS THAT USE EXPLICIT INTENTS THAT LEAD TO THE NEXT ACTIVITY.

    public void logout(){
        Intent myIntent = new Intent(HomeScreen.this, login_activity.class);
        startActivity(myIntent);
        Toast.makeText(getApplicationContext(), "LOGGING OUT", Toast.LENGTH_SHORT).show();
    }
    public void goToSendReport(){
        Intent myIntent = new Intent(HomeScreen.this, Report_page.class);
        startActivity(myIntent);
    }
    public void goToContact(){
        Intent myIntent = new Intent(HomeScreen.this, allcontact_page.class);
        startActivity(myIntent);
    }
    public void goToViewMachines(){
        Intent myIntent = new Intent(HomeScreen.this, View_Machines.class);
        startActivity(myIntent);
    }


}
