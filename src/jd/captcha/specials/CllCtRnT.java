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
        ArrayList<PixelObject> os = (ArrayList<PixelObject>) cl[3];
        Collections.sort(os);
        int gab = pixels[2] / (captcha.owner.getLetterNum());
        int[] mergeInfos = EasyCaptcha.mergeObjectsBasic(os, captcha, gab);
        EasyCaptcha.getRightletters(os, captcha, pixels, mergeInfos, 0);
        Collections.sort(os);
        ArrayList<Letter> ret = new ArrayList<Letter>();
        for (PixelObject pixelObject : os) {
            Letter let = pixelObject.toLetter();
            let.toBlackAndWhite();
            ret.add(let);
//            BasicWindow.showImage(let.getImage());
        }
        // toBlack(captcha);
        // BasicWindow.showImage(captcha.getImage());
        return ret.toArray(new Letter[] {});

    }
}
