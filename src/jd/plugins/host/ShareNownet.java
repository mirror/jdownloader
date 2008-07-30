package jd.plugins.host;

import java.io.File;
import java.net.URL;
import java.util.regex.Pattern;

import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class ShareNownet extends PluginForHost {
    private static final String CODER = "JD-Team";

    private static final String HOST = "share-now.net";

    private static final String PLUGIN_NAME = HOST;

    //private static final String new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch().*= "1.0.0.0";

    //private static final String PLUGIN_ID =PLUGIN_NAME + "-" + new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?share-now\\.net/{1,}files/\\d+-(.*?)\\.html", Pattern.CASE_INSENSITIVE);
    private RequestInfo requestInfo;
    private String downloadurl;
    private File captchaFile;
    private String captchaCode;

    public ShareNownet() {
        super();

    }

    
    public boolean doBotCheck(File file) {
        return false;
    }

    
    public String getCoder() {
        return CODER;
    }

    
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    
    public String getHost() {
        return HOST;
    }

    
    public String getVersion() {
       String ret=new Regex("$Revision$","\\$Revision: ([\\d]*?) \\$").getFirstMatch();return ret==null?"0.0":ret;
    }

    
    
        
   

    
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    
    public void reset() {
    }

    
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        downloadurl = downloadLink.getDownloadURL();
        try {
            requestInfo = HTTP.getRequest(new URL(downloadurl));
            if (!requestInfo.containsHTML("Datei existiert nicht oder wurde gel&ouml;scht!")) {

                String linkinfo[][] = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<h3 align=\"center\"><strong>(.*?)</strong> \\(\\s*([0-9\\.]*)\\s([GKMB]*)\\s*\\) </h3>", Pattern.CASE_INSENSITIVE)).getMatches();
                if (linkinfo.length != 1) linkinfo = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<span class=\"style1\">(.*?)\\(([0-9\\.]*)\\s*([GKMB]*)\\) </span>", Pattern.CASE_INSENSITIVE)).getMatches();
                if (linkinfo[0][2].matches("MB")) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(linkinfo[0][1]) * 1024 * 1024));
                } else if (linkinfo[0][2].matches("KB")) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(linkinfo[0][1]) * 1024));
                }
                downloadLink.setName(linkinfo[0][0]);
                return true;
            }
        } catch (Exception e) {

            e.printStackTrace();
        }
        downloadLink.setAvailable(false);
        return false;
    }

    public void handle(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        Form form = requestInfo.getForms()[1];
        form.withHtmlCode = false;
        /* gibts nen captcha? */
        if (requestInfo.containsHTML("Sicherheitscode eingeben")) {
            /* Captcha File holen */
            captchaFile = getLocalCaptchaFile(this);
            HTTPConnection captcha_con = new HTTPConnection(new URL("http://share-now.net/captcha.php?id=" + form.vars.get("download")).openConnection());
            captcha_con.setRequestProperty("Referer", downloadLink.getDownloadURL());
            captcha_con.setRequestProperty("Cookie", requestInfo.getCookie());
            if (!captcha_con.getContentType().contains("text") && !JDUtilities.download(captchaFile, captcha_con) || !captchaFile.exists()) {
                /* Fehler beim Captcha */
                logger.severe("Captcha Download fehlgeschlagen!");
                // step.setStatus(PluginStep.STATUS_ERROR);
                linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
                return;
            }
            /* CaptchaCode holen */
            if ((captchaCode = Plugin.getCaptchaCode(captchaFile, this)) == null) {
                // step.setStatus(PluginStep.STATUS_ERROR);
                linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
                return;
            }
            form.vars.put("captcha", captchaCode);
        }
        /* DownloadLink holen/Captcha check */
        requestInfo = form.getRequestInfo(false);
        if (requestInfo.getLocation() != null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }
        /* Datei herunterladen */
        HTTPConnection urlConnection = requestInfo.getConnection();
        String filename = getFileNameFormHeader(urlConnection);
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        downloadLink.setDownloadMax(urlConnection.getContentLength());
        downloadLink.setName(filename);
        long length = downloadLink.getDownloadMax();
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setFilesize(length);
        dl.setChunkNum(1);
        dl.setResume(false);
        dl.startDownload();
    }

    
    public void resetPluginGlobals() {
       
    }

    
    public String getAGBLink() {
        return "http://share-now.net/agb.php";
    }

}
