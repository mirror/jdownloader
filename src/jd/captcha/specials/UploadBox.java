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
public class UploadBox {
    private static void clean(Captcha captcha) {
        int[][] newgrid = new int[captcha.getWidth()][captcha.getHeight()];
        int dist = 2;
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {

                int p = captcha.getPixelValue(x, y);

                Color c = new Color(p);

                if (c.getBlue() > 250 && c.getRed() > 250 && c.getGreen() > 250) {
                    PixelGrid.setPixelValue(x, y, newgrid, Color.white.getRGB());
                } else {
                    if (c.getBlue() > 120 && c.getRed() > 120 && c.getGreen() > 120) {
                        int backg = 0;
                        int fordg = 0;
                        for (int x1 = x - dist; x1 < x + dist; x1++) {
                            for (int y1 = y - dist; y1 < y + dist; y1++) {
                                if (x1 > 0 && y1 > 0 && x1 < captcha.getWidth() && y1 < captcha.getHeight()) {
                                    int p1 = captcha.getPixelValue(x1, y1);

                                    Color c1 = new Color(p1);
                                    if (c1.getBlue() > 250 && c1.getRed() > 250 && c1.getGreen() > 250)
                                        backg++;
                                    else
                                        fordg++;
                                }
                            }
                        }
                        if (backg * 7 > fordg)
                            PixelGrid.setPixelValue(x, y, newgrid, 0xffffff);
                        else
                            PixelGrid.setPixelValue(x, y, newgrid, 0xff0000);

                    } else
                        PixelGrid.setPixelValue(x, y, newgrid, 0x000000);
                }

            }
        }
        captcha.grid = newgrid;

    }

    private static int getGapAt(Captcha captcha, int position) {
        int white = 0xffffff;
        int red = 0xff0000;

        int dist = 4;
        int lastBackground = -1;
        int gabX = position;
        for (int x = Math.max(0, position - dist); x < Math.min(position + dist, captcha.getWidth()); x++) {
            int background = 0;
            for (int y = 0; y < captcha.getHeight(); y++) {
                int col = captcha.getPixelValue(x, y);
                if (col == white)
                    background += 3;
                else if (col == red) background++;

            }
            if (lastBackground < background) {
                lastBackground = background;
                gabX = x;
            }
        }
        return gabX;
    }

    @SuppressWarnings("unused")
    private static int[] getGaps(Captcha captcha) {
        int x = 0;
        int white = 0xffffff;

        outer: for (; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {

                if (captcha.getPixelValue(x, y) != white) break outer;
            }
        }
        // System.out.println("asdf:" + x);
        int dist = 22;
        int[] gabs = new int[] { x = getGapAt(captcha, x + dist * 2), x = getGapAt(captcha, x + dist), x = getGapAt(captcha, x + dist), x = getGapAt(captcha, x + dist), x = getGapAt(captcha, x + dist), x = getGapAt(captcha, x + dist) };

        return gabs;
    }

    public static Letter[] getLetters(Captcha captcha) {

        clean(captcha);
        captcha.removeSmallObjects(0.75, 0.75, 6);
        // captcha.toBlackAndWhite(0.95);

        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {

                if (captcha.getPixelValue(x, y) == 0xff0000) captcha.setPixelValue(x, y, 0x000000);
            }
        }

        return EasyCaptcha.getLetters(captcha);

    }

}