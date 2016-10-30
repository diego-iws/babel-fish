package com.iwsbrazil.futurefaces.babelfish;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.iwsbrazil.futurefaces.babelfish.util.FirebaseManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity implements OnCompleteListener<AuthResult> {

    private Spinner spinner;
    private EditText editText;
    private String language;
    private LinearLayout parentLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        parentLayout = (LinearLayout) findViewById(R.id.parent_layout);
        editText = (EditText) findViewById(R.id.user_name);
        spinner = (Spinner) findViewById(R.id.spinner);

        setUpSpinner();
    }

    private void setUpSpinner() {
        spinner = (Spinner) findViewById(R.id.spinner);

        //Available locales for the app
        Locale localePtBr = new Locale("pt", "BR");
        List<Locale> locales = new ArrayList<>(Arrays.asList(
                Locale.ENGLISH,
                Locale.FRANCE,
                Locale.ITALIAN,
                Locale.GERMAN,
                localePtBr));

        final List<String> spinnerArray = new ArrayList<>();
        final HashMap<String, String> spinnerMap = new HashMap<>();
        for (int i = 0; i < locales.size(); i++) {
            spinnerArray.add(locales.get(i).getDisplayName());
            spinnerMap.put(locales.get(i).getDisplayName(), locales.get(i).toLanguageTag());
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, spinnerArray);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                language = spinnerMap.get(spinnerArray.get(i));
                /* SpeechToText and TextToSpeech need the Locale Tag format */
//                setLanguage(language);
                /* In the babel message only the translate format is necessary */
//                srcDestLanguage = language.substring(0, 2);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spinner.setSelection(0);
    }

    public void joinChat(View view) {
        String userName = editText.getText().toString();
        if (userName.isEmpty()) {
            Snackbar.make(parentLayout, getString(R.string.user_name_null), Snackbar.LENGTH_SHORT).show();
            return;
        }

        FirebaseManager.getInstance().doLogin(this);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("userName", userName);
        intent.putExtra("language", language);
        startActivity(intent);
    }

    @Override
    public void onComplete(@NonNull Task<AuthResult> task) {
        Log.d("doLogin", "signIn:onComplete:" + task.isSuccessful());

        if (task.isSuccessful()) {
            Snackbar.make(parentLayout, "Sign In Sucessfull", Snackbar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(parentLayout, "Sign In Failed", Snackbar.LENGTH_SHORT).show();
        }
    }
}
