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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jd.captcha.JACMethod;
import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Captcha;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.IOPermission;
import jd.gui.UserIO;
import jd.gui.swing.dialog.CaptchaDialogInterface;
import jd.plugins.Plugin;

import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.jdownloader.DomainInfo;

public class CaptchaController {

    private static final AtomicInteger         captchaCounter = new AtomicInteger(0);

    private final int                          id;
    private final String                       methodname;
    private final List<File>                   captchafile;
    private final String                       explain;
    private final CaptchaResult                suggest;
    private final DomainInfo                   host;

    private CaptchaDialogQueueEntry            dialog         = null;
    private CaptchaResult                      response       = null;

    private IOPermission                       ioPermission   = null;

    private boolean                            responseSet    = false;

    private Plugin                             plugin;
    private CaptchaDialogInterface.CaptchaType captchaType    = CaptchaDialogInterface.CaptchaType.TEXT;

    public Plugin getPlugin() {
        return plugin;
    }

    public CaptchaController(IOPermission ioPermission, final String method, final List<File> file, final CaptchaResult suggest, final String explain, Plugin plugin) {
        this.id = captchaCounter.getAndIncrement();
        this.host = DomainInfo.getInstance(plugin.getHost());
        this.methodname = method;
        this.captchafile = file;
        this.explain = explain;
        this.suggest = suggest;
        this.ioPermission = ioPermission;
        this.plugin = plugin;
        setCaptchaType(CaptchaDialogInterface.CaptchaType.TEXT);
    }

    public int getId() {
        return id;
    }

    public String getMethodname() {
        return methodname;
    }

    public File getCaptchaFile() {
        return captchafile.get(0);
    }

    public File getPreparedCaptchaFile() {
        if (captchafile.size() > 1) { return captchafile.get(1); }
        return getCaptchaFile();
    }

    public String getExplain() {
        return explain;
    }

    public CaptchaResult getSuggest() {
        return suggest;
    }

    public DomainInfo getHost() {
        return host;
    }

    /**
     * Returns if the method is enabled.
     * 
     * @return
     */
    private boolean hasMethod() {
        if (StringUtils.isEmpty(methodname)) return false;
        return (SubConfiguration.getConfig("JAC") != null && !SubConfiguration.getConfig("JAC").getBooleanProperty(Configuration.PARAM_CAPTCHA_JAC_DISABLE, false)) && JACMethod.hasMethod(methodname);
    }

    public CaptchaResult getCode(final int flag) {
        if (!hasMethod()) { return ((flag & UserIO.NO_USER_INTERACTION) > 0) ? null : addCaptchaToQueue(flag, suggest); }
        final JAntiCaptcha jac = new JAntiCaptcha(methodname);
        CaptchaResult captchaResult = new CaptchaResult();
        try {
            final Image captchaImage = ImageProvider.read(getPreparedCaptchaFile());
            final Captcha captcha = jac.createCaptcha(captchaImage);
            String captchaCode = jac.checkCaptcha(getPreparedCaptchaFile(), captcha);
            captchaResult.setCaptchaText(captchaCode);
            if (jac.isExtern()) {
                if ((flag & UserIO.NO_USER_INTERACTION) == 0 && captchaCode == null || captchaCode.trim().length() == 0) {
                    captchaResult = addCaptchaToQueue(flag, suggest);
                } else {
                    captchaResult.setCaptchaText(captchaCode);
                }
                return captchaResult;
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
                return ((flag & UserIO.NO_USER_INTERACTION) > 0) ? captchaResult : addCaptchaToQueue(flag, suggest);
            } else {
                return captchaResult;
            }
        } catch (final Exception e) {
            return null;
        }
    }

    public void setResponse(CaptchaResult code) {
        this.response = code;
        this.responseSet = true;
        if (dialog != null) dialog.setResponse(code);
    }

    private CaptchaResult addCaptchaToQueue(final int flag, final CaptchaResult def) {
        dialog = new CaptchaDialogQueueEntry(this, flag, def);
        CaptchaResult ret = CaptchaDialogQueue.getInstance().addWait(dialog);
        if (responseSet == false) return ret;
        return response;
    }

    public CaptchaDialogQueueEntry getDialog() {
        return dialog;
    }

    public IOPermission getIOPermission() {
        return ioPermission;
    }

    /**
     * @return the captchaType
     */
    public CaptchaDialogInterface.CaptchaType getCaptchaType() {
        return captchaType;
    }

    /**
     * @param captchaType
     *            the captchaType to set
     */
    public void setCaptchaType(CaptchaDialogInterface.CaptchaType captchaType) {
        this.captchaType = captchaType;
    }

}
