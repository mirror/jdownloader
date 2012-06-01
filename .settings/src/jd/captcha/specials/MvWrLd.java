package jd.captcha.specials;

import java.util.Collections;
import java.util.List;

import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;

public class MvWrLd {

    public static Letter[] getLetters(final Captcha captcha) {
        captcha.cleanBySaturation(0, 19);
        final List<PixelObject> ob = ColoredObject.getObjects(captcha, 4, 25, 3);
        Collections.sort(ob);
        final Letter[] let = new Letter[ob.size()];
        for (int i = 0; i < let.length; i++) {
            let[i] = ob.get(i).toLetter();
            toBlack(let[i]);
            let[i].reduceBlackNoise(4);
            let[i].normalize(4);
        }
        return let;
    }

    private static void toBlack(final PixelGrid captcha) {
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    captcha.grid[x][y] = 0x000000;
                }

            }
        }
    }
}
