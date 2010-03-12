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

import java.util.ArrayList;
import java.util.Collections;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;

/**
 * 
 * 
 * @author JD-Team
 */
public class DuckLoad {
    private static int follow(PixelGrid captcha, int x, int y, int color, int lastC, int distance) {
        int i = 0;
        if (x >= 0 && y >= 0 && captcha.getWidth() > x && captcha.getHeight() > y && captcha.grid[x][y] >= 0 && captcha.grid[x][y] != 0xffffff && captcha.grid[x][y] == lastC) {
            captcha.grid[x][y] = color;
            i++;
            for (int x1 = -distance; x1 <= distance; x1++) {
                for (int y1 = -distance; y1 <= distance; y1++) {
                    i += follow(captcha, x + x1, y + y1, color, lastC, distance);

                }
            }

        }
        return i;
    }
    private static void toBlack(PixelGrid captcha) {
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff)
                    captcha.grid[x][y] = 0x000000;

            }
        }
    }
    public static Letter[] getLetters(Captcha captcha) {
        int i = -2;
        class colorD implements Comparable<colorD> {
            public int color;
            public int anzahl;

            public colorD(int color, int anzahl) {
                super();
                this.color = color;
                this.anzahl = anzahl;
            }

            public int compareTo(colorD o) {
                return new Integer(o.anzahl).compareTo(anzahl);
            }

        }
        ArrayList<colorD> colors = new ArrayList<colorD>();
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    colors.add(new colorD(i, follow(captcha, x, y, i, captcha.grid[x][y], 6)));
                    i--;
                }

            }
        }
        Collections.sort(colors);
        for (colorD colorD : colors) {
            if (colorD.anzahl <= 200) {
                for (int x = 0; x < captcha.getWidth(); x++) {
                    for (int y = 0; y < captcha.getHeight(); y++) {
                        if (captcha.grid[x][y] == colorD.color) {
                            captcha.grid[x][y] = 0xffffff;
                        }
                    }
                }
            }
        }
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff)
                    captcha.grid[x][y] *= -500;
            }
        }
        ArrayList<PixelObject> coLetters = new ArrayList<PixelObject>();
        for (int x = 0; x < captcha.getWidth(); x++) {
            outerY: for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    for (PixelObject pixelObject : coLetters) {
                        if (pixelObject.getAverage() == captcha.grid[x][y]) {
                            pixelObject.add(x, y, captcha.grid[x][y]);
                            continue outerY;
                        }
                    }
                    PixelObject pix = new PixelObject(captcha);
                    pix.add(x, y, captcha.grid[x][y]);
                    coLetters.add(pix);

                }
            }
        }
        Letter[] letters = new Letter[coLetters.size()];
        Collections.sort(coLetters);
        i = 0;
        for (PixelObject pixelObject : coLetters) {
            Letter let = pixelObject.toLetter();
            toBlack(let);
            let.removeSmallObjects(0.9, 0.9, 4, 2, 2);
            let.clean();
//            let.autoAlign();
            let.resizetoHeight(20);
            letters[i] = let;
            i++;
        }
        return letters;

    }

}