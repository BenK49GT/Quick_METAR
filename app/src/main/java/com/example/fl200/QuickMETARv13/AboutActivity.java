package com.example.fl200.QuickMETARv13;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * Display an About Screen and apply style based on user selected theme
 */

public class AboutActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        //Get user selected theme from intent extras and set theme accordingly
        Intent intent = getIntent();
        int thm = intent.getIntExtra("theme", 0);

        if (thm == 0)   //night theme
        {
            setTheme(R.style.NightTheme);
        }
        else            //day theme
        {
            setTheme(R.style.DayTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_screen);

        final TextView txt         = (TextView) findViewById(R.id.textView);

        //Append the package name to show version of project
        txt.append("\n\nPackage: "+(this).getPackageName());

    }

    //Method to return user to Main Activity
    public void returnMeth(View v)
    {
        Intent i = new Intent(this,MainActivity.class);
        startActivity(i);
    }
}
