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
import java.util.List;

import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;

public class UgtF {
    static private int follow(int x, int y, int cx, int cy, Captcha captcha) {
        int ret = 0;
        if (captcha.getHeight()>y&&captcha.getWidth()>x&&captcha.grid[x][y] != 0xffffff) {
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

    static void clearlines(Captcha captcha) {

        int[][] grid = PixelGrid.getGridCopy(captcha.grid);
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
//                System.out.println(follow(x, y, 3, 3, captcha));
                    if (follow(x, y, 2, 2, captcha) < 46) grid[x][y] = 0xffffff;
            }
        }
        captcha.grid=grid;
    }

    private static void toBlack(Captcha captcha) {
        for (int x = 1; x < captcha.getWidth() - 1; x++) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    captcha.grid[x][y] = 0x000000;
                }

            }
        }
    }

    public static Letter[] getLetters(Captcha captcha) {

        double dist =12;
        // List<PixelObject> obs2 = ColorObjects.getObjects(captcha,40, 7);
        List<PixelObject> obs2 = ColoredObject.getObjects(captcha, dist, 2,2);

        PixelObject biggest = null;
        for (PixelObject o : obs2) {
            if(biggest==null)
                biggest=o;
            else if(o.getSize()<biggest.getSize())
            {
             biggest=o;   
            }
        }
        captcha.grid=biggest.toColoredLetter().grid;
        toBlack(captcha);
        clearlines(captcha);

        captcha.setOrgGrid(captcha.grid);
//BasicWindow.showImage(captcha.getImage());
        return EasyCaptcha.getLetters(captcha);
    }
}
