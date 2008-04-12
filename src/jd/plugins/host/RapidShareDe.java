//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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


package jd.plugins.host;

import java.io.File;
import java.net.URL;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.DownloadLink;
import jd.plugins.Form;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class RapidShareDe extends PluginForHost {
    private static final String  HOST             = "rapidshare.de";

    private static final String  VERSION          = "1.0.0.0";

    static private final Pattern patternSupported = Pattern.compile("http://.*?rapidshare\\.de/files/[\\d]{3,9}/.*", Pattern.CASE_INSENSITIVE);

    private Form                 form;

    private File                 captchaFile;

    private long                 waittime         = 0;

    private String               code             = null;

    //
    @Override
    public boolean doBotCheck(File file) {
        return false;
    } // kein BotCheck

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getPluginID() {
        return HOST + "-" + VERSION;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    public RapidShareDe() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));

        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));

        // setConfigElements();
    }

    @SuppressWarnings("unused")
    private void setConfigElements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), "PROPERTY_RSDE_PREMIUM_USER", JDLocale.L("plugins.hoster.rapidshare.de.premiumUser", "Premium User")));
        cfg.setDefaultValue("Kundennummer");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getProperties(), "PROPERTY_RSDE_PREMIUM_PASS", JDLocale.L("plugins.hoster.rapidshare.de.premiumPass", "Premium Pass")));
        cfg.setDefaultValue("Passwort");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "PROPERTY_RSDE_USE_PREMIUM", JDLocale.L("plugins.hoster.rapidshare.de.usePremium", "Premium Account verwenden")));
        cfg.setDefaultValue(false);

    }

    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {

        if (step == null) {
            logger.info("Plugin Ende erreicht.");
            return null;
        }

        logger.info("get Next Step " + step);

        if (this.getProperties().getBooleanProperty("PROPERTY_RSDE_USE_PREMIUM", false)) {
            try {
                return this.doPremiumStep(step, downloadLink);
            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else {
            try {
                return this.doFreeStep(step, downloadLink);
            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }

    public PluginStep doPremiumStep(PluginStep step, DownloadLink downloadLink) throws Exception {
        return null;

    }

    public PluginStep doFreeStep(PluginStep step, DownloadLink downloadLink) throws Exception {
        switch (step.getStep()) {
            case PluginStep.STEP_WAIT_TIME:
                Form[] forms = Form.getForms(downloadLink.getDownloadURL());
                if (forms.length < 2) {
                    step.setStatus(PluginStep.STATUS_ERROR);
                    logger.severe("konnte den Download nicht finden");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                    return null;
                }
                form = forms[1];
                form.remove("dl.start");
                form.put("dl.start", "Free");
                requestInfo = form.getRequestInfo();
                return step;
            case PluginStep.STEP_PENDING:
                if (aborted) {
                    logger.warning("Plugin abgebrochen");
                    downloadLink.setStatus(DownloadLink.STATUS_TODO);
                    step.setStatus(PluginStep.STATUS_TODO);
                    return step;
                }
                try {
                    waittime = Long.parseLong(new Regexp(requestInfo.getHtmlCode(), "<script>var.*?\\= ([\\d]+)").getFirstMatch()) * 1000;
                }
                catch (Exception e) {
                    try {
                        waittime = Long.parseLong(new Regexp(requestInfo.getHtmlCode(), "\\(Oder warte ([\\d]+) Minuten\\)").getFirstMatch()) * 60000;
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
                        step.setStatus(PluginStep.STATUS_ERROR);
                    }
                    catch (Exception es) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        logger.severe("kann wartezeit nicht setzen");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        return null;
                    }
                }
                step.setParameter((long) waittime);
                return step;
            case PluginStep.STEP_GET_CAPTCHA_FILE:
                String ticketCode = JDUtilities.htmlDecode(new Regexp(requestInfo.getHtmlCode(), "unescape\\(\\'(.*?)\\'\\)").getFirstMatch());
                RequestInfo req = new RequestInfo(ticketCode, null, requestInfo.getCookie(), requestInfo.getHeaders(), requestInfo.getResponseCode());
                req.setConnection(requestInfo.getConnection());
                form = Form.getForms(req)[0];
                captchaFile = getLocalCaptchaFile(this, ".png");
                String captchaAdress = new Regexp(ticketCode, "<img src=\"(.*?)\">").getFirstMatch();
                logger.info("CaptchaAdress:" + captchaAdress);
                boolean fileDownloaded = JDUtilities.download(captchaFile, getRequestWithoutHtmlCode(new URL(captchaAdress), requestInfo.getCookie(), null, true).getConnection());
                if (!fileDownloaded || !captchaFile.exists() || captchaFile.length() == 0) {
                    logger.severe("Captcha not found");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
                try {
                    code = Plugin.getCaptchaCode(captchaFile, this);
                }
                catch (Exception e) {

                }
                if (code == null || code == "") {
                    logger.severe("Bot erkannt");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_BOT_DETECTED);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    JDUtilities.appendInfoToFilename(this, captchaFile, "_NULL", false);
                    return step;
                }
                form.put("captcha", code);
                step.setStatus(PluginStep.STATUS_SKIP);
                return step;
            case PluginStep.STEP_DOWNLOAD:
                if (aborted) {
                    logger.warning("Plugin abgebrochen");
                    downloadLink.setStatus(DownloadLink.STATUS_TODO);
                    step.setStatus(PluginStep.STATUS_TODO);
                    return step;
                }
                HTTPConnection urlConnection = form.getConnection();
                downloadLink.setName(getFileNameFormHeader(urlConnection));
                downloadLink.setDownloadMax(urlConnection.getContentLength());
                if (!hasEnoughHDSpace(downloadLink)) {
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
               dl = new RAFDownload(this, downloadLink, urlConnection);

                if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {

                    logger.severe("captcha wrong");
                    step.setStatus(PluginStep.STATUS_ERROR);
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                    JDUtilities.appendInfoToFilename(this, captchaFile, "_" + code, false);
                }
                return step;

        }
        step.setStatus(PluginStep.STATUS_ERROR);
        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
        return step;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        Form[] forms = Form.getForms(downloadLink.getDownloadURL());
        if (forms.length < 2) return false;
        requestInfo = forms[1].getRequestInfo();
        try {
            String[][] regExp = new Regexp(requestInfo.getHtmlCode(), "<p>Du hast die Datei <b>(.*?)</b> \\(([\\d]+)").getMatches();
            downloadLink.setDownloadMax(Integer.parseInt(regExp[0][1]) * 1024);
            downloadLink.setName(regExp[0][0]);
            return true;
        }
        catch (Exception e) {
            // TODO: handle exception
        }
        return false;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        if (this.getProperties().getBooleanProperty("PROPERTY_RSDE_USE_PREMIUM", false)) {
            return Integer.MAX_VALUE;
        }
        else {
            return 1;
        }
    }

    @Override
    public void reset() {
    // TODO Automatisch erstellter Methoden-Stub
    }

    @Override
    public void resetPluginGlobals() {
    // TODO Automatisch erstellter Methoden-Stub
    }

    @Override
    public String getAGBLink() {

        return "http://rapidshare.de/de/faq.html";
    }
}
