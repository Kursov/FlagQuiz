package com.kursov.flagquiz;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class QuizFragment extends Fragment {

    private static final String TAG = "FlagQuiz Activity";

    private static final int FLAGS_IN_QUIZ = 10;

    private List<String> fileNameList;
    private List<String> quizCountriesList;
    private Set<String> regionsSet;
    private String correctAnswer;
    private int totalGuesses;
    private int correctAnswers;
    private int guessRows;
    private SecureRandom random;
    private Handler handler;
    private Animation shakeAnimation;

    private TextView questionNumberTextView;
    private ImageView flagImageView;
    private LinearLayout[] guessLinearLayouts;
    private TextView answerTextView;

    private int greenDark;
    private int redLight;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_quiz, container, false);

        fileNameList = new ArrayList<>();
        quizCountriesList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();

        getColors();

        //download animation of incorrect answers
        shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3);

        //getting GUI components
        questionNumberTextView = (TextView)view.findViewById(R.id.questionNumberTextView);
        flagImageView = (ImageView)view.findViewById(R.id.flagImageView);
        guessLinearLayouts = new LinearLayout[3];
        guessLinearLayouts[0] = (LinearLayout)view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = (LinearLayout)view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = (LinearLayout)view.findViewById(R.id.row3LinearLayout);
        answerTextView = (TextView)view.findViewById(R.id.answerTextView);

        //Configure listeners for answer buttons
        for (LinearLayout row : guessLinearLayouts){
            for (int column = 0; column < row.getChildCount(); column++) {
                Button button = (Button)row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }

        // set text questionNumberTextView
        questionNumberTextView.setText(getResources().getString(R.string.question, 1, FLAGS_IN_QUIZ));
        return view;
    }

    private void getColors() {
        Resources res = getResources();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            greenDark = res.getColor(android.R.color.holo_green_dark, null);
            redLight = res.getColor(android.R.color.holo_red_light, null);
        } else {
            greenDark = res.getColor(android.R.color.holo_green_dark);
            redLight = res.getColor(android.R.color.holo_red_light);
        }
    }

    //update guessRows using SharedPreferences
    public void updateGuessRows(SharedPreferences sharedPreferences){

        //Get the number of displayed options
        String choices = sharedPreferences.getString(MainActivity.CHOICES, null);
        guessRows = Integer.parseInt(choices) / 3;

        //hide all components LinearLayout with choice buttons
        for (LinearLayout layout : guessLinearLayouts)
            layout.setVisibility(View.INVISIBLE);

        //Display the required amount of components
        for (int row = 0; row < guessRows; row++) {
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
        }
    }

    //Update selected regions using data from SharedPreferences
    public void updateRegions(SharedPreferences sharedPreferences){
        regionsSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);
    }

    //Configure and start the next series of questions
    public void resetQuiz(){
        //using AssetManager to get names of image files
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear();

        try {
            //Iterate all regions
            for (String region : regionsSet){
                //getting list of all image files for regions
                String[] paths = assets.list(region);

                for (String path : paths)
                    fileNameList.add(path.replace(".png", ""));
            }
        }
        catch (IOException exception)
        {
            Log.e(TAG, "Error loading image file names", exception);
        }

        correctAnswers = 0;
        totalGuesses = 0;
        quizCountriesList.clear();

        int flagCounter = 1;
        int numberOfFlags = fileNameList.size();

        //Adding FLAGS_IN_QUIZ random file names in quizCountriesList
        while (flagCounter <= FLAGS_IN_QUIZ){
            int randomIndex = random.nextInt(numberOfFlags);

            //Getting a random file name
            String fileName = fileNameList.get(randomIndex);

            //If the region is included, but has not yet been selected
            if (!quizCountriesList.contains(fileName)){
                quizCountriesList.add(fileName);
                ++flagCounter;
            }
        }

        loadNextFlag();
    }

    //download next flag after correct answer
    private void loadNextFlag(){
        //Getting of the next flag filename and delete it from the list
        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage;
        answerTextView.setText("");

        //Displays the current question number
        questionNumberTextView.setText(
                getResources().getString(R.string.question, (correctAnswers + 1), FLAGS_IN_QUIZ));

        //Extracting a region from the name of the next image
        String region = nextImage.substring(0, nextImage.indexOf('-'));

        //Using AssetManager to load the next image
        AssetManager assets = getActivity().getAssets();

        try {
            //Getting InputStream object for a resource of the next flag
            InputStream stream = assets.open(region + "/" + nextImage + ".png");

            //Loading graphic as a Drawable object and output to flagImageView
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            flagImageView.setImageDrawable(flag);
        }
        catch (IOException exception) {
            Log.e(TAG, "Error loading " + nextImage, exception);
        }

        Collections.shuffle(fileNameList);

        //Putting the correct answer in the end fileNameList
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        //Adding 3, 6 or 9 buttons depending on the value guessRows
        for (int row = 0; row < guessRows; row++) {
            //Placing buttons in currentTableRow
            for (int column = 0; column < guessLinearLayouts[row].getChildCount(); column++) {
                //Get reference to Button
                Button newGuessButton = (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                //Assigning country name text newGuessButton
                String fileName = fileNameList.get((row * 3) + column);
                newGuessButton.setText(getCountryName(fileName));
            }
        }

        //Random replacement of a button the correct answer
        int row = random.nextInt(guessRows);
        int column = random.nextInt(3);
        LinearLayout randomRow = guessLinearLayouts[row];
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);
    }

    //The method identifies the filename of the country name
    private String getCountryName(String name)
    {
        return name.substring(name.indexOf('-') + 1).replace('_', ' ');
    }

    private OnClickListener guessButtonListener = new OnClickListener(){

        @Override
        public void onClick(View v) {
            Button guessButton = ((Button) v);
            String guess = guessButton.getText().toString();
            String answer = getCountryName(correctAnswer);
            ++totalGuesses;

            if (guess.equals(answer)){
                ++correctAnswers;

                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(greenDark);
                //answerTextView.setTextColor(getResources().getColor(R.color.correct_answer));

                disableButtons();

                if (correctAnswers == FLAGS_IN_QUIZ){
                    DialogFragment quizResults = new DialogFragment(){
                        @Override
                        public Dialog onCreateDialog(Bundle bundle) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setCancelable(false);

                            builder.setMessage(getResources().getString(R.string.results,
                                    totalGuesses, (1000 / (double) totalGuesses)));

                            builder.setPositiveButton(R.string.reset_quiz,
                                    new DialogInterface.OnClickListener(){

                                        public void onClick(DialogInterface dialog, int id) {
                                            resetQuiz();
                                        }
                                    }
                            );
                            return  builder.create();
                        }
                    };
                    quizResults.show(getFragmentManager(), "quiz results");
                } else {
                    handler.postDelayed(
                            new Runnable() {
                                @Override
                                public void run() {
                                    loadNextFlag();
                                }
                            }, 2000);
                }
            } else {
                flagImageView.startAnimation(shakeAnimation);
                answerTextView.setText(R.string.incorrect_answer);
                answerTextView.setTextColor(redLight);
                //answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer));
                guessButton.setEnabled(false);
            }
        }
    };

    private void disableButtons(){
        for (int row = 0; row < guessRows; row++) {
            LinearLayout guessRow = guessLinearLayouts[row];
            for (int i = 0; i < guessRow.getChildCount(); i++) {
                guessRow.getChildAt(i).setEnabled(false);
            }
        }
    }
}
