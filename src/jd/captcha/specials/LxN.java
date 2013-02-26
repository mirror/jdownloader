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

import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.nutils.Colors;

public class LxN {
    static void clearlines(Captcha captcha) {

        int[][] grid = PixelGrid.getGridCopy(captcha.grid);
        for (int x = 1; x < captcha.getWidth() - 1; x++) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (Colors.rgb2cmyk(captcha.grid[x][y])[3] > 20) {
                    int w = captcha.grid[x + 1][y] == 0x000000 ? 1 : 0;
                    w += captcha.grid[x + 1][y + 1] == 0x000000 ? 1 : 0;
                    w += captcha.grid[x][y + 1] == 0x000000 ? 1 : 0;
                    w += captcha.grid[x - 1][y + 1] == 0x000000 ? 1 : 0;
                    w += captcha.grid[x - 1][y - 1] == 0x000000 ? 1 : 0;
                    w += captcha.grid[x + 1][y - 1] == 0x000000 ? 1 : 0;
                    w += captcha.grid[x - 1][y] == 0x000000 ? 1 : 0;
                    w += captcha.grid[x][y - 1] == 0x000000 ? 1 : 0;
                    if (w < 4) grid[x][y] = 0xffffff;

                }
            }
        }
        captcha.grid = grid;
    }

    static void toBlack(Captcha captcha) {
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (Colors.rgb2cmyk(captcha.grid[x][y])[3] > 20) {
                    captcha.grid[x][y] = 0x000000;
                } else
                    captcha.grid[x][y] = 0xffffff;

            }
        }
    }

    public static Letter[] getLetters(Captcha captcha) throws InterruptedException{
        // BasicWindow.showImage(captcha.getImage());
        toBlack(captcha);

        clearlines(captcha);

        captcha.removeSmallObjects(0.7, 0.7, 95);

        return EasyCaptcha.getLetters(captcha);
    }
}
