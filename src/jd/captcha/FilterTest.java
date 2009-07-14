package jd.captcha;

import java.awt.image.BufferedImage;

import jd.captcha.gui.ScrollPaneWindow;
import jd.nutils.JDImage;

import com.jhlabs.image.BoxBlurFilter;
import com.jhlabs.image.TwirlFilter;

public class FilterTest {
    private static ScrollPaneWindow win;
    private static int i;

    public static void main(String args[]) {
        BufferedImage image = JDImage.getImage("logo/jd_logo_54_54");
      
        win = new ScrollPaneWindow();
        i = 0;
        win.setImage(0, i, image);
        win.setText(1, i, "Original");
        BoxBlurFilter blur = new com.jhlabs.image.BoxBlurFilter(2,2,3);
        BufferedImage dest = blur.createCompatibleDestImage(image, null);
       
        blur.filter(image, dest);
        i++;
        win.setImage(0, i, dest);
        win.setText(1, i, "BoxBlur");
        
        win.pack();
    }

   
}
