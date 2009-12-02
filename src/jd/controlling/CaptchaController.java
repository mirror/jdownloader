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

package jd.controlling;

import java.awt.Image;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import jd.captcha.JACMethod;
import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Captcha;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.UserIO;
import jd.utils.JDUtilities;

public class CaptchaController {

    private String methodname;
    private File captchafile;
    private String explain;
    private String suggest;
    private String host;
    private ImageIcon icon;

    public CaptchaController(String host, ImageIcon icon, String method, File file, String suggest, String explain) {
        this.host = host;
        this.icon = icon;
        this.methodname = method;
        this.captchafile = file;
        this.explain = explain;
        this.suggest = suggest;
    }

    /**
     * Returns if the method is enabled.
     * 
     * @return
     */
    private boolean hasMethod() {
        return (JDUtilities.getConfiguration() != null && !SubConfiguration.getConfig("JAC").getBooleanProperty(Configuration.PARAM_CAPTCHA_JAC_DISABLE, false)) && JACMethod.hasMethod(methodname);
    }

    public String getCode(int flag) {
        if (!hasMethod()) {
            if ((flag & UserIO.NO_USER_INTERACTION) > 0) return null;
            return UserIO.getInstance().requestCaptchaDialog(flag, host, icon, captchafile, suggest, explain);
        }
        JAntiCaptcha jac = new JAntiCaptcha(methodname);
        try {
            Image captchaImage = ImageIO.read(captchafile);

            Captcha captcha = jac.createCaptcha(captchaImage);
            String captchaCode = jac.checkCaptcha(captchafile, captcha);
            if (jac.isExtern()) {
                if ((flag & UserIO.NO_USER_INTERACTION) == 0 && captchaCode == null || captchaCode.trim().length() == 0) {
                    captchaCode = UserIO.getInstance().requestCaptchaDialog(flag, host, icon, captchafile, suggest, explain);
                }
                return captchaCode;
            }

            LetterComperator[] lcs = captcha.getLetterComperators();

            double vp = 0.0;
            if (lcs == null) {
                vp = 100.0;
            } else {
                for (LetterComperator element : lcs) {
                    if (element == null) {
                        vp = 100.0;
                        break;
                    }
                    vp = Math.max(vp, element.getValityPercent());
                }
            }

            if (vp > SubConfiguration.getConfig("JAC").getIntegerProperty(Configuration.AUTOTRAIN_ERROR_LEVEL, 95)) {
                if ((flag & UserIO.NO_USER_INTERACTION) > 0) return captchaCode;
                return UserIO.getInstance().requestCaptchaDialog(flag, host, icon, captchafile, captchaCode, explain);
            } else {
                return captchaCode;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
