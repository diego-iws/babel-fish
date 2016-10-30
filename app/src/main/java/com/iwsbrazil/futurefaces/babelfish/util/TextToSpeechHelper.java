package com.iwsbrazil.futurefaces.babelfish.util;

import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.iwsbrazil.futurefaces.babelfish.MainActivity;
import com.iwsbrazil.futurefaces.babelfish.model.BabelMessage;

import java.util.Locale;

import static com.iwsbrazil.futurefaces.babelfish.util.TranslatorHelper.translate;

/**
 * Created by administrador on 30/10/16.
 */

public class TextToSpeechHelper {

    private static TextToSpeechHelper instance;
    private static TextToSpeech textToSpeech;

    public static TextToSpeechHelper getInstance(MainActivity context) {
        if (instance ==  null) {
            instance = new TextToSpeechHelper();
            textToSpeech = new TextToSpeech(context, context);
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

    private void speakOut(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        Log.d("SPEAK", text);
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
