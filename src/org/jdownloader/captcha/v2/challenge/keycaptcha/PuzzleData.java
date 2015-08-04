package org.jdownloader.captcha.v2.challenge.keycaptcha;

import java.util.LinkedHashMap;

public class PuzzleData {

    private KeyCaptchaImages images;

    public KeyCaptchaImages getImages() {
        return images;
    }

    public LinkedHashMap<String, int[]> getCoordinates() {
        return coordinates;
    }

    private LinkedHashMap<String, int[]> coordinates;
    private String                       mmUrlReq;

    public String getMmUrlReq() {
        return mmUrlReq;
    }

    public PuzzleData(LinkedHashMap<String, int[]> fmsImg, KeyCaptchaImages keyCaptchaImage, String mmUrlReq) {
        this.images = keyCaptchaImage;
        this.coordinates = fmsImg;
        this.mmUrlReq = mmUrlReq;
    }

}
