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

import jd.captcha.utils.UTILITIES;

import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;

/**
 * 
 * 
 * @author JD-Team
 */
public class gwarez {
    
    public static Letter[] getLetters(Captcha captcha) {
//        captcha.cleanBackgroundByColor(captcha.getAverage());
        captcha.cleanWithDetailMask(captcha.owner.createCaptcha(UTILITIES.loadImage(captcha.owner.getResourceFile("mask.png"))), 1, 8);
        captcha.clean();
//        captcha.removeSmallObjects(0.8, 0.99);
//        captcha.reduceBlackNoise(10);
        ArrayList<Letter> ret = new ArrayList<Letter>();
        for (int i = 0; i < 5; i++) {
            int averageWidth = Math.min(captcha.getWidth(), (int) (captcha.getWidth() / (5 - i) +2));
            Letter first = new Letter(averageWidth, captcha.getHeight());
            first.setOwner(captcha.owner);
            for (int x = 0; x < averageWidth; x++) {
                for (int y = 0; y < captcha.getHeight(); y++) {
                    try {
                        first.grid[x][y] = captcha.grid[x][y];
                    } catch (Exception e) {

                    }
                }
            }

            LetterComperator r = captcha.owner.getLetter(first);

            // BasicWindow.showImage(r.getB().getImage(3));
            ret.add(first);
            if (i < 4) {
                captcha.crop(r.getIntersection().getWidth(), 0, 0, 0);

            }
        }
        if (ret.size() < 5) return null;
        return ret.toArray(new Letter[] {});

    }
    public static Letter[] letterFilter(Letter[] org, JAntiCaptcha jac) {
        return org;
    }
}