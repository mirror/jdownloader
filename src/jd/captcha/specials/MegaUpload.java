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

import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.nutils.Colors;

/**
 * 
 * 
 * @author JD-Team
 */
public class MegaUpload {
    private static final String[] STATICMATCH = new String[] { "qwertzuiopasdfghjklyxcvbnmQWERTZUIOPASDFGHJKLYXCVBNM", "qwertzuiopasdfghjklyxcvbnmQWERTZUIOPASDFGHJKLYXCVBNM", "qwertzuiopasdfghjklyxcvbnmQWERTZUIOPASDFGHJKLYXCVBNM", "1234567890" };

    public static Letter[] getLetters(Captcha captcha) {

        LetterComperator.MATCH_TABLE = STATICMATCH;

        getBorders(captcha);

        ArrayList<Letter> ret = new ArrayList<Letter>();
        for (int i = 0; i < 4; i++) {
            int averageWidth = Math.min(captcha.getWidth(), (captcha.getWidth() / (4 - i)) + 20);
            Letter first = new Letter(averageWidth, captcha.getHeight());

            first.setId(i);

            first.setOwner(captcha.owner);
            for (int x = 0; x < averageWidth; x++) {
                for (int y = 0; y < captcha.getHeight(); y++) {
                    try {
                        first.grid[x][y] = captcha.grid[x][y];
                    } catch (Exception e) {

                    }
                }
            }

            first = first.getSimplified(captcha.owner.getJas().getDouble("simplifyFaktor"));
            LetterComperator r = captcha.owner.getLetter(first);
            if (r == null) return null;

            first.detected = r;
            Letter b = r.getB();

            int[] offset = r.getPosition();

            if (offset == null) return null;
            offset[0] *= captcha.owner.getJas().getDouble("simplifyFaktor");
            offset[1] *= captcha.owner.getJas().getDouble("simplifyFaktor");

            ret.add(first);
            System.out.println(r.getDecodedValue() + "");
            if (i < 3) {
                captcha.crop(offset[0] + b.getWidth() / 2, 0, 0, 0);
                captcha.removeSmallObjects(0.95, 0.95, 15);
                captcha.clean();
            }
        }
        if (ret.size() < 4) return null;
        return ret.toArray(new Letter[] {});
    }

    public static void evaluate(LetterComperator comp, Double value) {
        int[] pos = comp.getPosition();
        if (pos == null) return;
        comp.setTmpExtensionError(pos[0] / 15.0);
    }

    private static void getBorders(Captcha captcha) {
        int[][] grid = new int[captcha.getWidth()][captcha.getHeight()];
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                int avg = captcha.getAverage(x, y, 2, 2);
                double dif = Math.min(Colors.getColorDifference(avg, 0), Colors.getColorDifference(avg, 0xffffff));

                if (dif > 43.0) {
                    grid[x][y] = 0;
                } else {
                    grid[x][y] = 0xffffff;
                }

            }
        }
        captcha.setGrid(grid);
        captcha.blurIt(2);
        captcha.toBlackAndWhite(0.99);

    }

    public static Letter[] letterFilter(Letter[] org, JAntiCaptcha jac) {
        return org;
    }

}