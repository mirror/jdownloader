package jd.captcha.specials;

import java.io.File;
import java.io.IOException;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;

import org.appwork.utils.ImageProvider.ImageProvider;

public class FsIuA {

    public static String getCode(int marginLeft, int width, File file) throws IOException {
        JAntiCaptcha jac = new JAntiCaptcha("fileshare.in.ua");
        Captcha captcha = jac.createCaptcha(ImageProvider.read(file));
        captcha.crop(-marginLeft, 0, captcha.getWidth() - width + marginLeft, 0);
        captcha.setOrgGrid(captcha.grid);
        return jac.checkCaptcha(file, captcha);
    }

}
