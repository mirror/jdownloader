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

import java.util.Collections;
import java.util.Vector;

import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;
import jd.nutils.Colors;

public class HstjNt {
    static void clearlines(Captcha captcha) {

        int[][] grid = PixelGrid.getGridCopy(captcha.grid);
        for (int x = 1; x < captcha.getWidth() - 1; x++) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (captcha.grid[x][y] == 0x000000) {
                    int w = captcha.grid[x + 1][y] == 0x000000 ? 1 : 0;
                    w += captcha.grid[x + 1][y + 1] == 0x000000 ? 1 : 0;
                    w += captcha.grid[x][y + 1] == 0x000000 ? 1 : 0;
                    w += captcha.grid[x - 1][y + 1] == 0x000000 ? 1 : 0;
                    w += captcha.grid[x - 1][y - 1] == 0x000000 ? 1 : 0;
                    w += captcha.grid[x + 1][y - 1] == 0x000000 ? 1 : 0;
                    w += captcha.grid[x - 1][y] == 0x000000 ? 1 : 0;
                    w += captcha.grid[x][y - 1] == 0x000000 ? 1 : 0;
                    if (w < 2)
                        grid[x][y] = 0xffffff;
                    else if (w < 3 && (captcha.grid[x + 1][y] == 0x000000 && captcha.grid[x - 1][y] == 0x000000)) grid[x][y] = 0xffffff;
                }
            }
        }
        captcha.grid = grid;
    }

    static void toBlack(Captcha captcha) {
        try {
            int color = captcha.getAverage();

            for (int x = 0; x < captcha.getWidth(); x++) {
                for (int y = 0; y < captcha.getHeight(); y++) {
                    // System.out.println(Colors.getRGBColorDifference2(captcha.grid[x][y],
                    // color));
                    if (Colors.getRGBColorDifference2(captcha.grid[x][y], color) > 25) {
                        captcha.grid[x][y] = 0x000000;
                    } else
                        captcha.grid[x][y] = 0xffffff;

                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        }

    }

    private static void blurIt(Captcha captcha, int factor) {
        int[][] grid = PixelGrid.getGridCopy(captcha.grid);

        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] == 0x000000) {
                    for (int i = 0; i < factor && (x + i) < captcha.getWidth(); i++) {
                        for (int j = 0; j < factor && (y + j) < captcha.getHeight(); j++) {
                            grid[x + i][y + j] = 0x000000;
                            if ((x - i) >= 0 && (y - j) >= 0) grid[x - i][y - j] = 0x000000;

                        }
                    }
                }
            }

        }
        captcha.grid = grid;
    }

    public static Letter[] getLetters(Captcha captcha) throws InterruptedException{

        toBlack(captcha);
        captcha.crop(2, 2, 2, 2);
        // BasicWindow.showImage(captcha.getImage());

        clearlines(captcha);

        captcha.removeSmallObjects(0.7, 0.7, 95);
        blurIt(captcha, 3);
        Vector<PixelObject> os = captcha.getObjects(0.5, 0.5);
        Collections.sort(os);
        Letter[] lets = new Letter[os.size()];
        if (os.size() == 4) {
            for (int i = 0; i < lets.length; i++) {
                lets[i] = os.get(i).toLetter();
                lets[i].resizetoHeight(35);
            }
        } else {
            lets = EasyCaptcha.getLetters(captcha);
            for (Letter letter : lets) {
                letter.resizetoHeight(35);
            }
        }
        return lets;
    }
}
