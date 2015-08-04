package org.jdownloader.captcha.v2.challenge.keycaptcha;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class CategoryData {
    private ArrayList<Image> images;

    public ArrayList<Image> getImages() {
        return images;
    }

    public BufferedImage getBackground() {
        return background;
    }

    public CategoryData() {
        images = new ArrayList<Image>();
    }

    private BufferedImage background;
    private String        resultUrl;
    private String        queryExtension;

    public String getQueryExtension() {
        return queryExtension;
    }

    public void setBackground(BufferedImage read) {
        this.background = read;
    }

    public void addImage(BufferedImage read) {
        images.add(read);
    }

    public void setResultUrl(String resultUrl) {
        this.resultUrl = resultUrl;
    }

    public String getResultUrl() {
        return resultUrl;
    }

    public void setQueryExtension(String additionalQuery) {
        this.queryExtension = additionalQuery;
    }

}
