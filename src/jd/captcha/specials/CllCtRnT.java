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

import java.util.ArrayList;
import java.util.Collections;

import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;

public class CllCtRnT {
    public static Letter[] getLetters(Captcha captcha) {

        // BasicWindow.showImage(captcha.getImage());

        Object[] cl = EasyCaptcha.clean(captcha);
        int[] pixels = new int[] { (Integer) cl[0], (Integer) cl[1], (Integer) cl[2] };
        @SuppressWarnings("unchecked")
        java.util.List<PixelObject> os = (java.util.List<PixelObject>) cl[3];
        Collections.sort(os);
        int gab = pixels[2] / (captcha.owner.getLetterNum());
        int[] mergeInfos = EasyCaptcha.mergeObjectsBasic(os, captcha, gab);
        EasyCaptcha.getRightletters(os, captcha, pixels, mergeInfos, 0);
        Collections.sort(os);
        java.util.List<Letter> ret = new ArrayList<Letter>();
        for (PixelObject pixelObject : os) {
            Letter let = pixelObject.toLetter();
            let.toBlackAndWhite();
            ret.add(let);
            // BasicWindow.showImage(let.getImage());
        }
        // toBlack(captcha);
        // BasicWindow.showImage(captcha.getImage());
        return ret.toArray(new Letter[] {});

    }
}
