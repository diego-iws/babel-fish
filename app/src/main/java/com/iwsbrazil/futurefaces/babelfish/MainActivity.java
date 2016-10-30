package com.iwsbrazil.futurefaces.babelfish;

import android.content.Intent;
import android.os.AsyncTask;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.iwsbrazil.futurefaces.babelfish.util.SpeechRecognizerHelper.createRecognizerIntent;
import static com.iwsbrazil.futurefaces.babelfish.util.SpeechRecognizerHelper.getErrorText;
import static com.iwsbrazil.futurefaces.babelfish.util.SpeechRecognizerHelper.setLanguage;
import static com.iwsbrazil.futurefaces.babelfish.util.TranslatorHelper.initTranslate;
import static com.iwsbrazil.futurefaces.babelfish.util.TranslatorHelper.translate;

public class MainActivity extends AppCompatActivity implements
        RecognitionListener, TextToSpeech.OnInitListener {

    private final static String LOG_TAG = "Voice";

    private FloatingActionButton buttonSpeak;
    private CoordinatorLayout parentLayout;

    private String userName;
    private String fullLanguage;
    private String translationLanguage;

    private SpeechRecognizer speechRecognizer = null;
    private TextToSpeech textToSpeech;
    private Intent recognizerIntent;

    private List<String> friends = new ArrayList<>();
    private Set<String> avoidDuplicates = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonSpeak = (FloatingActionButton) findViewById(R.id.button_speak);
        parentLayout = (CoordinatorLayout) findViewById(R.id.parent_layout);

        Intent caller = getIntent();
        userName = caller.getStringExtra("userName");
        fullLanguage = caller.getStringExtra("language");
        translationLanguage = fullLanguage.substring(0, 2);
        FirebaseManager.getInstance().addUser(userName);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(this);
        recognizerIntent = createRecognizerIntent(this);
        setLanguage(fullLanguage);

        initTranslate(this);
        textToSpeech = new TextToSpeech(this, this);

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

    public void textToSpeechPlay(final BabelMessage message) {

        new AsyncTask<BabelMessage, String, String>() {
            @Override
            protected String doInBackground(BabelMessage[] objects) {
                try {
                    BabelMessage msgin = objects[0];
                    String msg = msgin.getMessage();
                    String lcl = msgin.getLocale();

                    if (translationLanguage.equals(lcl)) {
                        return msg;
                    } else {
                        return translate(msg, lcl, translationLanguage);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String s) {
//                returnedText.setText(s);
                speakOut(s);
            }
        }.execute(message);
    }

    @Override
    public void onRmsChanged(float rmsdB) {
//        progressBar.setProgress((int) rmsdB);
    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            int result = textToSpeech.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
//                speakOut(returnedText.getText().toString());
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }

    private void speakOut(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        Log.d("SPEAK", text);
    }

    @Override
    public void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (userName != null) {
            FirebaseManager.getInstance().removeUser(userName);
        }
        super.onDestroy();
    }
}
