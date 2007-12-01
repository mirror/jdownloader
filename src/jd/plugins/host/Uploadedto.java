package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class Uploadedto extends PluginForHost {

    static private final Pattern    PAT_SUPPORTED                = getSupportPattern("http://[*]uploaded.to[+]");

    static private final String     HOST                         = "uploaded.to";

    static private final String     PLUGIN_NAME                  = HOST;

    static private final String     PLUGIN_VERSION               = "0.1";

    static private final String     PLUGIN_ID                    = PLUGIN_NAME + "-" + PLUGIN_VERSION;

    static private final String     CODER                        = "coalado/DwD CAPTCHA fix";

    // /Simplepattern

    static private final String     DOWNLOAD_URL                 = "<form name=\"download_form\" onsubmit=\"startDownload();\" method=\"post\" action=\"°\">";

    static private final String     DOWNLOAD_URL_WITHOUT_CAPTCHA = "<form name=\"download_form\" method=\"post\" action=\"°\">";

    static private final String     DOWNLOAD_URL_PREMIUM         = "<form name=\"download_form\" method=\"post\" action=\"°\">";

    private static final String     FILE_INFO                    = "Dateiname:°</td><td><b>°</b></td></tr>°<tr><td style=\"padding-left:4px;\">Dateityp:°</td><td>°</td></tr>°<tr><td style=\"padding-left:4px;\">Dateig°</td><td>°</td>";

    static private final String     FILE_NOT_FOUND               = "Datei existiert nicht";

    private static final Pattern    CAPTCHA_FLE                  = Pattern.compile("<img name=\"img_captcha\" src=\"(.*?)\" border=0 />");

    private static final Pattern    CAPTCHA_TEXTFLD              = Pattern.compile("<input type=\"text\" id=\".*?\" name=\"(.*?)\" onkeyup=\"cap\\(\\)\\;\" size=3 />");

    private HashMap<String, String> postParameter                = new HashMap<String, String>();

    private static String           lastPassword                 = null;

    private String                  captchaAddress;

    private String                  postTarget;

    private String                  finalURL;

    private boolean                 useCaptchaVersion;

    private String                  cookie;

    public Uploadedto() {
        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
        setConfigElements();
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }

    // @Override
    // public URLConnection getURLConnection() {
    // // XXX: ???
    // return null;
    // }
    public PluginStep doStep(PluginStep step, DownloadLink parameter) {
        if (getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false)) {

            return doPremiumStep(step, parameter);
        }
        // http://uploaded.to/file/6t2rrq
        // http://uploaded.to/?id=6t2rrq
        // http://uploaded.to/file/6t2rrq/blabla.rar
        // Url correction
        correctURL(parameter);
        RequestInfo requestInfo;
        try {
            DownloadLink downloadLink = (DownloadLink) parameter;
            switch (step.getStep()) {
                case PluginStep.STEP_WAIT_TIME:

                    requestInfo = getRequest(new URL(downloadLink.getUrlDownloadDecrypted()), "lang=de", null, true);

                    // Datei geloescht?
                    if (requestInfo.getHtmlCode().contains(FILE_NOT_FOUND)) {
                        logger.severe("download not found");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }

                    // 3 Versuche
                    String pass = null;
                    if (requestInfo.containsHTML("file_key")) {
                        logger.info("File is Password protected1");
                        if (lastPassword != null) {
                            logger.info("Try last pw: " + lastPassword);
                            pass = lastPassword;
                            requestInfo = postRequest(new URL(downloadLink.getUrlDownloadDecrypted()), "lang=de", null, null, "lang=de&file_key=" + pass, false);

                        }
                        else {
                            pass = JDUtilities.getController().getUiInterface().showUserInputDialog("Password?");
                            logger.info("Password: " + pass);
                            requestInfo = postRequest(new URL(downloadLink.getUrlDownloadDecrypted()), "lang=de", null, null, "lang=de&file_key=" + pass, false);
                        }

                    }
                    if (requestInfo.containsHTML("file_key")) {
                        logger.info("File is Password protected2");
                        pass = JDUtilities.getController().getUiInterface().showUserInputDialog("Password?");
                        logger.info("Password: " + pass);
                        requestInfo = postRequest(new URL(downloadLink.getUrlDownloadDecrypted()), "lang=de", null, null, "lang=de&file_key=" + pass, false);

                    }
                    if (requestInfo.containsHTML("file_key")) {
                        logger.info("File is Password protected3");
                        pass = JDUtilities.getController().getUiInterface().showUserInputDialog("Password?");
                        logger.info("Password: " + pass);
                        requestInfo = postRequest(new URL(downloadLink.getUrlDownloadDecrypted()), "lang=de", null, null, "lang=de&file_key=" + pass, false);

                    }
                    if (requestInfo.containsHTML("file_key")) {
                        logger.severe("Wrong password entered");

                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                        step.setParameter("Wrong Password");
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;

                    }
                    if (pass != null) {
                        lastPassword = pass;
                    }

                    // logger.info(requestInfo.getHtmlCode());
                    this.captchaAddress = "http://" + requestInfo.getConnection().getRequestProperty("host") + "/" + getFirstMatch(requestInfo.getHtmlCode(), CAPTCHA_FLE, 1);

                    this.postTarget = getFirstMatch(requestInfo.getHtmlCode(), CAPTCHA_TEXTFLD, 1);
                    String url = getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_URL, 0);
                    if (url == null) {
                        this.useCaptchaVersion = false;
                        // Captcha deaktiviert
                        // 

                        url = getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_URL_WITHOUT_CAPTCHA, 0);
                        logger.finer("Use Captcha free Plugin: " + url);
                        requestInfo = postRequest(new URL(url), "lang=de", null, null, null, false);
                        if (requestInfo.getConnection().getHeaderField("Location") != null) {
                            this.finalURL = "http://" + requestInfo.getConnection().getRequestProperty("host") + requestInfo.getConnection().getHeaderField("Location");
                            return step;
                        }
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    }
                    else {
                        useCaptchaVersion = true;
                        logger.finer("Use Captcha Plugin");
                        requestInfo = postRequest(new URL(url), "lang=de", null, null, null, false);
                        if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error") > 0) {
                            logger.severe("Unbekannter fehler.. retry in 20 sekunden");
                            step.setStatus(PluginStep.STATUS_ERROR);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                            step.setParameter(20000l);
                            return step;
                        }
                        if (requestInfo.getConnection().getHeaderField("Location") != null) {
                            this.finalURL = "http://" + requestInfo.getConnection().getRequestProperty("host") + requestInfo.getConnection().getHeaderField("Location");
                            return step;
                        }
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    }
                    return step;
                case PluginStep.STEP_GET_CAPTCHA_FILE:
                    if (useCaptchaVersion) {
                        File file = this.getLocalCaptchaFile(this);
                        if (!JDUtilities.download(file, captchaAddress) || !file.exists()) {
                            logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
                            step.setParameter(null);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                            return step;
                        }
                        else {
                            step.setParameter(file);
                            step.setStatus(PluginStep.STATUS_USER_INPUT);
                        }
                        break;
                    }
                    else {
                        step.setStatus(PluginStep.STATUS_SKIP);
                        downloadLink.setStatusText("No Captcha");
                        return step;

                    }
                case PluginStep.STEP_DOWNLOAD:
                    if (useCaptchaVersion) {
                        this.finalURL = finalURL + (String) steps.get(1).getParameter();
                        logger.info("dl " + finalURL);
                        postParameter.put(postTarget, (String) steps.get(1).getParameter());
                        requestInfo = getRequestWithoutHtmlCode(new URL(finalURL), "lang=de", null, false);

                        if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error-captcha") > 0) {
                            step.setStatus(PluginStep.STATUS_ERROR);
                            logger.severe("captcha Falsch");
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);

                            return step;
                        }

                        if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error") > 0) {
                            step.setStatus(PluginStep.STATUS_ERROR);
                            logger.severe("Fehler 1 Errorpage wird angezeigt " + requestInfo.getConnection().getHeaderField("Location"));

                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                            step.setParameter(20000l);
                            return step;
                        }
                        int length = requestInfo.getConnection().getContentLength();
                        downloadLink.setDownloadMax(length);
                        logger.info("Filenam1e: " + getFileNameFormHeader(requestInfo.getConnection()));
                      
                        if (getFileNameFormHeader(requestInfo.getConnection()) == null || getFileNameFormHeader(requestInfo.getConnection()).indexOf("?") >= 0) {
                            step.setStatus(PluginStep.STATUS_ERROR);
                            logger.severe("Fehler 2 Dateiname kann nicht ermittelt werden");
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                            step.setParameter(20000l);
                            return step;
                        }
                        downloadLink.setName(getFileNameFormHeader(requestInfo.getConnection()));
                        if (!hasEnoughHDSpace(downloadLink)) {
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }
                        download(downloadLink, (URLConnection) requestInfo.getConnection());
                        step.setStatus(PluginStep.STATUS_DONE);
                        downloadLink.setStatus(DownloadLink.STATUS_DONE);
                        return step;
                    }
                    else {
                        this.finalURL = finalURL + "";
                        logger.info("dl " + finalURL);

                        requestInfo = getRequestWithoutHtmlCode(new URL(finalURL), "lang=de", null, false);

                        if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error") > 0) {
                            step.setStatus(PluginStep.STATUS_ERROR);
                            logger.severe("Fehler 1 Errorpage wird angezeigt " + requestInfo.getConnection().getHeaderField("Location"));

                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                            step.setParameter(10000l);
                            return step;
                        }
                        int length = requestInfo.getConnection().getContentLength();
                        downloadLink.setDownloadMax(length);
                        logger.info("Filename: " + getFileNameFormHeader(requestInfo.getConnection()));
                        logger.info("Filenam2e: " + getFileNameFormHeader(requestInfo.getConnection()));
                        logger.info("Headers: "+requestInfo.getHeaders());
                        logger.info("Connection: "+requestInfo.getConnection());
                        logger.info("Code: \r\n"+requestInfo.getHtmlCode());
                        
                        if (getFileNameFormHeader(requestInfo.getConnection()) == null || getFileNameFormHeader(requestInfo.getConnection()).indexOf("?") >= 0) {
                            step.setStatus(PluginStep.STATUS_ERROR);
                            logger.severe("Fehler 2 Dateiname kann nicht ermittelt werden");
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                            step.setParameter(20000l);
                            return step;
                        }
                        downloadLink.setName(getFileNameFormHeader(requestInfo.getConnection()));
                        if (!hasEnoughHDSpace(downloadLink)) {
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }
                        download(downloadLink, (URLConnection) requestInfo.getConnection());
                        step.setStatus(PluginStep.STATUS_DONE);
                        downloadLink.setStatus(DownloadLink.STATUS_DONE);
                        return step;
                    }
            }
            return step;
        }
        catch (IOException e) {
            e.printStackTrace();
            step.setStatus(PluginStep.STATUS_ERROR);
            logger.severe("Unbekannter Fehler. siehe Exception");
            parameter.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            step.setParameter(20000l);
            return step;
        }
    }

    private PluginStep doPremiumStep(PluginStep step, DownloadLink parameter) {
        correctURL(parameter);

        RequestInfo requestInfo;
        String user = getProperties().getStringProperty(PROPERTY_PREMIUM_USER);
        String pass = getProperties().getStringProperty(PROPERTY_PREMIUM_PASS);

        if (user == null || pass == null) {

            step.setStatus(PluginStep.STATUS_ERROR);
            logger.severe("Premiumfehler Logins are incorrect");
            parameter.setStatus(DownloadLink.STATUS_ERROR_PREMIUM_LOGIN);
            return step;

        }
        try {
            DownloadLink downloadLink = (DownloadLink) parameter;
            switch (step.getStep()) {
                // Wird als login verwendet
                case PluginStep.STEP_WAIT_TIME:
                    logger.info("login");
                    requestInfo = Plugin.postRequest(new URL("http://uploaded.to/login"), null, null, null, "email=Honk&password=fxnsvzh", false);
                    if (requestInfo.getCookie().indexOf("auth") < 0) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        logger.severe("Premiumfehler Login");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_PREMIUM_LOGIN);
                        return step;
                    }
                    cookie = requestInfo.getCookie();

                    return step;
                case PluginStep.STEP_GET_CAPTCHA_FILE:
                    step.setStatus(PluginStep.STATUS_SKIP);
                    downloadLink.setStatusText("Premiumdownload");
                    step = nextStep(step);
                    return step;
                case PluginStep.STEP_DOWNLOAD:

                    requestInfo = getRequest(new URL(downloadLink.getUrlDownloadDecrypted()), cookie, null, false);
                    // Datei geloescht?
                    if (requestInfo.getHtmlCode().contains(FILE_NOT_FOUND)) {
                        logger.severe("download not found");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }

                    // 3 Versuche
                    String filepass = null;
                    if (requestInfo.containsHTML("file_key")) {
                        logger.info("File is Password protected1");
                        if (lastPassword != null) {
                            logger.info("Try last pw: " + lastPassword);
                            filepass = lastPassword;
                            requestInfo = postRequest(new URL(downloadLink.getUrlDownloadDecrypted()), cookie, null, null, "lang=de&file_key=" + filepass, false);

                        }
                        else {
                            filepass = JDUtilities.getController().getUiInterface().showUserInputDialog("Password?");
                            logger.info("Password: " + pass);
                            requestInfo = postRequest(new URL(downloadLink.getUrlDownloadDecrypted()), cookie, null, null, "lang=de&file_key=" + filepass, false);
                        }

                    }
                    if (requestInfo.containsHTML("file_key")) {
                        logger.info("File is Password protected2");
                        filepass = JDUtilities.getController().getUiInterface().showUserInputDialog("Password?");
                        logger.info("Password: " + pass);
                        requestInfo = postRequest(new URL(downloadLink.getUrlDownloadDecrypted()), cookie, null, null, "lang=de&file_key=" + filepass, false);

                    }
                    if (requestInfo.containsHTML("file_key")) {
                        logger.info("File is Password protected3");
                        filepass = JDUtilities.getController().getUiInterface().showUserInputDialog("Password?");
                        logger.info("Password: " + pass);
                        requestInfo = postRequest(new URL(downloadLink.getUrlDownloadDecrypted()), cookie, null, null, "lang=de&file_key=" + filepass, false);

                    }
                    if (requestInfo.containsHTML("file_key")) {
                        logger.severe("Wrong password entered");

                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                        step.setParameter("Wrong Password");
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;

                    }
                    if (filepass != null) {
                        lastPassword = filepass;
                    }
                    String newURL = null;
                    if (requestInfo.getConnection().getHeaderField("Location") == null || requestInfo.getConnection().getHeaderField("Location").length() < 10) {
                        newURL = getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_URL_PREMIUM, 0);
                        logger.info(requestInfo.getHtmlCode());
                        if (newURL == null) {
                            logger.severe("Indirekter Link konnte nicht gefunden werden");

                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                            step.setParameter("Indirect Link Error");
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }

                        requestInfo = postRequest(new URL(newURL), cookie, null, null, null, false);

                        if (requestInfo.getConnection().getHeaderField("Location") == null || requestInfo.getConnection().getHeaderField("Location").length() < 10) {
                            if (getFileNameFormHeader(requestInfo.getConnection()) == null || getFileNameFormHeader(requestInfo.getConnection()).indexOf("?") >= 0) {
                                step.setStatus(PluginStep.STATUS_ERROR);
                                logger.severe("Endlink not found");
                                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);

                                return step;
                            }

                        }
                    }
                    else {
                        logger.info("Direct Downloads active");

                    }
                    String redirect = requestInfo.getConnection().getHeaderField("Location");
                    if (!redirect.startsWith("http://") && newURL != null) {

                        redirect = "http://" + new URL(newURL).getHost() + redirect;

                    }

                    requestInfo = getRequestWithoutHtmlCode(new URL(redirect), cookie, null, false);
                    int length = requestInfo.getConnection().getContentLength();
                    downloadLink.setDownloadMax(length);
                    logger.info("Filename: " + getFileNameFormHeader(requestInfo.getConnection()));
                    if (getFileNameFormHeader(requestInfo.getConnection()) == null || getFileNameFormHeader(requestInfo.getConnection()).indexOf("?") >= 0) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        logger.severe("Fehler 2 Dateiname kann nicht ermittelt werden");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);

                        return step;
                    }
                    downloadLink.setName(getFileNameFormHeader(requestInfo.getConnection()));
                    if (!hasEnoughHDSpace(downloadLink)) {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    download(downloadLink, (URLConnection) requestInfo.getConnection());
                    step.setStatus(PluginStep.STATUS_DONE);
                    downloadLink.setStatus(DownloadLink.STATUS_DONE);
                    return step;
            }
            return step;
        }
        catch (Exception e) {
            e.printStackTrace();
            step.setStatus(PluginStep.STATUS_ERROR);
            logger.severe("Unbekannter Fehler. siehe Exception");
            parameter.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);

            return step;
        }
    }

    /**
     * Korrigiert den Downloadlink in ein einheitliches Format
     * 
     * @param parameter
     */
    private void correctURL(DownloadLink parameter) {
        String link = parameter.getUrlDownloadDecrypted();
        link = link.replace("/?id=", "/file/");
        link = link.replace("?id=", "file/");
        String[] parts = link.split("\\/");
        String newLink = "";
        for (int t = 0; t < Math.min(parts.length, 5); t++)
            newLink += parts[t] + "/";

        parameter.setUrlDownload(newLink);

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void reset() {
        this.finalURL = null;
        cookie = null;
    }

    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()) + ")";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        RequestInfo requestInfo;
        correctURL(downloadLink);
        try {
            requestInfo = getRequestWithoutHtmlCode(new URL(downloadLink.getUrlDownloadDecrypted()), null, null, false);
            if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error") > 0) {
                this.setStatusText("Error");
                return false;
            }
            else {
                if (requestInfo.getConnection().getHeaderField("Location") != null) {
                    requestInfo = getRequest(new URL("http://" + HOST + requestInfo.getConnection().getHeaderField("Location")), null, null, true);
                }
                else {
                    requestInfo = readFromURL(requestInfo.getConnection());
                }

                // Datei geloescht?
                if (requestInfo.getHtmlCode().contains(FILE_NOT_FOUND)) {
                    this.setStatusText("File Not Found");
                    return false;
                }

                String fileName = JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), FILE_INFO, 1));
                String ext = getSimpleMatch(requestInfo.getHtmlCode(), FILE_INFO, 4);
                String fileSize = getSimpleMatch(requestInfo.getHtmlCode(), FILE_INFO, 7);
                downloadLink.setName(fileName.trim() + "" + ext.trim());
                if (fileSize != null) {
                    try {
                        int length = (int) (Double.parseDouble(fileSize.trim().split(" ")[0]));
                        if (fileSize.toLowerCase().indexOf("mb") > 0) {
                            length *= 1024 * 1024;
                        }
                        else if (fileSize.toLowerCase().indexOf("kb") > 0) {
                            length *= 1024;
                        }

                        downloadLink.setDownloadMax(length);
                    }
                    catch (Exception e) {
                    }
                }
            }
            return true;
        }
        catch (MalformedURLException e) {
        }
        catch (IOException e) {
        }
        return false;
    }

    private void setConfigElements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Premium Account"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PROPERTY_PREMIUM_USER, "Premium User"));
        cfg.setDefaultValue("Kundennummer");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PROPERTY_PREMIUM_PASS, "Premium Pass"));
        cfg.setDefaultValue("Passwort");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_USE_PREMIUM, "Premium Account verwenden"));
        cfg.setDefaultValue(false);

    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return 1;
    }
}
