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

import jd.captcha.easy.BackGroundImageManager;
import jd.captcha.gui.BasicWindow;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;
import jd.nutils.Colors;

public class CrpTm {

    static boolean equalElements(int c, int c2) {
        if (isWhite(c) || isWhite(c2)) return false;
        if (c == 0x000000 || c2 == 0x000000) return c == c2;
        int[] hsvC = Colors.rgb2hsv(c);
        int[] hsvC2 = Colors.rgb2hsv(c2);
        // TODO The "hsvC[1] / hsvC2[2] == 1" is repeated twice
        // Is it a typo? Was a different comparison meant in the second place?
        return (hsvC[0] == hsvC2[0] && (hsvC[1] == hsvC2[1] || hsvC[2] == hsvC2[2] || hsvC[1] / hsvC2[2] == 1 || hsvC[1] / hsvC2[2] == 1)) && Colors.getRGBColorDifference2(c, c2) < 80;
    }

    static boolean isWhite(int c) {
        return c < 0 || c == 0xffffff;
    }

    static java.util.List<PixelObject> getObjects(Captcha grid) {
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
                    if (o.isTouching(x, y, true, 3, 3) && equalElements(c, o.getMostcolor())) {
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
        merge(ret);
        return ret;
    }

    static void merge(java.util.List<PixelObject> list) {
        for (PixelObject po : list) {
            if (po.getSize() < 20 || (po.getWidth() != po.getHeight() && po.getSize() < 180)) {
                java.util.List<PixelObject> merge = new ArrayList<PixelObject>();
                int most = po.getMostcolor();
                int[] hsvC = Colors.rgb2hsv(most);
                for (PixelObject po2 : list) {
                    if (po2 != po) {
                        int[] hsvC2 = Colors.rgb2hsv(po2.getMostcolor());

                        if (hsvC[0] == hsvC2[0] && ((po2.getWidth() != po2.getHeight() || po2.getSize() < 180 || (po2.getLocation()[0] == po.getLocation()[0] && po2.getLocation()[1] == po.getLocation()[1])) && po2.isTouching(po, true, 14, 14))) {
                            // TODO

                            merge.add(po2);
                        }
                    }
                }
                if (merge.size() == 1) {
                    list.remove(po);
                    merge.get(0).add(po);
                    merge(list);
                    return;
                }
                if (merge.size() > 0) {
                    for (PixelObject po3 : merge) {
                        list.remove(po3);
                        po.add(po3);
                    }
                    // BasicWindow.showImage(po.toColoredLetter().getImage());
                    merge(list);

                    return;
                }
            }
        }

    }

    public static Letter[] getLetters(Captcha captcha) {

        // Graphics g = image.getGraphics();
        // g.setColor(Color.black);
        // g.drawOval(55, 55, 18, 18);

        BackGroundImageManager bgit = new BackGroundImageManager(captcha);
        bgit.clearCaptchaAll();
        BasicWindow.showImage(captcha.getImage());
        java.util.List<PixelObject> ob = getObjects(captcha);
        Circle circle = new Circle(captcha, ob);
        Letter best = circle.getOpenCircle();
        if (best != null) BasicWindow.showImage(best.getImage(), "best");
        return null;
    }
}
