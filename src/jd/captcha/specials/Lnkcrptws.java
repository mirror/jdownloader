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

public class Lnkcrptws {

    private static boolean equalElements(int c, int c2) {
        return c == c2;
    }

    private static boolean isWhite(int c) {
        return c < 0 || c == 0xffffff;
    }

    /**
     * get objects with different color
     * 
     * @param grid
     * @return
     */
    public static java.util.List<PixelObject> getObjects(Captcha grid) {
        java.util.List<PixelObject> ret = new ArrayList<PixelObject>();
        java.util.List<PixelObject> merge;
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                int c = grid.getGrid()[x][y];
                if (isWhite(c)) continue;
                PixelObject n = new PixelObject(grid);
                n.add(x, y, c);
                merge = new ArrayList<PixelObject>();
                for (PixelObject o : ret) {
                    if (o.isTouching(x, y, true, 5, 5) && equalElements(c, o.getMostcolor())) {
                        merge.add(o);
                    }
                }
                if (merge.size() == 0) {
                    ret.add(n);
                } else if (merge.size() == 1) {
                    merge.get(0).add(n);
                } else {
                    for (PixelObject po : merge) {
                        ret.remove(po);
                        n.add(po);
                    }
                    ret.add(n);
                }
            }
        }
        return ret;
    }

    public static Letter[] getLetters(Captcha captcha) {
        java.util.List<PixelObject> ob = getObjects(captcha);
        // delete the lines
        for (Iterator<PixelObject> iterator = ob.iterator(); iterator.hasNext();) {
            PixelObject pixelObject = iterator.next();
            int ratio = pixelObject.getHeight() * 100 / pixelObject.getWidth();
            if (ratio > 105 || ratio < 95) iterator.remove();
        }
        Circle circle = new Circle(captcha, ob);
        circle.inBorder = 3;
        circle.outBorder = 2;
        circle.isElementColor = new Comparator<Integer>() {

            public int compare(Integer o1, Integer o2) {
                return o1.equals(o2) ? 1 : 0;
            }
        };
        // BasicWindow.showImage(captcha.getImage());
        // BasicWindow.showImage(circle.getOpenCircle().getImage());
        return circle.getOpenCirclePositionAsLetters();
    }
}
