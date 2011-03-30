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

package jd.controlling.captcha;

import java.awt.Image;
import java.io.File;

import javax.imageio.ImageIO;

import jd.captcha.JACMethod;
import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Captcha;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.UserIO;

public class CaptchaController {

    private static CaptchaSolver captchaSolver = null;

    public static void setCaptchaSolver(CaptchaSolver captchaSolver) {
        CaptchaController.captchaSolver = captchaSolver;
    }

    public static CaptchaSolver getCaptchaSolver() {
        return captchaSolver;
    }

    private final String methodname;
    private final File captchafile;
    private final String explain;
    private final String suggest;
    private final String host;

    private final long initTime;
    private CaptchaDialogQueueEntry dialog = null;
    private String response = null;

    public CaptchaController(long initTime, final String host, final String method, final File file, final String suggest, final String explain) {
        this.host = host;
        this.methodname = method;
        this.captchafile = file;
        this.explain = explain;
        this.suggest = suggest;
        this.initTime = initTime;
    }

    public String getMethodname() {
        return methodname;
    }

    public File getCaptchafile() {
        return captchafile;
    }

    public String getExplain() {
        return explain;
    }

    public String getSuggest() {
        return suggest;
    }

    public String getHost() {
        return host;
    }

    public long getInitTime() {
        return initTime;
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
        if (!hasMethod()) { return ((flag & UserIO.NO_USER_INTERACTION) > 0) ? null : addCaptchaToQueue(flag, suggest); }
        final JAntiCaptcha jac = new JAntiCaptcha(methodname);
        try {
            final Image captchaImage = ImageIO.read(captchafile);

            final Captcha captcha = jac.createCaptcha(captchaImage);
            String captchaCode = jac.checkCaptcha(captchafile, captcha);
            if (jac.isExtern()) {
                if ((flag & UserIO.NO_USER_INTERACTION) == 0 && captchaCode == null || captchaCode.trim().length() == 0) {
                    captchaCode = addCaptchaToQueue(flag, suggest);
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
                return ((flag & UserIO.NO_USER_INTERACTION) > 0) ? captchaCode : addCaptchaToQueue(flag, captchaCode);
            } else {
                return captchaCode;
            }
        } catch (final Exception e) {
            return null;
        }
    }

    public void setResponse(String code) {
        this.response = code;
        if (dialog != null) dialog.setResponse(code);
    }

    private String addCaptchaToQueue(final int flag, final String def) {
        CaptchaEventSender.getInstance().fireEvent(new CaptchaTodoEvent(this));
        try {
            response = CaptchaDialogQueue.getInstance().addWait(dialog = new CaptchaDialogQueueEntry(this, flag, def));
        } finally {
            CaptchaEventSender.getInstance().fireEvent(new CaptchaFinishEvent(this));
        }
        return response;
    }

}
