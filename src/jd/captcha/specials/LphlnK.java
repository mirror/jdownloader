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
import java.util.List;
import java.util.Vector;

import jd.captcha.LetterComperator;
import jd.captcha.easy.BackGroundImageManager;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;

public class LphlnK {
    private static void clearlines(Captcha captcha) {

        int[][] grid = PixelGrid.getGridCopy(captcha.grid);
        for (int x = 1; x < captcha.getWidth() - 1; x++) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (captcha.grid[x][y] == 0x000000) {
                    int w = captcha.grid[x + 1][y] != -16751002 ? 1 : 0;
                    w += captcha.grid[x + 1][y + 1] != -16751002 ? 1 : 0;
                    w += captcha.grid[x][y + 1] != -16751002 ? 1 : 0;
                    w += captcha.grid[x - 1][y + 1] != -16751002 ? 1 : 0;
                    w += captcha.grid[x - 1][y - 1] != -16751002 ? 1 : 0;
                    w += captcha.grid[x + 1][y - 1] != -16751002 ? 1 : 0;
                    w += captcha.grid[x - 1][y] != -16751002 ? 1 : 0;
                    w += captcha.grid[x][y - 1] != -16751002 ? 1 : 0;
                    if (w < 4) grid[x][y] = -16751002;

                }
            }
        }
        captcha.grid = grid;
    }

    private static void toBlack(Captcha captcha) {
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != -16751002) {
                    captcha.grid[x][y] = 0x000000;
                } else
                    captcha.grid[x][y] = 0xffffff;

            }
        }
    }

    public static void mergeos(PixelObject aos, List<PixelObject> os) {
        os.remove(aos);
        PixelObject nextos = os.get(0);
        int best;
        int matching = Math.min((aos.getXMax()), (nextos.getXMax())) - Math.max(aos.getXMin(), nextos.getXMin());
        if (matching >= 0)
            best = -matching;
        else
            best = Math.min((Math.min(Math.abs((aos.getXMin() + aos.getWidth()) - (nextos.getXMin())), Math.abs((aos.getXMin()) - (nextos.getXMin() + nextos.getWidth())))), (Math.min(Math.abs(aos.getXMin() - nextos.getXMin()), Math.abs((aos.getXMin() + aos.getWidth()) - (nextos.getXMin() + nextos.getWidth())))));
        for (int i = 1; i < os.size(); i++) {
            PixelObject b = os.get(i);
            int ib;
            matching = Math.min((aos.getXMax()), (b.getXMax())) - Math.max(aos.getXMin(), b.getXMin());
            if (matching >= 0)
                ib = -matching;
            else
                ib = Math.min((Math.min(Math.abs((aos.getXMin() + aos.getWidth()) - (b.getXMin())), Math.abs((aos.getXMin()) - (b.getXMin() + b.getWidth())))), (Math.min(Math.abs(aos.getXMin() - b.getXMin()), Math.abs((aos.getXMin() + aos.getWidth()) - (b.getXMin() + b.getWidth())))));

            if (ib < best) {
                best = ib;
                nextos = b;
            }
        }
        if (best < 4)
            nextos.add(aos);
        else {
            os.add(aos);
            aos.detected = new LetterComperator(null, null);
        }
    }

    private static void merge(Vector<PixelObject> os, Captcha captcha) {
        for (PixelObject pixelObject : os) {
            if (pixelObject.getArea() < 195 && pixelObject.detected == null) {
                mergeos(pixelObject, os);
                merge(os, captcha);
                break;
            }
        }
        for (PixelObject pixelObject : os) {
            pixelObject.detected = null;
        }

    }

    public static Letter[] getLetters(Captcha captcha) throws InterruptedException{
        // BasicWindow.showImage(captcha.getImage());
        BackGroundImageManager bgit = new BackGroundImageManager(captcha);
        bgit.clearCaptchaAll();

        clearlines(captcha);
        clearlines(captcha);
        clearlines(captcha);
        toBlack(captcha);
        // BasicWindow.showImage(captcha.getImage());

        captcha.crop(2, 2, 2, 2);
        captcha.removeSmallObjects(0.5, 0.5, 7);
        // BasicWindow.showImage(captcha.getImage());
        Vector<PixelObject> os = captcha.getObjects(0.5, 0.5);
        merge(os, captcha);
        Collections.sort(os);
        // EasyCaptcha.mergeObjectsBasic(os, captcha, 0);
        Letter[] lets = new Letter[os.size()];
        for (int i = 0; i < lets.length; i++) {
            lets[i] = os.get(i).toLetter();

        }
        for (Letter letter : lets) {
            letter.resizetoHeight(30);
            letter.betterAlign(10, 10);
        }
        return lets;
    }
}
