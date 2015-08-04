package org.jdownloader.captcha.v2.challenge.keycaptcha;

import java.util.ArrayList;

public class KeyCaptchaPuzzleResponseData {

    private String             out;
    private ArrayList<Integer> mouseArray;

    public String getOut() {
        return out;
    }

    public ArrayList<Integer> getMouseArray() {
        return mouseArray;
    }

    public KeyCaptchaPuzzleResponseData(String position, ArrayList<Integer> mouseArray) {
        this.out = position;
        this.mouseArray = mouseArray;
    }

}
