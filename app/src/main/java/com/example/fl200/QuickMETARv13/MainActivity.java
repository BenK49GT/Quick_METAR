package com.example.fl200.QuickMETARv13;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.Menu;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.util.Log;
import android.view.MenuItem;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.lang.String;

public class MainActivity extends AppCompatActivity
{
    //Declare Globals
    private static int menu_selected = 1;   //User selected theme (0-Dark or 1-Light)
    public static String home_list = "";    //User home airports list
    final String TAG = "MainActivity";      //Used for log filtering
    public static String curMetars;         //Any currently displayed METARs
    public static boolean firstRun = true;  //Prevent unwanted re-calculations on rotations

    //themeSetter
    /*
    This method will, when called, determine the screen orientation.
    Based on screen orientation and the global menu_selected it will
    set the background to the appropriate background image.
    */
    public void themeSetter()
    {
        //hide the action bar if the user has specified a home list
        if(home_list != "")
        {
            //Disabling the following due to rapid deflate on pixel 3
            //getSupportActionBar().hide();
        }
        else
        {
            getSupportActionBar().show();

        }
        //Get screen orientation
        int orientation = getResources().getConfiguration().orientation;
        Log.v(TAG, "menu_selected = "+menu_selected + "\norientation = "+orientation);

        final RelativeLayout layout = (RelativeLayout) findViewById(R.id.relLayout);
        TextView cText = (TextView) findViewById(R.id.textView2);
        EditText airpID = (EditText) findViewById((R.id.editText2));
        cText.setMovementMethod(new ScrollingMovementMethod());

        if(orientation==1) //Portrait Orientation
        {
            if (menu_selected==0) //Night Theme
            {
                layout.setBackgroundResource(R.drawable.drk_ver);
                cText.setText(curMetars);
                cText.setTextColor(Color.GREEN);
                airpID.setTextColor(Color.RED);
                airpID.setHintTextColor(Color.RED);
            }
            else //Day Theme
            {
                layout.setBackgroundResource(R.drawable.lgt_ver);
                cText.setText(curMetars);
                cText.setTextColor(Color.BLACK);
                airpID.setTextColor(Color.WHITE);
                airpID.setHintTextColor(Color.BLACK);
            }
        }
        else //Landscape Orientation
        {
            if (menu_selected==0) //Night Theme
            {
                layout.setBackgroundResource(R.drawable.drk_hor);
                cText.setText(curMetars);
                cText.setTextColor(Color.GREEN);
                airpID.setTextColor(Color.RED);
                airpID.setHintTextColor(Color.RED);
            }
            else //Day Theme
            {
                layout.setBackgroundResource(R.drawable.lgt_hor);
                cText.setText(curMetars);
                cText.setTextColor(Color.BLACK);
                airpID.setTextColor(Color.WHITE);
                airpID.setHintTextColor(Color.BLACK);
            }
        }
    }
    //Build URL based on user submitted airport ID & call aSync task to get weather
    public void getMetar(String id)
    {

        String urlBeg   = "https://www.aviationweather.gov/adds/dataserver_current/httpparam?dataSource=metars&requestType=retrieve&format=xml&stationString=";

        String urlEnd   = "&hoursBeforeNow=1";
        String url = urlBeg+id+urlEnd;
        Log.v(TAG, "url ="+url);

        new asyncMetarClass().execute(url);
    }

    //Hide Keyboard if user pressed button without pressing keyboard checkmark first
    public static void hideSoftKeyboard(Activity activity)
    {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                activity.getCurrentFocus().getWindowToken(), 0);
    }

    //Get users input from quick entry EditText and add METAR to screen
    public void getQuickWeather(View v)
    {
        //Hide Keyboard if user pressed button without pressing keyboard checkmark first
        hideSoftKeyboard(this);

        //Create handle for editText field
        EditText airpID = (EditText) findViewById((R.id.editText2));
        airpID.setMovementMethod(new ScrollingMovementMethod());

        //Get text from editText field
        String airp = airpID.getText().toString();

        //Clear editText field after use
        airpID.setText("");

        if(airp.length() >= 3)        //Only initiate weather retrieval if enough characters exist
        {
            getMetar(airp);
        }
        else //Display toast to explain process to user
        {
            Context context     = getApplicationContext();
            CharSequence text   = "Touch 'Enter Airport ID' first.";
            int duration        = Toast.LENGTH_LONG;
            Toast toast         = Toast.makeText(context, text, duration);
            toast.show();

            //clear screen since getMetar isn't called, prevent ghost weather after rotation
            getCurMetars();
        }
    }

    //Method to clear currently displayed METARs
    public void clearScr()
    {
        ((TextView) findViewById(R.id.textView2)).setText("");
    }

    //didTouch
    /*
    Method to show the action bar if user touches the screen.
    After a delay the bar will be hidden.
    */
    public void didTouch(View v)
    {
        getSupportActionBar().show();

        //Hide the keyboard if the screen is touched
        hideSoftKeyboard(this);


        final Handler handler = new Handler();
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                //hide the action bar if the user has specified a home list
                if(home_list != "")
                {
                    //Disabling the following due to rapid deflate on pixel 3

                    //getSupportActionBar().hide();
                }
           }
        }, 3000);   //duration to display the action bar
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get user's home list of airports and preferred theme from preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String stored_home_list = preferences.getString("storedHome","");
        int stored_theme_setting = preferences.getInt("storedTheme",0);

        //Call function to update global variable for home airports
        updateHomeList(stored_home_list);

        if((home_list == "") && (firstRun)) //Display Message to explain adding home airports
        {
            Context context     = getApplicationContext();
            CharSequence text   = "No home airports set.\nSelect 'Edit Home List' from Action Bar";
            int duration        = Toast.LENGTH_LONG;
            int yOffset         = 200;
            Toast toast         = Toast.makeText(context, text, duration);
            toast.setGravity(Gravity.TOP, 0, yOffset);
            toast.show();
            menu_selected   = stored_theme_setting;
            firstRun        = false;    //Prevent extraneous fetching of weather

            themeSetter();

        }
        else if(firstRun)  //Initiate action to get weather from server
        {
            firstRun        = false;    //Prevent extraneous fetching of weather
            menu_selected   = stored_theme_setting;
            getMetar(home_list);    //Fetch weather for saved home airports
            themeSetter();
        }

        //hide the action bar if the user has specified a home list
        if(home_list != "")
        {
            //Disabling the following due to rapid deflate on pixel 3

            //getSupportActionBar().hide();
        }
        else
        {
            getSupportActionBar().show();

        }
        //Create orientation listener to change background image on device rotation
        OrientationEventListener oEL = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL)
        {
            @Override
            public void onOrientationChanged(int orientation)
            {
                //Set the background images based on state of button
                Log.v(TAG, "menu_selected onOrientationChanged ="+menu_selected);
                themeSetter();
            }
        };

        //Enable/Disable the event listener
        if (oEL.canDetectOrientation()) oEL.enable();
        else oEL.disable();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //Update Global variable for home_list and store in preferences file
    public String updateHomeList(String newHomeList)
    {
        Log.v(TAG, "updateHomeList called with ="+newHomeList);

        if (newHomeList == "")
        {
            return home_list;
        }

        //Update Global variable
        home_list = newHomeList;

        //Store new home list in preferences file
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("storedHome", home_list);
        editor.apply();

        return home_list;
    }

    //Create dialog box with current list of home airports and allow user to modify
    public void editHomeList()
    {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("Home Airports");
        alertDialog.setMessage("Enter Airport IDs separated by spaces");

        final EditText input = new EditText(MainActivity.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT|
                InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS|
                InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);

        //Set EditText to stored value, reformat to caps & spaces
        input.setText(home_list.replaceAll("%20", " ").toUpperCase());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);

        alertDialog.setIcon(R.mipmap.ic_launcher);

        alertDialog.setPositiveButton("DONE",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        String newHomeList;

                        newHomeList = input.getText().toString();
                        Log.v(TAG, "raw home list =" + newHomeList);

                        //Replace spaces for URL formatting
                        newHomeList = newHomeList.replaceAll(" ", "%20").toLowerCase();
                        Log.v(TAG, "new home list ="+newHomeList);

                        //Call function to update global & preferences
                        updateHomeList(newHomeList);

                        //Clear Screen and get weather for new home list
                        clearScr();
                        if(newHomeList.length() >= 3)
                        {
                            getMetar(newHomeList);
                        }
                        else //clear screen since getMetar isn't called, prevent ghost weather after rotation
                        {
                            getCurMetars();
                        }
                        themeSetter();
                    }
                });

        alertDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });

        alertDialog.show();
    }

    //Set Global variable curMetars to current value of TextView, used on orientation and theme changes
    public void getCurMetars()
    {
        TextView eText = (TextView) findViewById(R.id.textView2);
        eText.setMovementMethod(new ScrollingMovementMethod());

        curMetars = eText.getText().toString();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();

        switch (item.getItemId())
        {

            case R.id.action_home:
                editHomeList();     //update home list
                themeSetter();      //call themeSetter to set user defined theme
                return true;

            case R.id.action_nighttime:
                menu_selected = 0;  //change global to reflect user selection
                themeSetter();      //call themeSetter to set user defined theme
                Log.v(TAG, "menu_selected set to 0 ="+menu_selected);

                //Store new theme selection in preferences file
                editor.putInt("storedTheme", menu_selected);
                editor.apply();

                return true;

            case R.id.action_daytime:
                menu_selected = 1;  //change global to reflect user selection
                themeSetter();      //call themeSetter to set user defined theme
                Log.v(TAG, "menu_selected set to 1 ="+menu_selected);

                //Store new theme selection in preferences file
                editor.putInt("storedTheme", menu_selected);
                editor.apply();
                return true;

            case R.id.action_about:
                /*
                //display about_screen layout and then return to activity_main layout
                setContentView(R.layout.about_screen);

                //hide the action bar

                getSupportActionBar().hide();

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        setContentView(R.layout.activity_main); //return to activity_main
                        themeSetter();  //call themeSetter to get back to user defined theme
                    }
                }, 3000);   //duration to display about screen
                */
                Intent i = new Intent(this,AboutActivity.class);
                i.putExtra("theme", menu_selected);
                startActivity(i);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    //Async Task to retrieve xml from dataserver
    private class asyncMetarClass extends AsyncTask<String , Void ,String>
    {
        private String server_response;

        private String readStream(InputStream is) throws IOException
        {
            StringBuilder sb = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(is),1000);
            for (String line = r.readLine(); line != null; line =r.readLine())
            {
                sb.append(line);
            }
            is.close();
            return sb.toString();
        }

        @Override
        protected String doInBackground(String... strings)
        {
            URL url;
            HttpURLConnection urlConnection = null;
            try
            {
                url = new URL(strings[0]);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setInstanceFollowRedirects(true);

                int responseCode = urlConnection.getResponseCode();
                Log.v(TAG, "responseCode ="+ responseCode);
                if(responseCode == HttpURLConnection.HTTP_OK)
                {
                    server_response = readStream(urlConnection.getInputStream());
                    Log.v("CatalogClient", server_response);
                }
                else
                {
                    //server_response = "NULL RESPONSE FROM SERVER";
                    server_response = readStream(urlConnection.getInputStream());
                    Log.e(TAG,"NOT OK server_response ="+ server_response);

                    Log.e(TAG, "getResponseMessage response ="+ urlConnection.getResponseMessage());
                    Log.e(TAG, "getURL response ="+ urlConnection.getURL());
                    Log.e(TAG, "getHeaderField location response ="+ urlConnection.getHeaderField("Location"));
                    Log.e(TAG, "getInstanceFollowRedirects response ="+ urlConnection.getInstanceFollowRedirects());
                }
            }
            catch (MalformedURLException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s)
        {
            super.onPostExecute(s);
            Log.e(TAG, "server_response = " + server_response);
            if (server_response == null)
            {
                Log.e(TAG, "NULL server_response = " );

                return;
            }

            //Tags identifying beggining and end of raw METAR
            final String START_TAG = "<raw_text>";
            final String END_TAG = "</raw_text>";

            boolean didResolve = false; //used to suppress resolve error in the case of multiple IDs

            TextView metarRep = (TextView) findViewById(R.id.textView2);
            int startI  = server_response.indexOf(START_TAG) + START_TAG.length();
            int endI    = server_response.indexOf(END_TAG) - 1;

            Log.e(TAG, "startI = " + startI);
            Log.e(TAG, "endI = " + endI);

            //Iterate through server_response and append METARs to textView element
            while(startI>10 && endI>20)
            {
                didResolve = true;

                //Prevent duplicate METARS
                if (!metarRep.getText().toString().contains(server_response.substring(startI,endI)))
                {
                    //append METAR with bullet
                    metarRep.append( "\n"+"\u2022 " + server_response.substring(startI, endI) + "\u2022" + "\n");
                }
                Log.e(TAG, "appended with = " + server_response.substring(startI,endI));
                server_response = server_response.substring(endI+END_TAG.length(), server_response.length());

                startI  = server_response.indexOf(START_TAG) + START_TAG.length();
                endI    = server_response.indexOf(END_TAG) - 1;
                Log.e(TAG, "truncated server_resp = " + server_response);
            }

            //Prompt user if no airport ID is resolved
            if ((startI<10 || endI<20) && !didResolve)
            {
                Context context = getApplicationContext();
                CharSequence text = "Unable to Resolve Airport ID";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
            //Store most recent METAR report to global variable; used for rotation and theme changes
            getCurMetars();
        }
    }
}
