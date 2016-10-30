package com.iwsbrazil.futurefaces.babelfish.util;

import android.content.Context;
import android.text.Html;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.translate.Translate;
import com.google.api.services.translate.model.TranslationsListResponse;
import com.iwsbrazil.futurefaces.babelfish.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by administrador on 30/10/16.
 */

public class TranslatorHelper {

    private static Translate translate;
    private static Context context;

    public static void initTranslate(Context argContext) {
        if (translate == null) {
            context = argContext;
            HttpTransport httpTransport = new NetHttpTransport();
            JsonFactory jsonFactory = AndroidJsonFactory.getDefaultInstance();
            Translate.Builder translateBuilder = new Translate.Builder(httpTransport, jsonFactory, null);
            translateBuilder.setApplicationName(context.getString(R.string.app_name));
            translate = translateBuilder.build();
        }
    }

    public static String translate(String text, String langFrom, String langTo) throws Exception {
        List<String> query = new ArrayList<>(Collections.singletonList(text));
        Translate.Translations.List list = translate.translations().list(query, langTo);
        list.setKey(context.getString(R.string.google_translate_key));
        list.setSource(langFrom);
        TranslationsListResponse translateResponse = list.execute();
        String response = translateResponse.getTranslations().get(0).getTranslatedText();
        /* Fixes encoding bug with single, double quotes, ... */
        return Html.fromHtml(response).toString();
    }
}
