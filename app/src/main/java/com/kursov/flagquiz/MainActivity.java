package com.kursov.flagquiz;

import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends Activity {

    //keys needed to reading SharedPreferences
    public static final String CHOICES = "pref_numberOfChoices";
    public static final String REGIONS = "pref_regionsToInclude";

    //toggle portrait mode
    private boolean phoneDevice = true;
    // check preferences
    private boolean preferencesChanged = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //default settings of SharedPreferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //registration of listener for changes of SharedPreferences
        PreferenceManager.getDefaultSharedPreferences(this).
                registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        //screen size definition
        int screenSize = getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;

        //if tablet set false for phoneDevice
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
                screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE)
            phoneDevice = false;

        //if phone set portrait orientation
        if (phoneDevice)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (preferencesChanged)
        {
            //after the configuration has been set by default,
            //initialize QuizFragment and run a quiz
            QuizFragment quizFragment = (QuizFragment)
                    getFragmentManager().findFragmentById(R.id.quizFragment);
            quizFragment.
                    updateGuessRows(
                            PreferenceManager.
                                    getDefaultSharedPreferences(getApplicationContext()));
            quizFragment.updateRegions(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.resetQuiz();
            preferencesChanged = false;
        }
    }

    //Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getting Display object of current screen
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        Point screenSize = new Point();
        display.getRealSize(screenSize);

        //menu is displayed only in portrait orientation
        if (screenSize.x < screenSize.y)
        {
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        }
        else
            return false;
    }

    //Launch the SettingsActivity when running on the phone
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent preferencesIntent = new Intent(this, SettingsActivity.class);
        startActivity(preferencesIntent);
        return super.onOptionsItemSelected(item);
    }

    //listener of changes in configuration of SharedPreferences
    private OnSharedPreferenceChangeListener preferenceChangeListener =
            new OnSharedPreferenceChangeListener()
            {
                //calls when configuration changed by user
                @Override
                public void onSharedPreferenceChanged(
                        SharedPreferences sharedPreferences, String key)
                {
                    preferencesChanged = true;

                    QuizFragment quizFragment = (QuizFragment)
                            getFragmentManager().findFragmentById(R.id.quizFragment);

                    if (key.equals(CHOICES))
                    {
                        quizFragment.updateGuessRows(sharedPreferences);
                        quizFragment.resetQuiz();
                    }
                    else if (key.equals(REGIONS))
                    {
                        Set<String> regions = sharedPreferences.getStringSet(REGIONS, null);

                        if (regions != null && regions.size() > 0)
                        {
                            quizFragment.updateRegions(sharedPreferences);
                            quizFragment.resetQuiz();
                        }
                        else
                        {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            assert regions != null;
                            regions.add(getResources().getString(R.string.default_region));
                            editor.putStringSet(REGIONS, regions);
                            editor.commit();
                            Toast.makeText(MainActivity.this,
                                    R.string.default_region_message, Toast.LENGTH_SHORT).show();
                        }
                    }

                    Toast.makeText(MainActivity.this,
                            R.string.restarting_quiz, Toast.LENGTH_SHORT).show();
                }
            };
}
