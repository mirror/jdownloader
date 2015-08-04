package org.jdownloader.captcha.v2.challenge.keycaptcha.dialog;

import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.images.IconIO;

public class CatOption {

    private Image         image;
    private int           selected;
    private BufferedImage gray;

    public CatOption(int i, Image img) {
        this.image = img;
        selected = 1;

        gray = ImageProvider.convertToGrayScale(IconIO.toBufferedImage(IconIO.getTransparent(image, 0.2f)));

    }

    public Icon getIcon(int i) {
        if (selected == i) {
            return new ImageIcon(image);
        } else {
            return new ImageIcon(gray);
        }
    }

    public boolean setSelected(int i) {
        selected = i;
        return true;
    }

}
