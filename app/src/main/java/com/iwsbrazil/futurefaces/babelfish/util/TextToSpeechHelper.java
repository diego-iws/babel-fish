package com.iwsbrazil.futurefaces.babelfish.util;

import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.iwsbrazil.futurefaces.babelfish.MainActivity;
import com.iwsbrazil.futurefaces.babelfish.model.BabelMessage;

import java.util.Locale;

import static com.iwsbrazil.futurefaces.babelfish.util.TranslatorHelper.translate;

public class TextToSpeechHelper {

    private static TextToSpeechHelper instance;
    private static TextToSpeech textToSpeech;
    private static MainActivity activity;
    private String sender;

    public static TextToSpeechHelper getInstance(MainActivity mainActivity) {
        if (instance == null) {
            activity = mainActivity;
            instance = new TextToSpeechHelper();
            textToSpeech = new TextToSpeech(mainActivity, mainActivity);
            textToSpeech.setLanguage(Locale.US);
        }
        return instance;
    }

    public void textToSpeechPlay(final BabelMessage message, final String translationLanguage) {

        new AsyncTask<BabelMessage, String, String>() {
            @Override
            protected String doInBackground(BabelMessage[] objects) {
                try {
                    BabelMessage msgin = objects[0];
                    String msg = msgin.getMessage();
                    String lcl = msgin.getLocale();
                    sender = msgin.getSender();


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
                activity.updateAvatar(sender);
                speakOut(s);
            }
        }.execute(message);
    }

    private void speakOut(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        Log.d("SPEAK", text);
        activity.hideAvatar();
    }

    public void destroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    public void setLanguage(Locale locale) {
        textToSpeech.setLanguage(locale);
    }
}
