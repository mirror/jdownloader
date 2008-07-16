package jd.plugins.host;

import java.io.File;
import java.net.URL;
import java.util.regex.Pattern;

import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class ShareNownet extends PluginForHost {
    private static final String CODER = "JD-Team";

    private static final String HOST = "share-now.net";

    private static final String PLUGIN_NAME = HOST;

    private static final String PLUGIN_VERSION = "1.0.0.0";

    private static final String PLUGIN_ID = PLUGIN_NAME + "-" + PLUGIN_VERSION;

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?share-now\\.net/{1,}files/\\d+-(.*?)\\.html", Pattern.CASE_INSENSITIVE);
    private RequestInfo requestInfo;
    private String downloadurl;
    private File captchaFile;
    private String captchaCode;

    public ShareNownet() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
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
        downloadurl = downloadLink.getDownloadURL();
        try {
            requestInfo = HTTP.getRequest(new URL(downloadurl));
            if (!requestInfo.containsHTML("Datei existiert nicht oder wurde gel&ouml;scht!")) {
                String linkinfo[][] = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<span class=\"style1\">(.*?)\\((.*?) (.*?)\\) </span>", Pattern.CASE_INSENSITIVE)).getMatches();
                if (linkinfo[0][2].matches("MB")) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(linkinfo[0][1]) * 1024 * 1024));
                } else if (linkinfo[0][2].matches("KB")) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(linkinfo[0][1]) * 1024));
                }
                downloadLink.setName(linkinfo[0][0]);
                return true;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        downloadLink.setAvailable(false);
        return false;
    }

    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {
        if (step == null) return null;
        try {
            /* Nochmals das File überprüfen */
            if (!getFileInformation(downloadLink)) {
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                step.setStatus(PluginStep.STATUS_ERROR);
                return step;
            }
            Form form = requestInfo.getForms()[1];
            form.withHtmlCode = false;
            /* Captcha File holen */
            captchaFile = getLocalCaptchaFile(this);
            HTTPConnection captcha_con = new HTTPConnection(new URL("http://share-now.net/captcha.php?id=" + form.vars.get("download")).openConnection());
            captcha_con.setRequestProperty("Referer", downloadLink.getDownloadURL());
            captcha_con.setRequestProperty("Cookie", requestInfo.getCookie());
            if (!captcha_con.getContentType().contains("text") && !JDUtilities.download(captchaFile, captcha_con) || !captchaFile.exists()) {
                /* Fehler beim Captcha */
                logger.severe("Captcha Download fehlgeschlagen!");
                step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                return step;
            }
            /* CaptchaCode holen */
            if ((captchaCode = Plugin.getCaptchaCode(captchaFile, this)) == null) {
                step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                return step;
            }
            /* DownloadLink holen/Captcha check */
            form.vars.put("captcha", captchaCode);
            requestInfo = form.getRequestInfo(false);
            if (requestInfo.getLocation() != null) {
                step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                return step;
            }
            /* Datei herunterladen */
            HTTPConnection urlConnection = requestInfo.getConnection();
            String filename = getFileNameFormHeader(urlConnection);
            if (urlConnection.getContentLength() == 0) {
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                step.setStatus(PluginStep.STATUS_ERROR);
                return step;
            }
            downloadLink.setDownloadMax(urlConnection.getContentLength());
            downloadLink.setName(filename);
            long length = downloadLink.getDownloadMax();
            dl = new RAFDownload(this, downloadLink, urlConnection);
            dl.setFilesize(length);
            dl.setChunkNum(1);
            dl.setResume(false);
            if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                step.setStatus(PluginStep.STATUS_ERROR);
                return step;
            }
            return step;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        step.setStatus(PluginStep.STATUS_ERROR);
        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
        return step;
    }

    @Override
    public void resetPluginGlobals() {
        // TODO Auto-generated method stub
    }

    @Override
    public String getAGBLink() {
        return "http://share-now.net/agb.php";
    }

}
