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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Uploadedto extends PluginForHost {

    static private final String AGB_LINK = "http://uploaded.to/agb";

    private static final Pattern CAPTCHA_FLE = Pattern.compile("<img name=\"img_captcha\" src=\"(.*?)\" border=0 />");

    private static final Pattern CAPTCHA_TEXTFLD = Pattern.compile("<input type=\"text\" id=\".*?\" name=\"(.*?)\" onkeyup=\"cap\\(\\)\\;\" size=3 />");

    // static private final String new Regex("$Revision$","\\$Revision:
    // ([\\d]*?)\\$").getFirstMatch().*= "0.1.1";

    // static private final String PLUGIN_ID =PLUGIN_NAME + "-" + new
    // Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();

    static private final String CODER = "JD-Team";

    static private final String DOWNLOAD_LIMIT_REACHED = "Free-Traffic ist aufgebraucht";

    // Simplepattern

    static private final String DOWNLOAD_URL = "<form name=\"download_form\" onsubmit=\"startDownload();\" method=\"post\" action=\"°\">";

    static private final String DOWNLOAD_URL_PREMIUM = "<form name=\"download_form\" method=\"post\" action=\"°\">";

    static private final String DOWNLOAD_URL_WITHOUT_CAPTCHA = "<form name=\"download_form\" method=\"post\" action=\"°\">";

    private static final String FILE_INFO = "Dateiname:°</td><td><b>°</b></td></tr>°<tr><td style=\"padding-left:4px;\">Dateityp:°</td><td>°</td></tr>°<tr><td style=\"padding-left:4px;\">Dateig°</td><td>°</td>";

    static private final String FILE_NOT_FOUND = "Datei existiert nicht";

    static private final String HOST = "uploaded.to";

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?uploaded\\.to/(file/|\\?id\\=)[a-zA-Z0-9]{6}", Pattern.CASE_INSENSITIVE);
    static private final String TRAFFIC_EXCEEDED = "Ihr Premium-Traffic ist aufgebraucht";

    static private final String TRAFFIC_EXCEEDED_FREE = "Ihr Free-Traffic ist aufgebraucht";

    private String captchaAddress;

    private String cookie;

    private String finalURL;

    private HashMap<String, String> postParameter = new HashMap<String, String>();

    private String postTarget;

    private boolean useCaptchaVersion;

    public Uploadedto() {
        // steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        // steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
        setConfigElements();
        this.enablePremium();
    }

    /**
     * Korrigiert den Downloadlink in ein einheitliches Format
     * 
     * @param parameter
     */
    private void correctURL(DownloadLink parameter) {
        String link = parameter.getDownloadURL();
        link = link.replace("/?id=", "/file/");
        link = link.replace("?id=", "file/");
        String[] parts = link.split("\\/");
        String newLink = "";
        for (int t = 0; t < Math.min(parts.length, 5); t++) {
            newLink += parts[t] + "/";
        }

        parameter.setUrlDownload(newLink);

    }

    public boolean doBotCheck(File file) {
        return false;
    }

    private void doPremium(DownloadLink parameter) throws Exception {
        LinkStatus linkStatus = parameter.getLinkStatus();

        correctURL(parameter);

        RequestInfo requestInfo;
        String user = getProperties().getStringProperty(PROPERTY_PREMIUM_USER);
        String pass = getProperties().getStringProperty(PROPERTY_PREMIUM_PASS);

        if (user == null || pass == null) {

            // step.setStatus(PluginStep.STATUS_ERROR);
            logger.severe("Premiumfehler Logins are incorrect");
            linkStatus.setStatus(LinkStatus.ERROR_PREMIUM);
            linkStatus.setErrorMessage(JDLocale.L("plugins.host.premium.loginError", "Login Fehler") + ": " + user);
            getProperties().setProperty(PROPERTY_USE_PREMIUM, false);
            return;

        }

        DownloadLink downloadLink = (DownloadLink) parameter;
        // switch (step.getStep()) {
        // Wird als login verwendet
        // case PluginStep.STEP_WAIT_TIME:
        logger.info("login");
        requestInfo = HTTP.postRequest(new URL("http://uploaded.to/login"), null, null, null, "email=" + user + "&password=" + pass, false);

        if (requestInfo.getCookie().indexOf("auth") < 0) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.setStatus(LinkStatus.ERROR_PREMIUM);
            linkStatus.setErrorMessage("Login Error: " + user);
            getProperties().setProperty(PROPERTY_USE_PREMIUM, false);
            return;
        }
        cookie = requestInfo.getCookie();

        // case PluginStep.STEP_DOWNLOAD:

        requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()), cookie, null, false);
        // Datei geloescht?
        if (requestInfo.getHtmlCode().contains(FILE_NOT_FOUND)) {
            logger.severe("download not found");
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        // Traffic aufgebraucht?
        if (requestInfo.getHtmlCode().contains(TRAFFIC_EXCEEDED)) {
            logger.warning("Premium traffic exceeded (> 50 GiB in the last 10 days)");
            linkStatus.addStatus(LinkStatus.ERROR_PREMIUM);
            linkStatus.setErrorMessage("Premium overload (> 50 GiB)");
            // step.setStatus(PluginStep.STATUS_ERROR);
            getProperties().setProperty(PROPERTY_USE_PREMIUM, false);
            return;
        }

        String filepass = null;
        if (requestInfo.containsHTML("file_key")) {
            logger.info("File is Password protected");
            if (downloadLink.getStringProperty("pass", null) != null) {
                filepass = downloadLink.getStringProperty("pass", null);
            } else {
                filepass = JDUtilities.getController().getUiInterface().showUserInputDialog("Password?");
            }
            requestInfo = HTTP.postRequest(new URL(downloadLink.getDownloadURL()), cookie, null, null, "lang=de&file_key=" + filepass, false);
        }

        if (requestInfo.containsHTML("file_key")) {
            logger.severe("Wrong password entered");
            /* PassCode war falsch, also Löschen */
            downloadLink.setProperty("pass", null);
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage("Wrong Password");
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;

        }
        if (filepass != null) {
            /* PassCode war richtig, also Speichern */
            downloadLink.setProperty("pass", filepass);
        }
        String newURL = null;
        if (requestInfo.getConnection().getHeaderField("Location") == null || requestInfo.getConnection().getHeaderField("Location").length() < 10) {
            newURL = SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_URL_PREMIUM, 0);

            if (newURL == null) {
                logger.severe("Indirekter Link konnte nicht gefunden werden");

                linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
                linkStatus.setErrorMessage("Indirect Link Error");
                // step.setStatus(PluginStep.STATUS_ERROR);
                return;
            }
            if (!newURL.startsWith("http")) {
                newURL = "http://uploaded.to" + newURL;
            }
            requestInfo = HTTP.postRequest(new URL(newURL), cookie, null, null, null, false);

            if (requestInfo.getConnection().getHeaderField("Location") == null || requestInfo.getConnection().getHeaderField("Location").length() < 10) {
                if (Plugin.getFileNameFormHeader(requestInfo.getConnection()) == null || Plugin.getFileNameFormHeader(requestInfo.getConnection()).indexOf("?") >= 0) {
                    // step.setStatus(PluginStep.STATUS_ERROR);
                    logger.severe("Endlink not found");
                    linkStatus.addStatus(LinkStatus.ERROR_RETRY);

                    return;
                }

            }
        } else {
            logger.info("Direct Downloads active");

        }
        String redirect = requestInfo.getConnection().getHeaderField("Location");
        if (!redirect.startsWith("http://") && newURL != null) {

            redirect = "http://" + new URL(newURL).getHost() + redirect;

        }

        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(redirect), cookie, null, false);

        if (Plugin.getFileNameFormHeader(requestInfo.getConnection()) == null || Plugin.getFileNameFormHeader(requestInfo.getConnection()).indexOf("?") >= 0) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            logger.severe("Fehler 2 Dateiname kann nicht ermittelt werden");
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);

            return;
        }
        dl = new RAFDownload(this, downloadLink, requestInfo.getConnection());
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));
        dl.setResume(true);
        dl.startDownload();

    }

    public String getAGBLink() {
        return AGB_LINK;
    }

    public String getCoder() {
        return CODER;
    }

    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        RequestInfo requestInfo;
        correctURL(downloadLink);
        try {
            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(downloadLink.getDownloadURL()), null, null, false);
            if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error") > 0) {
                downloadLink.getLinkStatus().setStatusText("Error");
                if (requestInfo.getConnection().getHeaderField("Location").contains("error_traffic_exceeded_free")) { return true;

                }
                return false;
            } else {
                if (requestInfo.getConnection().getHeaderField("Location") != null) {
                    requestInfo = HTTP.getRequest(new URL("http://" + HOST + requestInfo.getConnection().getHeaderField("Location")), null, null, true);
                } else {
                    requestInfo = HTTP.readFromURL(requestInfo.getConnection());
                }

                // Datei geloescht?
                if (requestInfo.getHtmlCode().contains(FILE_NOT_FOUND)) {
                    downloadLink.getLinkStatus().setStatusText("File Not Found");
                    return false;
                }

                String fileName = JDUtilities.htmlDecode(SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), FILE_INFO, 1));
                String ext = SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), FILE_INFO, 4);
                String fileSize = SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), FILE_INFO, 7);
                downloadLink.setName(fileName.trim() + "" + ext.trim());
                if (fileSize != null) {
                    try {
                        int length = (int) Double.parseDouble(fileSize.trim().split(" ")[0]);
                        if (fileSize.toLowerCase().indexOf("mb") > 0) {
                            length *= 1024 * 1024;
                        } else if (fileSize.toLowerCase().indexOf("kb") > 0) {
                            length *= 1024;
                        }

                        downloadLink.setDownloadSize(length);
                    } catch (Exception e) {
                    }
                }
            }
            return true;
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return false;
    }

    public String getFileInformationString(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()) + ")";
    }

    public String getHost() {
        return HOST;
    }

    /*public int getMaxSimultanDownloadNum() {
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true) && getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false)) {
            return 20;
        } else {
            return 1;
        }
    }

   */ public String getPluginName() {
        return HOST;
    }

    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    public void handleFree(DownloadLink parameter) throws Exception {
        LinkStatus linkStatus = parameter.getLinkStatus();
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true) && getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false)) {

            doPremium(parameter);

            return;
        }
        // http://uploaded.to/file/6t2rrq
        // http://uploaded.to/?id=6t2rrq
        // http://uploaded.to/file/6t2rrq/blabla.rar
        // Url correction
        correctURL(parameter);
        RequestInfo requestInfo;

        DownloadLink downloadLink = (DownloadLink) parameter;
        // switch (step.getStep()) {
        // case PluginStep.STEP_WAIT_TIME:

        requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()), "lang=de", null, true);
        // /?view=error_traffic_exceeded_free
        if (requestInfo.containsHTML(TRAFFIC_EXCEEDED_FREE) || requestInfo.containsHTML(DOWNLOAD_LIMIT_REACHED) || requestInfo.getLocation() != null && requestInfo.getLocation().indexOf("traffic_exceeded") >= 0) {
            int waitTime = 61 * 60 * 1000;
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            // step.setStatus(PluginStep.STATUS_ERROR);
            logger.info("Traffic Limit reached....");
            linkStatus.setValue(waitTime);
            return;
        }
        // Datei geloescht?
        if (requestInfo.getHtmlCode().contains(FILE_NOT_FOUND)) {
            logger.severe("download not found");
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        String filepass = null;
        if (requestInfo.containsHTML("file_key")) {
            logger.info("File is Password protected");
            if (downloadLink.getStringProperty("pass", null) != null) {
                filepass = downloadLink.getStringProperty("pass", null);
            } else {
                filepass = JDUtilities.getController().getUiInterface().showUserInputDialog("Password?");
            }
            requestInfo = HTTP.postRequest(new URL(downloadLink.getDownloadURL()), "lang=de", null, null, "lang=de&file_key=" + filepass, false);
        }
        if (requestInfo.containsHTML("file_key")) {
            logger.severe("Wrong password entered");
            /* PassCode war falsch, also Löschen */
            downloadLink.setProperty("pass", null);
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage("Wrong Password");
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;

        }
        if (filepass != null) {
            /* PassCode war richtig, also Speichern */
            downloadLink.setProperty("pass", filepass);
        }

        // logger.info(requestInfo.getHtmlCode());
        captchaAddress = "http://" + requestInfo.getConnection().getRequestProperty("host") + "/" + new Regex(requestInfo.getHtmlCode(), CAPTCHA_FLE).getMatch(1 - 1);

        postTarget = new Regex(requestInfo.getHtmlCode(), CAPTCHA_TEXTFLD).getMatch(1 - 1);
        String url = SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_URL, 0);
        if (url == null) {
            useCaptchaVersion = false;
            // Captcha deaktiviert
            // 

            url = SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_URL_WITHOUT_CAPTCHA, 0);
            logger.finer("Use Captcha free Plugin: " + url);
            requestInfo = HTTP.postRequest(new URL(url), "lang=de", null, null, null, false);
            // /?view=error_traffic_exceeded_free
            if (requestInfo.containsHTML(DOWNLOAD_LIMIT_REACHED) || requestInfo.getLocation() != null && requestInfo.getLocation().indexOf("traffic_exceeded") >= 0) {

                int waitTime = 61 * 60 * 1000;
                linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
                // step.setStatus(PluginStep.STATUS_ERROR);
                logger.info("Traffic Limit reached....");
                // step.setParameter((long) waitTime);
                return;
            }
            if (requestInfo.getConnection().getHeaderField("Location") != null) {
                finalURL = "http://" + requestInfo.getConnection().getRequestProperty("host") + requestInfo.getConnection().getHeaderField("Location");
                return;
            }
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
        } else {
            useCaptchaVersion = true;
            logger.finer("Use Captcha Plugin");
            requestInfo = HTTP.postRequest(new URL(url), "lang=de", null, null, null, false);
            // /?view=error_traffic_exceeded_free
            if (requestInfo.containsHTML(DOWNLOAD_LIMIT_REACHED) || requestInfo.getLocation() != null && requestInfo.getLocation().indexOf("traffic_exceeded") >= 0) {

                int waitTime = 61 * 60 * 1000;
                linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
                // step.setStatus(PluginStep.STATUS_ERROR);
                logger.info("Traffic Limit reached....");
                // step.setParameter((long) waitTime);
                return;
            }

            if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error") > 0) {
                logger.severe("Unbekannter fehler.. retry in 20 sekunden");
                // step.setStatus(PluginStep.STATUS_ERROR);
                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                sleep(20000, downloadLink);
                return;
            }
            if (requestInfo.getConnection().getHeaderField("Location") != null) {
                finalURL = "http://" + requestInfo.getConnection().getRequestProperty("host") + requestInfo.getConnection().getHeaderField("Location");
                return;
            }
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
        }

        // case PluginStep.STEP_GET_CAPTCHA_FILE:
        File file = this.getLocalCaptchaFile(this);
        if (useCaptchaVersion) {

            if (!JDUtilities.download(file, captchaAddress) || !file.exists()) {
                logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
                // this.sleep(nul,downloadLink);
                // step.setStatus(PluginStep.STATUS_ERROR);
                linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);// step.setParameter("Captcha
                // ImageIO
                // Error");

            }

        } else {
            // step.setStatus(PluginStep.STATUS_SKIP);
            downloadLink.getLinkStatus().setStatusText("No Captcha");

        }
        // case PluginStep.STEP_DOWNLOAD:
        if (useCaptchaVersion) {
            String code = this.getCaptchaCode(file, downloadLink);
            finalURL = finalURL + code;
            logger.info("dl " + finalURL);
            postParameter.put(postTarget, code);
            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(finalURL), "lang=de", null, false);
            // /?view=error_traffic_exceeded_free
            if (requestInfo.containsHTML(DOWNLOAD_LIMIT_REACHED) || requestInfo.getLocation() != null && requestInfo.getLocation().indexOf("traffic_exceeded") >= 0) {

                int waitTime = 61 * 60 * 1000;
                linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
                // step.setStatus(PluginStep.STATUS_ERROR);
                logger.info("Traffic Limit reached....");
                // step.setParameter((long) waitTime);
                return;
            }
            if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error-captcha") > 0) {
                // step.setStatus(PluginStep.STATUS_ERROR);
                logger.severe("captcha Falsch");
                linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);

                return;
            }

            if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error") > 0) {
                // step.setStatus(PluginStep.STATUS_ERROR);
                logger.severe("Fehler 1 Errorpage wird angezeigt " + requestInfo.getConnection().getHeaderField("Location"));

                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                sleep(20000, downloadLink);
                return;
            }

            if (Plugin.getFileNameFormHeader(requestInfo.getConnection()) == null || Plugin.getFileNameFormHeader(requestInfo.getConnection()).indexOf("?") >= 0) {
                // step.setStatus(PluginStep.STATUS_ERROR);
                logger.severe("Fehler 2 Dateiname kann nicht ermittelt werden");
                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                sleep(20000, downloadLink);
                return;
            }
            dl = new RAFDownload(this, downloadLink, requestInfo.getConnection());

            dl.startDownload();
            return;
        } else {
            finalURL = finalURL + "";
            logger.info("dl " + finalURL);

            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(finalURL), "lang=de", null, false);

            if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error") > 0) {
                // step.setStatus(PluginStep.STATUS_ERROR);
                logger.severe("Fehler 1 Errorpage wird angezeigt " + requestInfo.getConnection().getHeaderField("Location"));

                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                sleep(20000, downloadLink);
                return;
            }

            int w = 0;
            while (requestInfo.getHeaders().size() < 2) {
                w++;
                downloadLink.getLinkStatus().setStatusText("Warte auf Verbindung...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                if (w > 30) {
                    logger.severe("ERROR!!!");
                    break;
                }
            }
            // logger.info("Filename: " +
            // getFileNameFormHeader(requestInfo.getConnection()));

            // logger.info("Headers: " +
            // requestInfo.getHeaders().size());
            // logger.info("Connection: " +
            // requestInfo.getConnection());
            // logger.info("Code: \r\n" + requestInfo.getHtmlCode());

            dl = new RAFDownload(this, downloadLink, requestInfo.getConnection());
            dl.startDownload();

        }
    }

    public void reset() {
        finalURL = null;
        cookie = null;
    }

    public void resetPluginGlobals() {
    }

    private void setConfigElements() {
      

    }
}
