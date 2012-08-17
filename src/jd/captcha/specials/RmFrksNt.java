//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

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
        java.util.List<PixelObject> ob = ColorObjects.getObjects(captcha, 80, 15);
        // delete the lines
        for (Iterator<PixelObject> iterator = ob.iterator(); iterator.hasNext();) {
            PixelObject pixelObject = iterator.next();
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
