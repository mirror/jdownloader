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

public class CaptchaController {

    private final String    methodname;
    private final File      captchafile;
    private final String    explain;
    private final String    suggest;
    private final String    host;
    private final ImageIcon icon;

    public CaptchaController(final String host, final ImageIcon icon, final String method, final File file, final String suggest, final String explain) {
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
        return (SubConfiguration.getConfig("JAC") != null && !SubConfiguration.getConfig("JAC").getBooleanProperty(Configuration.PARAM_CAPTCHA_JAC_DISABLE, false)) && JACMethod.hasMethod(methodname);
    }

    public String getCode(final int flag) {
        if (!hasMethod()) { return ((flag & UserIO.NO_USER_INTERACTION) > 0) ? null : showDialog(flag, suggest); }

        final JAntiCaptcha jac = new JAntiCaptcha(methodname);
        try {
            final Image captchaImage = ImageIO.read(captchafile);

            final Captcha captcha = jac.createCaptcha(captchaImage);
            String captchaCode = jac.checkCaptcha(captchafile, captcha);
            if (jac.isExtern()) {
                if ((flag & UserIO.NO_USER_INTERACTION) == 0 && captchaCode == null || captchaCode.trim().length() == 0) {
                    captchaCode = showDialog(flag, suggest);
                }
                return captchaCode;
            }

            final LetterComperator[] lcs = captcha.getLetterComperators();

            double vp = 0.0;
            if (lcs == null) {
                vp = 100.0;
            } else {
                for (final LetterComperator element : lcs) {
                    if (element == null) {
                        vp = 100.0;
                        break;
                    }
                    vp = Math.max(vp, element.getValityPercent());
                }
            }

            if (vp > SubConfiguration.getConfig("JAC").getIntegerProperty(Configuration.AUTOTRAIN_ERROR_LEVEL, 95)) {
                return ((flag & UserIO.NO_USER_INTERACTION) > 0) ? captchaCode : showDialog(flag, captchaCode);
            } else {
                return captchaCode;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String showDialog(int flag, String def) {
        UserIO.setCountdownTime(SubConfiguration.getConfig("JAC").getIntegerProperty(Configuration.JAC_SHOW_TIMEOUT, 20));
        String ret = UserIO.getInstance().requestCaptchaDialog(flag, host, icon, captchafile, def, explain);
        UserIO.setCountdownTime(-1);
        return ret;
    }
}
