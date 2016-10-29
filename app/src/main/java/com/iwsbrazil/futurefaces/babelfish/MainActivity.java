package com.iwsbrazil.futurefaces.babelfish;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.translate.Translate;
import com.google.api.services.translate.model.TranslationsListResponse;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.iwsbrazil.futurefaces.babelfish.SpeechRecognizerHelper.createRecognizerIntent;
import static com.iwsbrazil.futurefaces.babelfish.SpeechRecognizerHelper.getErrorText;
import static com.iwsbrazil.futurefaces.babelfish.SpeechRecognizerHelper.setLanguage;

public class MainActivity extends AppCompatActivity implements
        RecognitionListener, TextToSpeech.OnInitListener {

    private TextView returnedText;
    private ToggleButton toggleButton;
    private ProgressBar progressBar;
    private Spinner spinner;

    private TextToSpeech textToSpeech;

    private SpeechRecognizer speechRecognizer = null;
    private String LOG_TAG = "Voice";
    private Intent recognizerIntent;

    private DatabaseReference firebase;

    private Translate translate;
    private String sourceLanguage;

    private List<String> friends = new ArrayList<>();

    private Set<String> avoidDuplicates = new HashSet<>();

    public Translate getTranslate() {
        if (translate == null) {
            HttpTransport httpTransport = new NetHttpTransport();
            JsonFactory jsonFactory = AndroidJsonFactory.getDefaultInstance();
            Translate.Builder translateBuilder = new Translate.Builder(httpTransport, jsonFactory, null);
            translateBuilder.setApplicationName(getString(R.string.app_name));
            this.translate = translateBuilder.build();
        }
        return translate;
    }

    private DatabaseReference getFirebase() {
        if (firebase == null) {
            firebase = FirebaseDatabase.getInstance().getReference();

            FirebaseAuth.getInstance().signInWithEmailAndPassword("teste@teste.com", "123456")
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            Log.d(LOG_TAG, "signIn:onComplete:" + task.isSuccessful());

                            if (task.isSuccessful()) {
                                Toast.makeText(MainActivity.this, "Sign In Sucessfull",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "Sign In Failed",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }

        return firebase;
    }

    public String translate(String text, String langFrom, String langTo) throws Exception {

        List<String> query = new ArrayList<>(Collections.singletonList(text));
        Translate.Translations.List list = getTranslate().translations().list(query, langTo);
        list.setKey(getString(R.string.google_api_key));
        list.setSource(langFrom);
        TranslationsListResponse translateResponse = list.execute();
        return translateResponse.getTranslations().get(0).getTranslatedText();
    }

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

        setUpSpinner();
        recognizerIntent = createRecognizerIntent(this);

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

    private void setUpSpinner() {
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
                String language = locales.get(i);
                sourceLanguage = language;
                setLanguage(language);
                textToSpeech.setLanguage(new Locale(language));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spinner.setSelection(0);
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
    public void onResults(final Bundle results) {
        Log.i(LOG_TAG, "onResults");
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        if (matches == null) return;

        String text = matches.get(0);

        if(text == null) return;
        if(text.isEmpty()) return;
        if(avoidDuplicates.contains(text)) return;
        avoidDuplicates.add(text);

        sendMessage(text);
    }

    public void sendMessage(String text) {

        EditText editText = (EditText) findViewById(R.id.user_name);
        final String myName = editText.getText().toString();

        final String room = "TestRoom";
        Message message = new Message();
        message.setMessage(text);
        message.setLocale(sourceLanguage.substring(0, 2));

        for (String name : friends) {
            if (!name.equals(myName)) {
                getFirebase().child(room).child("chat").child(name).child(String.valueOf(System.currentTimeMillis())).setValue(message);
            }
        }
    }

    public void textToSpeechPlay(final Message message) {

        new AsyncTask<Message, String, String>() {
            @Override
            protected String doInBackground(Message[] objects) {
                try {
                    Message msgin = objects[0];
                    String msg = msgin.getMessage();
                    String lcl = msgin.getLocale();

                    if(sourceLanguage.equals(lcl)) {
                        return msg;
                    } else {
                        return translate(msg, lcl, sourceLanguage);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String s) {
                returnedText.setText(s);
                speakOut(s);
            }
        }.execute(message);
    }

    public void joinChat(View view) {
        EditText editText = (EditText) findViewById(R.id.user_name);
        final String room = "TestRoom";
        final String name = editText.getText().toString();

        getFirebase().child(room).child("friends").child(name).setValue("name");

        getFirebase().child(room).child("chat").child(name).addChildEventListener(
                new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        Message message = dataSnapshot.getValue(Message.class);
                        textToSpeechPlay(message);
//                        dataSnapshot.getRef().removeValue();
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                }
        );

        getFirebase().child(room).child("friends").addChildEventListener(
                new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        friends.add(dataSnapshot.getKey());
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                }
        );

    }

    @Override
    public void onRmsChanged(float rmsdB) {
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
