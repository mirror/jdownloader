package jd.captcha;

import java.awt.image.BufferedImage;

import jd.captcha.gui.ScrollPaneWindow;
import jd.nutils.JDImage;

import com.jhlabs.image.BoxBlurFilter;

public class FilterTest {

    public static void main(String args[]) {
        BufferedImage image = JDImage.getImage("logo/jd_logo_54_54");

        ScrollPaneWindow win = new ScrollPaneWindow();
        int i = 0;
        win.setImage(0, i, image);
        win.setText(1, i, "Original");
        BoxBlurFilter blur = new BoxBlurFilter(2, 2, 3);
        BufferedImage dest = blur.createCompatibleDestImage(image, null);

        blur.filter(image, dest);
        i++;
        win.setImage(0, i, dest);
        win.setText(1, i, "BoxBlur");

        win.pack();
    }

}
