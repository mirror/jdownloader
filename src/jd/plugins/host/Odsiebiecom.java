package jd.plugins.host;

import java.io.File;
import java.net.URL;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class Odsiebiecom extends PluginForHost {
    private static final String CODER = "JD-Team";

    private static final String HOST = "odsiebie.com";

    private static final String PLUGIN_NAME = HOST;

    private static final String PLUGIN_VERSION = "1.0.0.0";

    private static final String PLUGIN_ID = PLUGIN_NAME + "-" + PLUGIN_VERSION;

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?odsiebie\\.com/pokaz/\\d+---[a-zA-Z0-9]+.html", Pattern.CASE_INSENSITIVE);
    private RequestInfo requestInfo;
    private String referrerurl;
    private String downloadurl;
    private String downloadcookie;
    private File captchaFile;
    private String captchaCode;

    public Odsiebiecom() {
        super();
        //steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
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

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        referrerurl = downloadurl = downloadLink.getDownloadURL();
        try {
            requestInfo = HTTP.getRequest(new URL(downloadurl));
            if (requestInfo != null && requestInfo.getLocation() == null) {
                String filename = requestInfo.getRegexp("Nazwa pliku: <strong>(.*?)</strong>").getFirstMatch();
                String filesize;
                if ((filesize = requestInfo.getRegexp("Rozmiar pliku: <strong>(.*?)MB</strong>").getFirstMatch()) != null) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize) * 1024 * 1024));
                } else if ((filesize = requestInfo.getRegexp("Rozmiar pliku: <strong>(.*?)KB</strong>").getFirstMatch()) != null) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize) * 1024));
                }
                downloadLink.setName(filename);
                return true;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        downloadLink.setAvailable(false);
        return false;
    }

    public void handle( DownloadLink downloadLink) {
        if (step == null) return null;
        try {
            /* Nochmals das File überprüfen */
            if (!getFileInformation(downloadLink)) {
                downloadLink.setStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                step.setStatus(PluginStep.STATUS_ERROR);
                return step;
            }
            /*
             * Zuerst schaun ob wir nen Button haben oder direkt das File
             * vorhanden ist
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
                    downloadLink.setStatus(LinkStatus.ERROR_UNKNOWN);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
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
                    String captchaurl=new Regex(requestInfo.getHtmlCode(),Pattern.compile("<img src=\"(.*?captcha.*?)\">",Pattern.CASE_INSENSITIVE)).getFirstMatch();                    
                    captchaFile = getLocalCaptchaFile(this);
                    HTTPConnection captcha_con = new HTTPConnection(new URL(captchaurl).openConnection());
                    captcha_con.setRequestProperty("Referer", referrerurl);
                    captcha_con.setRequestProperty("Cookie", downloadcookie);
                    if (!captcha_con.getContentType().contains("text") && !JDUtilities.download(captchaFile, captcha_con) || !captchaFile.exists()) {
                        /* Fehler beim Captcha */
                        logger.severe("Captcha Download fehlgeschlagen!");
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(LinkStatus.ERROR_PLUGIN_SPECIFIC);//step.setParameter("Captcha ImageIO Error");
                        return step;
                    }
                    /* CaptchaCode holen */
                    if ((captchaCode = Plugin.getCaptchaCode(captchaFile, this)) == null) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(LinkStatus.ERROR_CAPTCHA_WRONG);
                        return step;
                    }
                    /* Überprüfen(Captcha,Password) */
                    downloadurl = "http://odsiebie.com/pobierz/" + steplink + ".html?captcha=" + captchaCode;
                    requestInfo = HTTP.getRequest((new URL(downloadurl)), downloadcookie, referrerurl, false);
                    if (requestInfo.getLocation() != null && requestInfo.getLocation().contains("html?err")) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(LinkStatus.ERROR_CAPTCHA_WRONG);
                        return step;
                    }
                    downloadcookie = downloadcookie + requestInfo.getCookie();
                }
                /* DownloadLink suchen */
                steplink = requestInfo.getRegexp("<a href=\"/download/(.*?)\"").getFirstMatch();
                if (steplink == null) {
                    downloadLink.setStatus(LinkStatus.ERROR_UNKNOWN);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
                downloadurl = "http://odsiebie.com/download/" + steplink;
                requestInfo = HTTP.getRequest(new URL(downloadurl), downloadcookie, referrerurl, false);
                if (requestInfo.getLocation() == null || requestInfo.getLocation().contains("upload")) {
                    downloadLink.setStatus(LinkStatus.ERROR_UNKNOWN);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
                downloadurl = requestInfo.getLocation();
                if (downloadurl == null) {
                    downloadLink.setStatus(LinkStatus.ERROR_UNKNOWN);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
            }
            /*Leerzeichen müssen durch %20 ersetzt werden!!!!!!!!, sonst werden sie von new URL() abgeschnitten*/
            downloadurl=downloadurl.replaceAll(" ", "%20"); 
            /* Datei herunterladen */
            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(downloadurl), requestInfo.getCookie(), referrerurl, false);
            HTTPConnection urlConnection = requestInfo.getConnection();
            String filename = getFileNameFormHeader(urlConnection);
            if (urlConnection.getContentLength() == 0) {
                downloadLink.setStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                step.setStatus(PluginStep.STATUS_ERROR);
                return step;
            }
            downloadLink.setDownloadMax(urlConnection.getContentLength());
            downloadLink.setName(filename);
            long length = downloadLink.getDownloadMax();
            dl = new RAFDownload(this, downloadLink, urlConnection);
            dl.setChunkNum(1);
            dl.setResume(false);
            dl.setFilesize(length);
            if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {
                downloadLink.setStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                step.setStatus(PluginStep.STATUS_ERROR);
                return step;
            }
            return step;

        } catch (Exception e) {
            e.printStackTrace();
        }
        step.setStatus(PluginStep.STATUS_ERROR);
        downloadLink.setStatus(LinkStatus.ERROR_UNKNOWN);
        return step;
    }

    @Override
    public void resetPluginGlobals() {
        // TODO Auto-generated method stub
    }

    @Override
    public String getAGBLink() {
        return "http://share-online.biz/rules.php";
    }

}
