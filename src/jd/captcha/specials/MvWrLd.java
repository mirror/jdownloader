package jd.captcha.specials;

import java.util.Collections;
import java.util.List;

import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;

public class MvWrLd {

    private static void toBlack(PixelGrid captcha) {
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    captcha.grid[x][y] = 0x000000;
                }
            }
        }
    }

    public static Letter[] getLetters(Captcha captcha) {
        captcha.cleanBySaturation(0, 19);
        List<PixelObject> ob = ColoredObject.getObjects(captcha, 4, 25, 3);
        Collections.sort(ob);
        Letter[] let = new Letter[ob.size()];
        for (int i = 0; i < let.length; i++) {
            let[i] = ob.get(i).toLetter();
            toBlack(let[i]);
            let[i].reduceBlackNoise(4);
            let[i].normalize(4);
        }
        return let;
    }

}