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

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.http.Browser;
import jd.http.Request;
import jd.parser.HTMLParser;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Megauploadcom extends PluginForHost {
    // http://www.megaupload.com/de/?d=0XOSKVY9
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?(megaupload|megarotic|sexuploader)\\.com/.*?\\?d\\=.{8}", Pattern.CASE_INSENSITIVE);

    static private final String HOST = "megaupload.com";

    static private final String PLUGIN_NAME = HOST;

    static private final String PLUGIN_VERSION = "0.1";

    static private final String PLUGIN_ID = PLUGIN_NAME + "-" + PLUGIN_VERSION;

    static private final String CODER = "JD-Team";

    static private final String SIMPLEPATTERN_CAPTCHA_URl = " <img src=\"/capgen.php?°\">";

    static private final String SIMPLEPATTERN_FILE_NAME = "Dateiname°</b>°</div>";

    static private final String SIMPLEPATTERN_FILE_SIZE = "Dateigr°</b>°</div>";

    static private final String SIMPLEPATTERN_CAPTCHA_POST_URL = "<form method=\"POST\" action=\"°\" target";

    static private final String COOKIE = "l=de; v=1; ve_view=1";

    static private final String SIMPLEPATTERN_GEN_DOWNLOADLINK = "var ° = String.fromCharCode(Math.abs(°));°var ° = '°' + String.fromCharCode(Math.sqrt(°));";

    static private final String SIMPLEPATTERN_GEN_DOWNLOADLINK_LINK = "Math.sqrt(°));°document.getElementById(\"°\").innerHTML = '<a href=\"°' ° '°\"°onclick=\"loadingdownload()";

    static private final String ERROR_TEMP_NOT_AVAILABLE = "Zugriff auf die Datei ist vor";

    static private final String ERROR_FILENOTFOUND = "Die Datei konnte leider nicht gefunden werden";

    static private final int PENDING_WAITTIME = 45000;

    private static final String PATTERN_PASSWORD_WRONG = "Wrong password! Please try again";

    // /Simplepattern
    // private String finalURL;

    private String captchaURL;

    private HashMap<String, String> fields;

    private String captchaPost;

    private boolean tempUnavailable = false;

    // private String finalurl;

    public Megauploadcom() {
        // steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        // steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        // steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
        setConfigElements();
    }

    private void setConfigElements() {

        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, JDLocale.L("plugins.host.premium.account", "Premium Account")));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PROPERTY_PREMIUM_USER, JDLocale.L("plugins.host.premium.user", "Benutzer")));
        cfg.setDefaultValue("Kundennummer");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getProperties(), PROPERTY_PREMIUM_PASS, JDLocale.L("plugins.host.premium.password", "Passwort")));
        cfg.setDefaultValue("Passwort");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_USE_PREMIUM, JDLocale.L("plugins.host.premium.useAccount", "Premium Account verwenden")));
        cfg.setDefaultValue(false);

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getProperties(), "COUNTRY_ID", new String[] { "-", "en", "de", "fr", "es", "pt", "nl", "it", "cn", "ct", "jp", "kr", "ru", "fi", "se", "dk", "tr", "sa", "vn", "pl" }, "LänderID").setDefaultValue("-"));

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
    public void handle(DownloadLink parameter) throws Exception {
        LinkStatus linkStatus = parameter.getLinkStatus();
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true) && getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false)) {

            doPremium(parameter);
            return;
        }

        DownloadLink downloadLink = (DownloadLink) parameter;
        String link = downloadLink.getDownloadURL().replaceAll("/de", "");

        String countryID = getProperties().getStringProperty("COUNTRY_ID", "-");
        if (!countryID.equals("-")) {
            logger.info("Use Country trick");
            // http://www.megaupload.com/HIER_STEHT_DER_2_STELLIGE_LÄNDERKÜRZEL/?d=EMXRGYTM

            try {
                link = "http://" + new URL(link).getHost() + "/" + countryID + "/?d=" + link.substring(link.indexOf("?d=") + 3);
            } catch (MalformedURLException e) {

                e.printStackTrace();
            }

        }

        // switch (step.getStep()) {
        // case PluginStep.STEP_WAIT_TIME:

        requestInfo = HTTP.getRequest(new URL(link), COOKIE, null, true);
        if (requestInfo.containsHTML(ERROR_TEMP_NOT_AVAILABLE)) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            this.tempUnavailable = true;
            this.sleep(60 * 30, downloadLink);
            return;
        }
        if (requestInfo.containsHTML(ERROR_FILENOTFOUND)) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }

        this.captchaURL = "http://" + new URL(link).getHost() + "/capgen.php?" + SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_CAPTCHA_URl, 0);
        this.fields = HTMLParser.getInputHiddenFields(requestInfo.getHtmlCode(), "checkverificationform", "passwordhtml");
        this.captchaPost = SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_CAPTCHA_POST_URL, 0);
        // this.sleep(captchaUR,downloadLink);
        if (captchaURL.endsWith("null") || captchaPost == null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
        }

        // case PluginStep.STEP_GET_CAPTCHA_FILE:
        File file = this.getLocalCaptchaFile(this);
        logger.info("Captcha " + captchaURL);
        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(captchaURL), COOKIE, requestInfo.getLocation(), true);
        if (!requestInfo.isOK() || !JDUtilities.download(file, requestInfo.getConnection()) || !file.exists()) {
            logger.severe("Captcha Download fehlgeschlagen: " + captchaURL);
            // this.sleep(nul,downloadLink);
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_SPECIFIC);// step.setParameter("Captcha
                                                                    // ImageIO
                                                                    // Error");
            return;
        }
        String code = this.getCaptchaCode(file);
        // case PluginStep.STEP_PENDING:
        requestInfo = HTTP.postRequest(new URL(captchaPost), COOKIE, null, null, joinMap(fields, "=", "&") + "&imagestring=" + code, true);
        if (SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_CAPTCHA_URl, 0) != null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA_WRONG);
            return;
        }

        String pwdata = HTMLParser.getFormInputHidden(requestInfo.getHtmlCode(), "passwordbox", "passwordcountdown");
        if (pwdata != null && pwdata.indexOf("passkey") > 0) {
            logger.info("Password protected");
            String pass = JDUtilities.getController().getUiInterface().showUserInputDialog("Password:");
            if (pass == null) {
                // step.setStatus(PluginStep.STATUS_ERROR);
                linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_SPECIFIC);
                // step.setParameter("wrong Password");
                return;
            }
            if (countryID.equals("-")) {

                requestInfo = HTTP.postRequest(new URL("http://" + new URL(link).getHost() + "/de/"), COOKIE, null, null, pwdata + "&pass=" + pass, true);
            } else {
                requestInfo = HTTP.postRequest(new URL("http://" + new URL(link).getHost() + "/" + countryID + "/"), COOKIE, null, null, pwdata + "&pass=" + pass, true);

            }
            if (requestInfo.containsHTML(PATTERN_PASSWORD_WRONG)) {
                // step.setStatus(PluginStep.STATUS_ERROR);
                linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_SPECIFIC);
                // step.setParameter("wrong Password");
                return;
            }

        }
        this.sleep(PENDING_WAITTIME, downloadLink);

        Character l = (char) Math.abs(Integer.parseInt(SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_GEN_DOWNLOADLINK, 1).trim()));
        String i = SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_GEN_DOWNLOADLINK, 4) + (char) Math.sqrt(Integer.parseInt(SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_GEN_DOWNLOADLINK, 5).trim()));
        String url = (JDUtilities.htmlDecode(SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_GEN_DOWNLOADLINK_LINK, 3) + i + l + SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_GEN_DOWNLOADLINK_LINK, 5)));

        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(url), COOKIE, null, true);
        if (!requestInfo.isOK()) {
            logger.warning("Download Limit!");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_TRAFFIC_LIMIT);
            String wait = requestInfo.getConnection().getHeaderField("Retry-After");
            logger.finer("Warten: " + wait + " minuten");
            if (wait != null) {
                linkStatus.setValue(Integer.parseInt(wait.trim()) * 60 * 1000);
            } else {
                linkStatus.setValue(120 * 60 * 1000);
            }
            return;

        }
        int length = requestInfo.getConnection().getContentLength();
        downloadLink.setDownloadMax(length);
        logger.info("Filename: " + getFileNameFormHeader(requestInfo.getConnection()));

        downloadLink.setName(getFileNameFormHeader(requestInfo.getConnection()));

        dl = new RAFDownload(this, downloadLink, requestInfo.getConnection());

        dl.startDownload();

    }

    private void doPremium(DownloadLink parameter) throws Exception {
        LinkStatus linkStatus = parameter.getLinkStatus();
        DownloadLink downloadLink = (DownloadLink) parameter;
        String link = downloadLink.getDownloadURL().replaceAll("/de", "");
        String user = getProperties().getStringProperty(PROPERTY_PREMIUM_USER);
        String pass = getProperties().getStringProperty(PROPERTY_PREMIUM_PASS);
        String countryID = getProperties().getStringProperty("COUNTRY_ID", "-");
        logger.info("PREMOIM");
        String url = "http://www.megaupload.com/de/";
        if (!countryID.equals("-")) {
            logger.info("Use Country trick");
            // http://www.megaupload.com/HIER_STEHT_DER_2_STELLIGE_LÄNDERKÜRZEL/?d=EMXRGYTM

            link = link.replace(".com/", ".com/" + countryID + "/");
            url = url.replaceAll("/de/", "/" + countryID + "/");

        }

        downloadLink.getLinkStatus().setStatusText("Login");
        Browser br = new Browser();
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.0; de; rv:1.8.1.14) Gecko/20080404 Firefox/2.0.0.14;MEGAUPLOAD 1.0");
        br.getHeaders().put("X-MUTB", link);
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        br.postPage(url, "login=" + user + "&password=" + pass);
        if (Browser.getCookie(url, "user") == null || Browser.getCookie(url, "user").length() == 0) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_PREMIUM);
        }

        String id = Request.parseQuery(link).get("d");
        br.getHeaders().clear();
        br.getHeaders().put("TE", "trailers");
        br.getHeaders().put("Connection", "TE");
        br.setFollowRedirects(false);
        br.getPage("http://" + new URL(link).getHost() + "/mgr_dl.php?d=" + id + "&u=" + Browser.getCookie(url, "user"));

        HTTPConnection urlConnection;
        downloadLink.getLinkStatus().setStatusText("Premium");

        // requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(finalurl),
        // Browser.getCookie(url,"user"), link, false);
        urlConnection = br.openGetConnection(br.getRedirectLocation());
        String name = getFileNameFormHeader(urlConnection);
        downloadLink.setName(name);
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setResume(true);
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));
        dl.setResume(true);
        dl.startDownload();

    }

    // Retry-After

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void reset() {
        // this.finalURL = null;
        this.captchaPost = null;
        this.captchaURL = null;
        this.fields = null;
    }

    public String getFileInformationString(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        return (tempUnavailable ? "<Temp. unavailable> " : "") + downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()) + ")";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        RequestInfo requestInfo;

        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll("/de", ""));
        try {
            requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()), "l=de; v=1; ve_view=1", null, true);
            if (requestInfo.containsHTML(ERROR_TEMP_NOT_AVAILABLE)) {
                downloadLink.getLinkStatus().setStatusText("Temp. not available");
                logger.info("Temp. unavailable");
                this.tempUnavailable = true;
                return false;
            }
            if (requestInfo.containsHTML(ERROR_FILENOTFOUND)) {
                downloadLink.getLinkStatus().setStatusText("File Not Found");
                return false;
            }

            String fileName = JDUtilities.htmlDecode(SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_FILE_NAME, 1));
            String fileSize = SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_FILE_SIZE, 1);
            if (fileName == null || fileSize == null) { return false; }
            downloadLink.setName(fileName.trim());
            if (fileSize.indexOf("KB") > 0) {
                downloadLink.setDownloadMax((int) (Double.parseDouble(fileSize.trim().split(" ")[0].trim()) * 1024));
            }
            if (fileSize.indexOf("MB") > 0) {
                downloadLink.setDownloadMax((int) (Double.parseDouble(fileSize.trim().split(" ")[0].trim()) * 1024 * 1024));
            }
        } catch (MalformedURLException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true) && this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false)) {
            return 20;
        } else {
            return 1;
        }
    }

    @Override
    public void resetPluginGlobals() {
        // TODO Auto-generated method stub

    }

    @Override
    public String getAGBLink() {
        // TODO Auto-generated method stub
        return "http://www.megaupload.com/terms/";
    }
}
