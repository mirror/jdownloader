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

import jd.captcha.easy.BackGroundImageManager;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.nutils.Colors;

public class XflsKcm {
    static boolean isRgb(int color) {
        return color==0xffffff;
    }
    static private int follow(int x, int y, int cx, int cy, Captcha captcha) {
        int ret = 0;
        if (captcha.getHeight()>y&&captcha.getWidth()>x&&Colors.getColorDifference(0xffffff, captcha.grid[x][y]) > 30) {
            ret++;
            if (cx >= 0) {
                ret += follow(x + 1, y, cx - 1, cy, captcha);
            }
            if (cy >= 0) {
                ret += follow(x, y + 1, cx, cy - 1, captcha);
            }
        }
        return ret;
    }

    static void clearlines2(Captcha captcha, int val) {

        int[][] grid = PixelGrid.getGridCopy(captcha.grid);
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
//                System.out.println(follow(x, y, 3, 3, captcha));
                    if (follow(x, y, 2, 2, captcha) < val) grid[x][y] = 0xffffff;
            }
        }
        captcha.grid=grid;
    }

    /**
     * overwrite the colored dots in digits with black dots die bunten punkte in
     * den Zahlen werden mit schwarzen punkten ersetzt
     * 
     * @param captcha
     */
    static void setDotsInDigits(Captcha captcha) {

        int[][] grid = PixelGrid.getGridCopy(captcha.grid);
        for (int x = 1; x < captcha.getWidth() - 1; x++) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (isRgb(captcha.grid[x][y])) {
                    int co;
                    int w = (co = captcha.grid[x + 1][y]) != 0xffffff && !isRgb(co) ? 1 : 0;
                    w += (co = captcha.grid[x + 1][y + 1]) != 0xffffff && !isRgb(co) ? 1 : 0;
                    w += (co = captcha.grid[x][y + 1]) != 0xffffff && !isRgb(co) ? 1 : 0;
                    w += (co = captcha.grid[x - 1][y + 1]) != 0xffffff && !isRgb(co) ? 1 : 0;
                    w += (co = captcha.grid[x - 1][y - 1]) != 0xffffff && !isRgb(co) ? 1 : 0;
                    w += (co = captcha.grid[x + 1][y - 1]) != 0xffffff && !isRgb(co) ? 1 : 0;
                    w += (co = captcha.grid[x - 1][y]) != 0xffffff && !isRgb(co) ? 1 : 0;
                    w += (co = captcha.grid[x][y - 1]) != 0xffffff && !isRgb(co) ? 1 : 0;
                    if (w > 4) grid[x][y] = 0x000000;

                }
            }
        }
        captcha.grid = grid;
    }

    static void toBlack(Captcha captcha) {
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != -1) {
                    captcha.grid[x][y] = 0x000000;
                }
                else
                    captcha.grid[x][y] =0xffffff;

            }
        }
    }

    public static Letter[] getLetters(Captcha captcha) {

        BackGroundImageManager bgit = new BackGroundImageManager(captcha);
        bgit.clearCaptchaAll();
        
//        BasicWindow.showImage(captcha.getImage());
        toBlack(captcha);


        setDotsInDigits(captcha);

        clearlines2(captcha, 4);
        clearlines2(captcha, 4);
        captcha.removeSmallObjects(0.5, 0.5, 8);

//        clearlines2(captcha, 8);


//        captcha.removeSmallObjects(0.5, 0.5, 7);
        Letter[] lets = EasyCaptcha.getLetters(captcha);

        return lets;
    }
}
