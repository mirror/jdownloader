package jd.plugins.host;

import java.io.File;
import java.net.URL;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;

public class Odsiebiecom extends PluginForHost {
    private static final String CODER = "JD-Team";

    private static final String HOST = "odsiebie.com";

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?odsiebie\\.com/pokaz/\\d+---[a-zA-Z0-9]+.html", Pattern.CASE_INSENSITIVE);

    // private static final String new Regex("$Revision$","\\$Revision:
    // ([\\d]*?)\\$").getFirstMatch().*= "1.0.0.0";

    // private static final String PLUGIN_ID =PLUGIN_NAME + "-" + new
    // Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();

    private static final String PLUGIN_NAME = HOST;
    private String captchaCode;
    private File captchaFile;
    private String downloadcookie;
    private String downloadurl;
    private String referrerurl;
    private RequestInfo requestInfo;

    public Odsiebiecom() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://share-online.biz/rules.php";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        referrerurl = downloadurl = downloadLink.getDownloadURL();
        try {
            requestInfo = HTTP.getRequest(new URL(downloadurl));
            if (requestInfo != null && requestInfo.getLocation() == null) {
                String filename = requestInfo.getRegexp("Nazwa pliku: <strong>(.*?)</strong>").getFirstMatch();
                String filesize;
                if ((filesize = requestInfo.getRegexp("Rozmiar pliku: <strong>(.*?)MB</strong>").getFirstMatch()) != null) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize) * 1024 * 1024));
                } else if ((filesize = requestInfo.getRegexp("Rozmiar pliku: <strong>(.*?)KB</strong>").getFirstMatch()) != null) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize) * 1024));
                }
                downloadLink.setName(filename);
                return true;
            }
        } catch (Exception e) {

            e.printStackTrace();
        }
        downloadLink.setAvailable(false);
        return false;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    /*public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
   */ public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        /*
         * Zuerst schaun ob wir nen Button haben oder direkt das File vorhanden
         * ist
         */
        String steplink = requestInfo.getRegexp("<a href=\"/pobierz/(.*?)\"  style=\"font-size: 18px\">(.*?)</a>").getFirstMatch();
        if (steplink == null) {
            /* Kein Button, also muss der Link irgendwo auf der Page sein */
            /* Film,Mp3 */
            downloadurl = requestInfo.getRegexp("<PARAM NAME=\"FileName\" VALUE=\"(.*?)\"").getFirstMatch();
            /* Flash */
            if (downloadurl == null) {
                downloadurl = requestInfo.getRegexp("<PARAM NAME=\"movie\" VALUE=\"(.*?)\"").getFirstMatch();
            }
            /* Bilder, Animationen */
            if (downloadurl == null) {
                downloadurl = requestInfo.getRegexp("onLoad=\"scaleImg\\('thepic'\\)\" src=\"(.*?)\" \\/").getFirstMatch();
            }
            /* kein Link gefunden */
            if (downloadurl == null) {
                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                // step.setStatus(PluginStep.STATUS_ERROR);
                return;
            }
        } else {
            /* Button folgen, schaun ob Link oder Captcha als nächstes kommt */
            downloadurl = "http://odsiebie.com/pobierz/" + steplink + ".html";
            downloadcookie = requestInfo.getCookie();
            requestInfo = HTTP.getRequest(new URL(downloadurl), requestInfo.getCookie(), referrerurl, false);
            /* Das Cookie wird überschrieben, daher selbst zusammenbauen */
            downloadcookie = downloadcookie + requestInfo.getCookie();
            referrerurl = downloadurl;
            if (requestInfo.getLocation() != null) {
                /* Weiterleitung auf andere Seite, evtl mit Captcha */
                downloadurl = requestInfo.getLocation();
                requestInfo = HTTP.getRequest(new URL(downloadurl), requestInfo.getCookie(), referrerurl, false);
                downloadcookie = requestInfo.getCookie();
                referrerurl = downloadurl;
            }
            if (requestInfo.containsHTML("captcha.php")) {
                /* Captcha File holen */
                String captchaurl = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<img src=\"(.*?captcha.*?)\">", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                captchaFile = getLocalCaptchaFile(this);
                HTTPConnection captcha_con = new HTTPConnection(new URL(captchaurl).openConnection());
                captcha_con.setRequestProperty("Referer", referrerurl);
                captcha_con.setRequestProperty("Cookie", downloadcookie);
                if (!captcha_con.getContentType().contains("text") && !Browser.download(captchaFile, captcha_con) || !captchaFile.exists()) {
                    /* Fehler beim Captcha */
                    logger.severe("Captcha Download fehlgeschlagen!");
                    // step.setStatus(PluginStep.STATUS_ERROR);
                    linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);// step.setParameter("Captcha
                    // ImageIO
                    // Error");
                    return;
                }
                /* CaptchaCode holen */
                if ((captchaCode = Plugin.getCaptchaCode(captchaFile, this)) == null) {
                    // step.setStatus(PluginStep.STATUS_ERROR);
                    linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
                    return;
                }
                /* Überprüfen(Captcha,Password) */
                downloadurl = "http://odsiebie.com/pobierz/" + steplink + ".html?captcha=" + captchaCode;
                requestInfo = HTTP.getRequest((new URL(downloadurl)), downloadcookie, referrerurl, false);
                if (requestInfo.getLocation() != null && requestInfo.getLocation().contains("html?err")) {
                    // step.setStatus(PluginStep.STATUS_ERROR);
                    linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
                    return;
                }
                downloadcookie = downloadcookie + requestInfo.getCookie();
            }
            /* DownloadLink suchen */
            steplink = requestInfo.getRegexp("<a href=\"/download/(.*?)\"").getFirstMatch();
            if (steplink == null) {
                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                // step.setStatus(PluginStep.STATUS_ERROR);
                return;
            }
            downloadurl = "http://odsiebie.com/download/" + steplink;
            requestInfo = HTTP.getRequest(new URL(downloadurl), downloadcookie, referrerurl, false);
            if (requestInfo.getLocation() == null || requestInfo.getLocation().contains("upload")) {
                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                // step.setStatus(PluginStep.STATUS_ERROR);
                return;
            }
            downloadurl = requestInfo.getLocation();
            if (downloadurl == null) {
                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                // step.setStatus(PluginStep.STATUS_ERROR);
                return;
            }
        }
        /*
         * Leerzeichen müssen durch %20 ersetzt werden!!!!!!!!, sonst werden sie
         * von new URL() abgeschnitten
         */
        downloadurl = downloadurl.replaceAll(" ", "%20");
        /* Datei herunterladen */
        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(downloadurl), requestInfo.getCookie(), referrerurl, false);
        HTTPConnection urlConnection = requestInfo.getConnection();
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setChunkNum(1);
        dl.setResume(false);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }

}
