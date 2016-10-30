package com.iwsbrazil.futurefaces.babelfish.model;

/**
 * Created by administrador on 29/10/16.
 */

public class BabelMessage {

    private String message;
    private String locale;

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
