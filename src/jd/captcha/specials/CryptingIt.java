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

import java.awt.image.BufferedImage;
import java.util.Vector;

import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;
import jd.captcha.utils.GifDecoder;

public class CryptingIt {
    public static Letter[] getLetters(Captcha captcha) throws InterruptedException{
        try {
            GifDecoder d = new GifDecoder();
            d.read(captcha.getCaptchaFile().getAbsolutePath());
            int n = d.getFrameCount();
            Captcha[] frames = new Captcha[d.getFrameCount()];
            Letter[] Letters = new Letter[3];
            int c = 0;
            for (int i = 0; i < n; i++) {
                BufferedImage frame = d.getFrame(i);
                frames[i] = captcha.owner.createCaptcha(frame);
                if (i > 0 && !frames[i].getString().equals(frames[i - 1].getString())) {
                    Vector<PixelObject> os = frames[i].getObjects(0.99, 0.99);
                    Letters[c++] = os.get(0).toLetter();
                    if (c == 3) break;
                }
            }
            return Letters;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Letter[] letterFilter(Letter[] org, JAntiCaptcha jac) throws InterruptedException {
        for (int i = 0; i < org.length; i++) {
            Letter letter = org[i];
            letter.toBlackAndWhite(0.7);
            LetterComperator r = jac.getLetter(letter);
            letter.detected = r;
        }
        return org;
    }
}
