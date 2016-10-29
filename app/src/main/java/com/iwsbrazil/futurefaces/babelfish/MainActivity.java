package com.iwsbrazil.futurefaces.babelfish;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.iwsbrazil.futurefaces.babelfish.SpeechRecognizerHelper.createRecognizerIntent;
import static com.iwsbrazil.futurefaces.babelfish.SpeechRecognizerHelper.getErrorText;
import static com.iwsbrazil.futurefaces.babelfish.SpeechRecognizerHelper.setLanguage;

public class MainActivity extends AppCompatActivity implements
        RecognitionListener,  TextToSpeech.OnInitListener {

    private TextView returnedText;
    private ToggleButton toggleButton;
    private ProgressBar progressBar;
    private Spinner spinner;

    private TextToSpeech textToSpeech;

    private SpeechRecognizer speechRecognizer = null;
    private String LOG_TAG = "Voice";
    private Intent recognizerIntent;

//    private static Translate translate;
//
//    public static Translate getTranslate() {
//        if (translate == null) {
//            translate = TranslateOptions.builder().apiKey("AIzaSyBQXIDyiEqjOSSoFdFGECMD-GUSzUgshg4").build().service();
//        }
//        return translate;
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        returnedText = (TextView) findViewById(R.id.textView1);
        progressBar = (ProgressBar) findViewById(R.id.progressBar1);
        toggleButton = (ToggleButton) findViewById(R.id.toggleButton1);

        progressBar.setVisibility(View.INVISIBLE);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(this);

        textToSpeech = new TextToSpeech(this, this);

        setUpLanguages();
        recognizerIntent = createRecognizerIntent(this, spinner.getSelectedItem().toString());

        toggleButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {

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
        });
    }

    private void setUpLanguages() {
        spinner = (Spinner) findViewById(R.id.spinner);
        final List<String> locales = new ArrayList<>(Arrays.asList(
                Locale.ENGLISH.toLanguageTag(),
                Locale.FRANCE.toLanguageTag(),
                Locale.ITALIAN.toLanguageTag(),
                Locale.GERMAN.toLanguageTag(),
                "pt-BR"));
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, locales);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setLanguage(locales.get(i));
                textToSpeech.setLanguage(new Locale(locales.get(i)));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spinner.setSelection(0);
    }

    @Override
    public void onResume() {
        super.onResume();
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
        progressBar.setIndeterminate(false);
        progressBar.setMax(10);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived: " + buffer);
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");
        progressBar.setIndeterminate(true);
        toggleButton.setChecked(false);
    }

    @Override
    public void onError(int errorCode) {
        String errorMessage = getErrorText(errorCode);
        Log.d(LOG_TAG, "FAILED " + errorMessage);
        returnedText.setText(errorMessage);
        toggleButton.setChecked(false);
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
    public void onResults(Bundle results) {
        Log.i(LOG_TAG, "onResults");
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        String text = matches.get(0);

        // Translates some text into English
        //Translation translation = getTranslate().translate(text, Translate.TranslateOption.sourceLanguage("pt"), Translate.TranslateOption.targetLanguage("en"));
        //returnedText.setText(text + "\n" + translation.translatedText());
        returnedText.setText(text);

        speakOut(text);
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
        progressBar.setProgress((int) rmsdB);
    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            int result = textToSpeech.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
                speakOut(returnedText.getText().toString());
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
        super.onDestroy();
    }
}