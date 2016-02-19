package org.jdownloader.captcha.v2.challenge.keycaptcha;

import java.util.ArrayList;

import org.appwork.storage.Storable;

public class KeyCaptchaApiResponse implements Storable {
    public KeyCaptchaApiResponse(/* Storable */) {
    }

    private ArrayList<Integer> mouseArray;

    public ArrayList<Integer> getMouseArray() {
        return mouseArray;
    }

    public void setMouseArray(ArrayList<Integer> mouseArray) {
        this.mouseArray = mouseArray;
    }

    public String getOut() {
        return out;
    }

    public void setOut(String cout) {
        this.out = cout;
    }

    private String out;
}
