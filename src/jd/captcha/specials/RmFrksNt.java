package jd.captcha.specials;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;
import jd.nutils.Colors;

public class RmFrksNt {

    public static Letter[] getLetters(Captcha captcha) {
        ArrayList<PixelObject> ob = ColorObjects.getObjects(captcha, 80, 15);
        // delete the lines
        for (Iterator<PixelObject> iterator = ob.iterator(); iterator.hasNext();) {
            PixelObject pixelObject = (PixelObject) iterator.next();
            int ratio = pixelObject.getHeight() * 100 / pixelObject.getWidth();
            if (ratio > 105 || ratio < 95) iterator.remove();
        }
        Circle circle = new Circle(captcha, ob);
        circle.inBorder = 2;
        circle.outBorder = 4;
        circle.isElementColor = new Comparator<Integer>() {

            public int compare(Integer o1, Integer o2) {
                return Colors.getColorDifference(o1, o2) < 80 ? 1 : 0;
            }
        };
        // BasicWindow.showImage(captcha.getImage());
        // BasicWindow.showImage(circle.getOpenCircle().getImage());
        return circle.getOpenCirclePositionAsLetters();
    }
}
