package jd.captcha.specials;

import java.io.File;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.nutils.JDImage;

public class FsIuA {

    public static String getCode(int marginLeft, int width, File file)
    {
        JAntiCaptcha jac = new JAntiCaptcha("fileshare.in.ua");
        Captcha captcha = jac.createCaptcha(JDImage.getImage(file));
        captcha.crop(-marginLeft, 0, captcha.getWidth()-width+marginLeft, 0);
        captcha.setOrgGrid(captcha.grid);
        return jac.checkCaptcha(file, captcha);
    }

}
