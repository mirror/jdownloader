package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.Download;
import jd.plugins.DownloadLink;
import jd.plugins.Form;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class DatenKlo extends PluginForHost {
    private static final String  HOST          = "datenklo.net";

    private static final String  VERSION       = "1.0.0.0";

    private static final Pattern PAT_SUPPORTED = Pattern.compile("http://.*?datenklo\\.net/dl\\-[a-zA-Z0-9]{5}", Pattern.CASE_INSENSITIVE);

    private Form                 form;

    private File                 captchaFile;
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
	public boolean collectCaptchas() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean useUserinputIfCaptchaUnknown() {
		// TODO Auto-generated method stub
		return false;
	}
    @Override
    public String getPluginID() {
        return HOST + "-" + VERSION;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    public DatenKlo() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
        setConfigElements();
    }

    private void setConfigElements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), "PROPERTY_DK_PREMIUM_USER", JDLocale.L("plugins.hoster.datenklo.net.premiumUser", "Premium User")));
        cfg.setDefaultValue("Kundennummer");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getProperties(), "PROPERTY_DK_PREMIUM_PASS", JDLocale.L("plugins.hoster.datenklo.net.premiumPass", "Premium Pass")));
        cfg.setDefaultValue("Passwort");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "PROPERTY_DK_USE_PREMIUM", JDLocale.L("plugins.hoster.datenklo.net.usePremium", "Premium Account verwenden")));
        cfg.setDefaultValue(false);

    }

    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {

        if (step == null) {
            logger.info("Plugin Ende erreicht.");
            return null;
        }

        logger.info("get Next Step " + step);

        if (this.getProperties().getBooleanProperty("PROPERTY_DK_USE_PREMIUM", false)) {
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

        switch (step.getStep()) {
            case PluginStep.STEP_GET_CAPTCHA_FILE:
                String loginURL = "http://www.datenklo.net/index.php?inc=login";
                requestInfo = getRequest(new URL(loginURL));
                String cookie = requestInfo.getCookie();
                Form[] forms = Form.getForms(requestInfo);
                if (forms == null || forms.length == 0 || forms[0] == null) {
                    step.setStatus(PluginStep.STATUS_ERROR);
                    logger.severe("login fehlgeschlagen");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                    return null;
                }
                forms[0].put("login_passwort", this.getProperties().getStringProperty("PROPERTY_DK_PREMIUM_PASS"));
                forms[0].put("login_nickname", this.getProperties().getStringProperty("PROPERTY_DK_PREMIUM_USER"));
                forms[0].withHtmlCode = false;
                requestInfo = forms[0].getRequestInfo();
                cookie = cookie + ";" + requestInfo.getCookie();
                String dlurl = downloadLink.getDownloadURL();
                String password = new Regexp(dlurl, "\\&down_passwort\\=(.*)").getFirstMatch();
                if (password != null) dlurl = dlurl.replaceFirst("\\&down_passwort.*", "");
                requestInfo = getRequest(new URL(dlurl), cookie, requestInfo.getConnection().getURL().toString(), true);
                forms = Form.getForms(requestInfo);
                if (forms == null || forms.length == 0 || forms[0] == null) {
                    step.setStatus(PluginStep.STATUS_ERROR);
                    logger.severe("konnte den Download nicht finden");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                    return null;
                }
                form = forms[0];
                form.withHtmlCode = false;
                if (requestInfo.getHtmlCode().contains("type=\"text\" name=\"down_passwort\"")) {
                    if (password != null)
                        form.put("down_passwort", password);
                    else
                        form.put("down_passwort", JDUtilities.getController().getUiInterface().showUserInputDialog("Please enter the password!"));
                }
                step = nextStep(step);
            case PluginStep.STEP_DOWNLOAD:
                return doFreeStep(step, downloadLink);
        }
        return step;

    }

    public PluginStep doFreeStep(PluginStep step, DownloadLink downloadLink) throws Exception {
        switch (step.getStep()) {
            case PluginStep.STEP_GET_CAPTCHA_FILE:

                String dlurl = downloadLink.getDownloadURL();
                String password = new Regexp(dlurl, "\\&down_passwort\\=(.*)").getFirstMatch();
                if (password != null) dlurl = dlurl.replaceFirst("\\&down_passwort.*", "");
                requestInfo = getRequest(new URL(dlurl));
                if (requestInfo.getHtmlCode().contains(">Offline</span>")) {
                    logger.info("Server is offline");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return null;
                }
                Form[] forms = Form.getForms(requestInfo);
                if (forms == null || forms.length == 0 || forms[0] == null) {
                    step.setStatus(PluginStep.STATUS_ERROR);
                    logger.severe("konnte den Download nicht finden");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                    return null;
                }
                form = forms[0];
                form.withHtmlCode = false;
                if (requestInfo.getHtmlCode().contains("type=\"text\" name=\"down_passwort\"")) {
                    if (password != null)
                        form.put("down_passwort", password);
                    else
                        form.put("down_passwort", JDUtilities.getController().getUiInterface().showUserInputDialog("Please enter the password!"));
                }
                captchaFile = getLocalCaptchaFile(this, ".gif");
                String captchaAdress = "http://" + HOST + new Regexp(requestInfo.getHtmlCode(), "(/lib/captcha/CaptchaImage.php.*?)\"").getFirstMatch();
                logger.info("CaptchaAdress:" + captchaAdress);
                boolean fileDownloaded = JDUtilities.download(captchaFile, getRequestWithoutHtmlCode(new URL(captchaAdress), requestInfo.getCookie(), null, true).getConnection());
                if (!fileDownloaded || !captchaFile.exists() || captchaFile.length() == 0) {
                    logger.severe("Captcha not found");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
                form.put("down_captcha", Plugin.getCaptchaCode(captchaFile, this));
                step = nextStep(step);
            case PluginStep.STEP_DOWNLOAD:
                if (aborted) {
                    logger.warning("Plugin abgebrochen");
                    downloadLink.setStatus(DownloadLink.STATUS_TODO);
                    step.setStatus(PluginStep.STATUS_TODO);
                    return step;
                }
                HTTPConnection urlConnection = form.getConnection();
                int size = urlConnection.getContentLength();
                if (urlConnection.getContentType().matches(".*html.*")) {
                    if (size == 170) {
                        logger.warning("Password falsch");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    else if (size == 160) {
                        logger.severe("captcha wrong");
                        JDUtilities.appendInfoToFilename(this, captchaFile, "_" + form.vars.get("down_captcha"), false);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                }
                downloadLink.setName(getFileNameFormHeader(urlConnection));
                downloadLink.setDownloadMax(urlConnection.getContentLength());
                if (!hasEnoughHDSpace(downloadLink)) {
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
                Download dl = new Download(this, downloadLink, urlConnection);

                if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {

                    logger.severe("unknown error");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                    step.setStatus(PluginStep.STATUS_ERROR);
                }
                return step;
        }
        step.setStatus(PluginStep.STATUS_ERROR);
        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
        return step;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            RequestInfo requestInfo = getRequest(new URL(downloadLink.getDownloadURL()));
            String fileName = new Regexp(requestInfo.getHtmlCode(), "<td>Datei: </td>.*?<td>(.*?)</td>").getFirstMatch();
            String[][] fileSize = new Regexp(requestInfo.getHtmlCode(), "<td>Dateigr.*?: </td>.*?<td>(.*?) (.*?)</td>").getMatches();
            String password = "";
            if (requestInfo.getHtmlCode().contains("type=\"text\" name=\"down_passwort\"")) password = "&down_passwort=" + JDUtilities.getController().getUiInterface().showUserInputDialog("Please enter the password!");
            // Wurden DownloadInfos gefunden? --> Datei ist vorhanden/online
            if (fileName != null && fileSize != null) {
                downloadLink.setName(fileName);
                downloadLink.setUrlDownload(downloadLink.getDownloadURL() + password);
                try {
                    double length = Double.parseDouble(fileSize[0][0].trim());
                    int bytes;
                    String type = fileSize[0][1].toLowerCase();
                    if (type.equalsIgnoreCase("kb")) {
                        bytes = (int) (length * 1024);
                    }
                    else if (type.equalsIgnoreCase("mb")) {
                        bytes = (int) (length * 1024 * 1024);
                    }
                    else {
                        bytes = (int) length;
                    }
                    downloadLink.setDownloadMax(bytes);
                }
                catch (Exception e) {
                }
                if (requestInfo.getHtmlCode().contains(">Offline</span>")) {
                    logger.info("Server is offline");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                    return false;
                }
                // Datei ist noch verfuegbar
                return true;
            }
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        // Datei scheinbar nicht mehr verfuegbar, Fehler?
        return false;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
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

        return "http://www.datenklo.net/agb";
    }
}
