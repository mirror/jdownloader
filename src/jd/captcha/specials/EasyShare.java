//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.awt.Color;

import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;

/**
 * 
 * 
 * @author JD-Team
 */
public class EasyShare {
    /*
    private static void mergeObjects(Vector<PixelObject> os) {
        for (PixelObject a : os) {
            for (PixelObject b : os) {
                if (a == b) continue;

                int xMin = Math.max(a.getXMin(), b.getXMin());
                int xMax = Math.min(a.getXMin() + a.getWidth(), b.getXMin() + b.getWidth());
                if (xMax <= xMin) continue;
                int yMin = Math.max(a.getYMin(), b.getYMin());
                int yMax = Math.min(a.getYMin() + a.getHeight(), b.getYMin() + b.getHeight());

                if (((xMax - xMin) < 30) && ((yMax - yMin) < 30)) {
                    a.add(b);
                    os.remove(b);
                    mergeObjects(os);
                    return;
                }
            }
        }

    }
*/
    private static void clean(Captcha captcha) {
        int[][] newgrid = new int[captcha.getWidth()][captcha.getHeight()];
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {

                int p = captcha.getPixelValue(x, y);

                Color c = new Color(p);
                if (c.getBlue() > 180 && c.getRed() > 180 && c.getGreen() > 180) {
                    PixelGrid.setPixelValue(x, y, newgrid, captcha.getMaxPixelValue());

                } else {
                    newgrid[x][y] = captcha.grid[x][y];
                }

            }
        }
        captcha.grid = newgrid;
    }
/*
    private static Vector<PixelObject> getRightletters(Vector<PixelObject> os, Captcha captcha) {
        if (os.size() > 5) return os;
        PixelObject biggest = os.get(0);
        for (int i = 1; i < os.size(); i++) {
            PixelObject po = os.get(i);
            if (po.getWidth() > biggest.getWidth()) biggest = po;
        }
        if (biggest.getWidth() > 20) {
            if (os.size() == 5 && biggest.getWidth() < 25) return os;
            PixelObject[] bs = biggest.cut(biggest.getWidth() / 2, biggest.getWidth(), 0);
            os.remove(biggest);

            for (PixelObject pixelObject : bs) {
                if (pixelObject != null) os.add(pixelObject);
            }
            return getRightletters(os, captcha);
        }
        return os;
    }
*/
    public static Letter[] getLetters(Captcha captcha) {
        // captcha.cleanByRGBDistance(1, 25);
        clean(captcha);
        captcha.removeSmallObjects(0.75, 0.75, 6);
        captcha.toBlackAndWhite(0.95);
        /*
        Vector<PixelObject> os = captcha.getObjects(0.5, 0.5);
        Collections.sort(os);
        mergeObjects(os);
        for (ListIterator<PixelObject> iterator = os.listIterator(os.size()); iterator.hasPrevious();) {
            PixelObject pixelObject = iterator.previous();
            if (pixelObject.getArea() < 30) {
                iterator.remove();
            }
        }
        os = getRightletters(os, captcha);
        Collections.sort(os);
        ArrayList<Letter> ret = new ArrayList<Letter>();
        for (PixelObject pixelObject : os) {
            if (pixelObject.getArea() > 30) {
                Letter let = pixelObject.toLetter();
                let.removeSmallObjects(0.75, 0.75, 6);
                let.resizetoHeight(25);
                ret.add(let);
            }
        }
        */
        return EasyCaptcha.getLetters(captcha);

    }

}