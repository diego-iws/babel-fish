package com.iwsbrazil.futurefaces.babelfish;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.iwsbrazil.futurefaces.babelfish.model.BabelMessage;
import com.iwsbrazil.futurefaces.babelfish.util.FirebaseManager;
import com.iwsbrazil.futurefaces.babelfish.util.TextToSpeechHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.iwsbrazil.futurefaces.babelfish.util.SpeechRecognizerHelper.createRecognizerIntent;
import static com.iwsbrazil.futurefaces.babelfish.util.SpeechRecognizerHelper.getErrorText;
import static com.iwsbrazil.futurefaces.babelfish.util.SpeechRecognizerHelper.setLanguage;
import static com.iwsbrazil.futurefaces.babelfish.util.TranslatorHelper.initTranslate;

public class MainActivity extends AppCompatActivity implements
        RecognitionListener, TextToSpeech.OnInitListener {

    private final static String LOG_TAG = "Voice";

    private CoordinatorLayout parentLayout;

    private String userName;
    private String translationLanguage;

    private SpeechRecognizer speechRecognizer = null;
    private Intent recognizerIntent;

    private List<String> friends = new ArrayList<>();
    private Set<String> avoidDuplicates = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        parentLayout = (CoordinatorLayout) findViewById(R.id.parent_layout);

        Intent caller = getIntent();
        userName = caller.getStringExtra("userName");
        String fullLanguage = caller.getStringExtra("language");
        translationLanguage = fullLanguage.substring(0, 2);
        TextToSpeechHelper.getInstance(this).setLanguage(new Locale(fullLanguage));
        FirebaseManager.getInstance().addUser(userName, this, fullLanguage);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(this);
        recognizerIntent = createRecognizerIntent(this);
        setLanguage(fullLanguage);

        initTranslate(this);

        FloatingActionButton buttonSpeak = (FloatingActionButton) findViewById(R.id.button_speak);
        buttonSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speechRecognizer.startListening(recognizerIntent);
            }
        });

        /*toggleButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setIndeterminate(true);
                    speechRecognizer.startListening(recognizerIntent);
                } else {
                    progressBar.setIndeterminate(false);
                    progressBar.setVisibility(View.INVISIBLE);
                    speechRecognizer.stopListening();
                }
            }
        });*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            Log.i(LOG_TAG, "destroy");
        }
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.i(LOG_TAG, "onBeginningOfSpeech");
//        progressBar.setIndeterminate(false);
//        progressBar.setMax(10);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived: " + buffer);
    }

    @Override
    public void onError(int errorCode) {
        String errorMessage = getErrorText(errorCode);
        Log.d(LOG_TAG, "FAILED " + errorMessage);
        Snackbar.make(parentLayout, errorMessage, Snackbar.LENGTH_LONG).show();
//        returnedText.setText(errorMessage);
//        toggleButton.setChecked(false);
    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
        Log.i(LOG_TAG, "onEvent");
    }

    @Override
    public void onPartialResults(Bundle arg0) {
        Log.i(LOG_TAG, "onPartialResults");
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
        Log.i(LOG_TAG, "onReadyForSpeech");
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");
//        progressBar.setIndeterminate(true);
//        toggleButton.setChecked(false);
        avoidDuplicates.clear();
    }

    @Override
    public void onResults(final Bundle results) {
        Log.i(LOG_TAG, "onResults");
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        if (matches == null) return;

        String text = matches.get(0);

        if (text == null) return;
        if (text.isEmpty()) return;
        if (avoidDuplicates.contains(text)) return;
        avoidDuplicates.add(text);

        sendMessage(text);
    }

    public void sendMessage(String text) {

        BabelMessage message = new BabelMessage();
        message.setMessage(text);
        message.setLocale(translationLanguage);

        for (String friendName : FirebaseManager.getInstance().getFriends()) {
            if (!friendName.equals(userName)) {
                FirebaseManager.getInstance().sendBabelMessage(friendName, message);
            }
        }
    }

    @Override
    public void onRmsChanged(float rmsdB) {
//        progressBar.setProgress((int) rmsdB);
    }

    @Override
    public void onDestroy() {
        TextToSpeechHelper.getInstance(this).destroy();

        if (userName != null) {
            FirebaseManager.getInstance().removeUser(userName);
        }
        super.onDestroy();
    }

    @Override
    public void onInit(int i) {}
}
