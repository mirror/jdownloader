package jd.captcha.specials;
import java.awt.image.BufferedImage;

import com.jhlabs.image.BoxBlurFilter;
import com.jhlabs.image.ContrastFilter;

import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;

public class EasyCaptcha_AppsceneOrg {
    public static Letter[] getLetters(Captcha captcha) {
        BufferedImage image = (BufferedImage) captcha.getImage();
        BoxBlurFilter blur = new BoxBlurFilter(1, 1, 3);
        BufferedImage dest = blur.createCompatibleDestImage(image, null);
        
        blur.filter(image, dest);
        ContrastFilter cf = new ContrastFilter();
        cf.setContrast(8);
        image=dest;
        dest=cf.createCompatibleDestImage(image, null);
        cf.filter(image, dest);
        Captcha cap2 = captcha.owner.createCaptcha(dest);
        return EasyCaptcha.getLetters(cap2);
    }
}
